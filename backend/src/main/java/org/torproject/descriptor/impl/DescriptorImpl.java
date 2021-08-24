/* Copyright 2012--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.torproject.descriptor.Descriptor;
import org.torproject.descriptor.DescriptorParseException;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public abstract class DescriptorImpl implements Descriptor {

  public static final String NL = "\n";

  public static final String SP = " ";

  protected byte[] rawDescriptorBytes;

  /**
   * The index of the first byte of this descriptor in
   * {@link #rawDescriptorBytes} which may contain more than just one
   * descriptor.
   */
  protected int offset;

  /**
   * The number of bytes of this descriptor in {@link #rawDescriptorBytes} which
   * may contain more than just one descriptor.
   */
  protected int length;

  @Override
  public int getRawDescriptorLength() {
    return this.length;
  }

  /**
   * Optional reference to the descriptor file, if this descriptor was read from
   * a descriptor file.
   */
  private File descriptorFile;

  @Override
  public File getDescriptorFile() {
    return this.descriptorFile;
  }

  /**
   * Returns a <em>copy</em> of the full raw descriptor bytes.
   *
   * <p>If possible, subclasses should avoid retrieving raw descriptor bytes and
   * converting them to a String themselves and instead rely on
   * {@link #newScanner()} and related methods to parse the descriptor.</p>
   *
   * @return Copy of the full raw descriptor bytes.
   */
  @Override
  public byte[] getRawDescriptorBytes() {
    return this.getRawDescriptorBytes(this.offset, this.length);
  }

  /**
   * Returns a <em>copy</em> of raw descriptor bytes starting at
   * {@code offset} and containing {@code length} bytes.
   *
   * <p>If possible, subclasses should avoid retrieving raw descriptor bytes and
   * converting them to a String themselves and instead rely on
   * {@link #newScanner()} and related methods to parse the descriptor.</p>
   *
   * @param offset The index of the first byte to include.
   * @param length The number of bytes to include.
   * @return Copy of the given raw descriptor bytes.
   */
  protected byte[] getRawDescriptorBytes(int offset, int length) {
    if (offset < this.offset || offset + length > this.offset + this.length
        || length < 0) {
      throw new IndexOutOfBoundsException("offset=" + offset + " length="
          + length + " this.offset=" + this.offset + " this.length="
          + this.length);
    }
    byte[] result = new byte[length];
    System.arraycopy(this.rawDescriptorBytes, offset, result, 0, length);
    return result;
  }

  /**
   * Returns a new {@link Scanner} for parsing the full raw descriptor using
   * UTF-8 as charset.
   *
   * @return Scanner for the full raw descriptor bytes.
   */
  protected Scanner newScanner() {
    return this.newScanner(this.offset, this.length);
  }

  /**
   * Returns a new {@link Scanner} for parsing the raw descriptor starting at
   * byte {@code offset} containing {@code length} bytes using UTF-8
   * as charset.
   *
   * @param offset The index of the first byte to parse.
   * @param length The number of bytes to parse.
   * @return Scanner for the given raw descriptor bytes.
   */
  protected Scanner newScanner(int offset, int length) {
    return new Scanner(new InputStreamReader(new ByteArrayInputStream(
        this.rawDescriptorBytes, offset, length), StandardCharsets.UTF_8));
  }

  /**
   * Returns the index within the raw descriptor of the first occurrence of the
   * given {@code key}, or {@code -1} if the key is not contained.
   *
   * @param key Key to search for.
   * @return Index of the first occurrence, or -1.
   */
  protected int findFirstIndexOfKey(Key key) {
    String ascii = new String(this.rawDescriptorBytes, this.offset, this.length,
        StandardCharsets.US_ASCII);
    if (ascii.startsWith(key.keyword + SP)
        || ascii.startsWith(key.keyword + NL)) {
      return this.offset;
    }
    int keywordIndex = ascii.indexOf(NL + key.keyword + SP);
    if (keywordIndex < 0) {
      keywordIndex = ascii.indexOf(NL + key.keyword + NL);
    }
    if (keywordIndex < 0) {
      return -1;
    } else {
      return this.offset + keywordIndex + 1;
    }
  }

  /**
   * Returns a list of two-element arrays containing offsets and lengths of
   * descriptors starting with the given {@code key} in the raw descriptor
   * starting at byte {@code offset} containing {@code length} bytes.
   *
   * @param key Key to search for.
   * @param offset The index of the first byte to split.
   * @param length The number of bytes to split.
   * @param truncateTrailingNewlines Whether trailing newlines shall be
   *      truncated.
   * @return List of two-element arrays containing offsets and lengths.
   */
  protected List<int[]> splitByKey(Key key, int offset, int length,
      boolean truncateTrailingNewlines) {
    List<int[]> splitParts = new ArrayList<>();
    String ascii = new String(this.rawDescriptorBytes, offset, length,
        StandardCharsets.US_ASCII);
    int from = 0;
    while (from < length) {
      int to = ascii.indexOf(NL + key.keyword + SP, from);
      if (to < 0) {
        to = ascii.indexOf(NL + key.keyword + NL, from);
      }
      if (to < 0) {
        to = length;
      } else {
        to += 1;
      }
      int toNoNewline = to;
      while (truncateTrailingNewlines && toNoNewline > from
          && ascii.charAt(toNoNewline - 1) == '\n') {
        toNoNewline--;
      }
      splitParts.add(new int[] { offset + from, toNoNewline - from });
      from = to;
    }
    return splitParts;
  }

  protected List<String> unrecognizedLines;

  @Override
  public List<String> getUnrecognizedLines() {
    return this.unrecognizedLines == null ? new ArrayList<>()
        : new ArrayList<>(this.unrecognizedLines);
  }

  protected DescriptorImpl(byte[] rawDescriptorBytes, int[] offsetAndLength,
      File descriptorFile) {
    int offset = offsetAndLength[0];
    int length = offsetAndLength[1];
    if (offset < 0 || offset + length > rawDescriptorBytes.length
        || length < 0) {
      throw new IndexOutOfBoundsException("Invalid bounds: "
          + "rawDescriptorBytes.length=" + rawDescriptorBytes.length
          + " offset=" + offset + " length=" + length);
    }
    this.rawDescriptorBytes = rawDescriptorBytes;
    this.offset = offset;
    this.length = length;
    this.descriptorFile = descriptorFile;
  }

  protected DescriptorImpl(byte[] rawDescriptorBytes, int[] offsetAndLength,
      File descriptorFile, boolean blankLinesAllowed)
      throws DescriptorParseException {
    this(rawDescriptorBytes, offsetAndLength, descriptorFile);
    this.cutOffAnnotations();
    this.countKeys(rawDescriptorBytes, blankLinesAllowed);
  }

  /* Parse annotation lines from the descriptor bytes. */
  private List<String> annotations = new ArrayList<>();

  private void cutOffAnnotations() throws DescriptorParseException {
    int start = 0;
    String ascii = new String(this.getRawDescriptorBytes(),
        StandardCharsets.US_ASCII);
    while ((start == 0 && ascii.startsWith("@"))
        || (start > 0 && ascii.indexOf(NL + "@", start - 1) >= 0)) {
      int end = ascii.indexOf(NL, start);
      if (end < 0) {
        throw new DescriptorParseException("Annotation line does not "
            + "contain a newline.");
      }
      this.annotations.add(ascii.substring(start, end));
      start = end + 1;
    }
    this.offset += start;
    this.length -= start;
  }

  @Override
  public List<String> getAnnotations() {
    return new ArrayList<>(this.annotations);
  }

  private Key firstKey = Key.EMPTY;

  private Key lastKey = Key.EMPTY;

  private Map<Key, Integer> parsedKeys = new EnumMap<>(Key.class);

  /* Count parsed keywords for consistency checks by subclasses. */
  private void countKeys(byte[] rawDescriptorBytes,
      boolean blankLinesAllowed) throws DescriptorParseException {
    if (rawDescriptorBytes.length == 0) {
      throw new DescriptorParseException("Descriptor is empty.");
    }
    boolean skipCrypto = false;
    Scanner scanner = this.newScanner().useDelimiter(NL);
    while (scanner.hasNext()) {
      String line = scanner.next();
      if (line.isEmpty() && !blankLinesAllowed) {
        throw new DescriptorParseException("Blank lines are not allowed.");
      } else if (line.startsWith(Key.CRYPTO_BEGIN.keyword)) {
        skipCrypto = true;
      } else if (line.startsWith(Key.CRYPTO_END.keyword)) {
        skipCrypto = false;
      } else if (!line.isEmpty() && !line.startsWith("@")
          && !skipCrypto) {
        String lineNoOpt = line.startsWith(Key.OPT.keyword + SP)
            ? line.substring(Key.OPT.keyword.length() + 1) : line;
        String keyword = lineNoOpt.split(SP, -1)[0];
        if (keyword.equals("")) {
          throw new DescriptorParseException("Illegal keyword in line '"
              + line + "'.");
        }
        Key key = Key.get(keyword);
        if (Key.EMPTY == this.firstKey) {
          this.firstKey = key;
        }
        lastKey = key;
        parsedKeys.put(key, parsedKeys.getOrDefault(key, 0) + 1);
      }
    }
  }

  protected void checkFirstKey(Key key)
      throws DescriptorParseException {
    if (this.firstKey != key) {
      throw new DescriptorParseException("Keyword '" + key.keyword + "' must "
          + "be contained in the first line.");
    }
  }

  protected void checkLastKey(Key key)
      throws DescriptorParseException {
    if (this.lastKey != key) {
      throw new DescriptorParseException("Keyword '" + key.keyword + "' must "
          + "be contained in the last line.");
    }
  }

  protected void checkExactlyOnceKeys(Set<Key> keys)
      throws DescriptorParseException {
    for (Key key : keys) {
      int contained = this.parsedKeys.getOrDefault(key, 0);
      if (contained != 1) {
        throw new DescriptorParseException("Keyword '" + key.keyword + "' is "
            + "contained " + contained + " times, but must be contained "
            + "exactly once.");
      }
    }
  }

  protected void checkAtLeastOnceKeys(Set<Key> keys)
      throws DescriptorParseException {
    for (Key key : keys) {
      if (!this.parsedKeys.containsKey(key)) {
        throw new DescriptorParseException("Keyword '" + key.keyword + "' is "
            + "contained 0 times, but must be contained at least once.");
      }
    }
  }

  protected void checkAtMostOnceKeys(Set<Key> keys)
      throws DescriptorParseException {
    for (Key key : keys) {
      int contained = this.parsedKeys.getOrDefault(key, 0);
      if (contained > 1) {
        throw new DescriptorParseException("Keyword '" + key.keyword + "' is "
            + "contained " + contained + " times, "
            + "but must be contained at most once.");
      }
    }
  }

  protected void checkKeysDependOn(Set<Key> dependentKeys,
      Key dependingKey) throws DescriptorParseException {
    for (Key dependentKey : dependentKeys) {
      if (this.parsedKeys.containsKey(dependentKey)
          && !this.parsedKeys.containsKey(dependingKey)) {
        throw new DescriptorParseException("Keyword '" + dependentKey.keyword
            + "' is contained, but keyword '" + dependingKey.keyword + "' is "
            + "not.");
      }
    }
  }

  protected int getKeyCount(Key key) {
    return this.parsedKeys.getOrDefault(key, 0);
  }

  protected void clearParsedKeys() {
    this.parsedKeys = null;
  }

  private String digestSha1Hex;

  protected void setDigestSha1Hex(String digestSha1Hex) {
    this.digestSha1Hex = digestSha1Hex;
  }

  protected void calculateDigestSha1Hex(String startToken, String endToken)
      throws DescriptorParseException {
    if (null == this.digestSha1Hex) {
      String ascii = new String(this.rawDescriptorBytes, this.offset,
          this.length, StandardCharsets.US_ASCII);
      int start = ascii.indexOf(startToken);
      int end = -1;
      if (null == endToken) {
        end = ascii.length();
      } else if (ascii.contains(endToken)) {
        end = ascii.indexOf(endToken) + endToken.length();
      }
      if (start >= 0 && end >= 0 && end > start) {
        this.digestSha1Hex = Hex.encodeHexString(
            messageDigest("SHA-1", start, end)).toLowerCase();
      }
    }
    if (null == this.digestSha1Hex) {
      throw new DescriptorParseException("Could not calculate descriptor "
          + "digest.");
    }
  }

  private byte[] messageDigest(String alg, int start, int end) {
    try {
      MessageDigest md = MessageDigest.getInstance(alg);
      md.update(this.rawDescriptorBytes, this.offset + start, end - start);
      return md.digest();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public String getDigestSha1Hex() {
    return this.digestSha1Hex;
  }

  private String digestSha256Base64;

  protected void setDigestSha256Base64(String digestSha256Base64) {
    this.digestSha256Base64 = digestSha256Base64;
  }

  protected void calculateDigestSha256Base64(String startToken,
      String endToken) throws DescriptorParseException {
    if (null == this.digestSha256Base64) {
      String ascii = new String(this.rawDescriptorBytes, this.offset,
          this.length, StandardCharsets.US_ASCII);
      int start = 0;
      if (null != startToken) {
        start = ascii.indexOf(startToken);
      }
      int end = -1;
      if (null == endToken) {
        end = ascii.length();
      } else if (ascii.contains(endToken)) {
        end = ascii.indexOf(endToken) + endToken.length();
      }
      if (start >= 0 && end >= 0 && end > start) {
        this.digestSha256Base64 = Base64.encodeBase64String(
            messageDigest("SHA-256", start, end)).replaceAll("=", "");
      }
    }
    if (null == this.digestSha256Base64) {
      throw new DescriptorParseException("Could not calculate descriptor "
          + "digest.");
    }
  }

  protected void calculateDigestSha256Base64(String startToken)
      throws DescriptorParseException {
    this.calculateDigestSha256Base64(startToken, null);
  }

  protected void calculateDigestSha256Base64() throws DescriptorParseException {
    this.calculateDigestSha256Base64(null, null);
  }

  public String getDigestSha256Base64() {
    return this.digestSha256Base64;
  }
}

