/* Copyright 2011--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.DescriptorParseException;
import org.torproject.descriptor.NetworkStatusEntry;

import java.util.*;

import static org.torproject.descriptor.impl.DescriptorImpl.NL;
import static org.torproject.descriptor.impl.DescriptorImpl.SP;

public class NetworkStatusEntryImpl implements NetworkStatusEntry {

  private static final long serialVersionUID = 8531564655041660420L;

  private DescriptorImpl parent;

  private int offset;

  private int length;

  @Override
  public byte[] getStatusEntryBytes() {
    /* We need to pass this.offset and this.length, because the overloaded
     * method without arguments would use this.parent.offset and
     * this.parent.length as bounds, which is not what we want! */
    return this.parent.getRawDescriptorBytes(this.offset, this.length);
  }

  private boolean microdescConsensus;

  private List<String> unrecognizedLines;

  protected List<String> getAndClearUnrecognizedLines() {
    List<String> lines = this.unrecognizedLines;
    this.unrecognizedLines = null;
    return lines;
  }

  private Map<String, Integer> flagIndexes;

  private Map<Integer, String> flagStrings;

  protected NetworkStatusEntryImpl(DescriptorImpl parent, int offset,
      int length, boolean microdescConsensus, Map<String, Integer> flagIndexes,
      Map<Integer, String> flagStrings) throws DescriptorParseException {
    this.parent = parent;
    this.offset = offset;
    this.length = length;
    this.microdescConsensus = microdescConsensus;
    this.flagIndexes = flagIndexes;
    this.flagStrings = flagStrings;
    this.parseStatusEntryBytes();
    this.clearAtMostOnceKeys();
  }

  private Set<Key> atMostOnceKeys = EnumSet.of(
      Key.S, Key.V, Key.PR, Key.W, Key.P);

  private void parsedAtMostOnceKey(Key key)
      throws DescriptorParseException {
    if (!this.atMostOnceKeys.contains(key)) {
      throw new DescriptorParseException("Duplicate '" + key.keyword
          + "' line in status entry.");
    }
    this.atMostOnceKeys.remove(key);
  }

  private void parseStatusEntryBytes() throws DescriptorParseException {
    /* We need to pass this.offset and this.length, because the overloaded
     * method without arguments would use this.parent.offset and
     * this.parent.length as bounds, which is not what we want! */
    Scanner scanner = this.parent.newScanner(this.offset, this.length)
        .useDelimiter(NL);
    String line;
    if (!scanner.hasNext() || !(line = scanner.next()).startsWith("r ")) {
      throw new DescriptorParseException("Status entry must start with "
          + "an r line.");
    }
    String[] rlineParts = line.split("[ \t]+");
    this.parseRLine(line, rlineParts);
    while (scanner.hasNext()) {
      line = scanner.next();
      String[] parts = !line.startsWith(Key.OPT.keyword + SP)
          ? line.split("[ \t]+")
          : line.substring(Key.OPT.keyword.length() + 1).split("[ \t]+");
      Key key = Key.get(parts[0]);
      switch (key) {
        case A:
          this.parseALine(line, parts);
          break;
        case S:
          this.parseSLine(parts);
          break;
        case V:
          this.parseVLine(line);
          break;
        case PR:
          this.parsePrLine(line, parts);
          break;
        case W:
          this.parseWLine(line, parts);
          break;
        case P:
          this.parsePLine(line, parts);
          break;
        case M:
          this.parseMLine(line, parts);
          break;
        case ID:
          this.parseIdLine(line, parts);
          break;
        default:
          if (this.unrecognizedLines == null) {
            this.unrecognizedLines = new ArrayList<>();
          }
          this.unrecognizedLines.add(line);
      }
    }
  }

  private void parseRLine(String line, String[] parts)
      throws DescriptorParseException {
    if ((!this.microdescConsensus && parts.length != 9)
        || (this.microdescConsensus && parts.length != 8)) {
      throw new DescriptorParseException("r line '" + line + "' has "
          + "fewer space-separated elements than expected.");
    }
    this.nickname = ParseHelper.parseNickname(line, parts[1]);
    this.fingerprint = ParseHelper.convertTwentyByteBase64StringToHex(line,
        parts[2]);
    int descriptorOffset = 0;
    if (!this.microdescConsensus) {
      this.descriptor = ParseHelper.convertTwentyByteBase64StringToHex(line,
          parts[3]);
      descriptorOffset = 1;
    }
    this.publishedMillis = ParseHelper.parseTimestampAtIndex(line, parts,
        3 + descriptorOffset, 4 + descriptorOffset);
    this.address = ParseHelper.parseIpv4Address(line,
        parts[5 + descriptorOffset]);
    this.orPort = ParseHelper.parsePort(line,
        parts[6 + descriptorOffset]);
    this.dirPort = ParseHelper.parsePort(line,
        parts[7 + descriptorOffset]);
  }

  private void parseALine(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length != 2) {
      throw new DescriptorParseException("Invalid line '" + line + "' in "
          + "status entry.");
    }
    /* TODO Add more checks. */
    this.orAddresses.add(parts[1]);
  }

  private void parseSLine(String[] parts)
      throws DescriptorParseException {
    this.parsedAtMostOnceKey(Key.S);
    BitSet flags = new BitSet(this.flagIndexes.size());
    for (int i = 1; i < parts.length; i++) {
      String flag = parts[i];
      if (!this.flagIndexes.containsKey(flag)) {
        this.flagStrings.put(this.flagIndexes.size(), flag);
        this.flagIndexes.put(flag, this.flagIndexes.size());
      }
      flags.set(this.flagIndexes.get(flag));
    }
    this.flags = flags;
  }

  private void parseVLine(String line)
      throws DescriptorParseException {
    this.parsedAtMostOnceKey(Key.V);
    String noOptLine = line;
    if (noOptLine.startsWith(Key.OPT.keyword + SP)) {
      noOptLine = noOptLine.substring(4);
    }
    if (noOptLine.length() < 3) {
      throw new DescriptorParseException("Invalid line '" + line + "' in "
          + "status entry.");
    } else {
      this.version = noOptLine.substring(2);
    }
  }

  private void parsePrLine(String line, String[] parts)
      throws DescriptorParseException {
    this.parsedAtMostOnceKey(Key.PR);
    this.protocols = ParseHelper.parseProtocolVersions(line, line, parts);
  }

  private void parseWLine(String line, String[] parts)
      throws DescriptorParseException {
    this.parsedAtMostOnceKey(Key.W);
    SortedMap<String, Integer> pairs =
        ParseHelper.parseKeyValueIntegerPairs(line, parts, 1);
    if (pairs.isEmpty()) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    if (pairs.containsKey("Bandwidth")) {
      this.bandwidth = pairs.remove("Bandwidth");
    }
    if (pairs.containsKey("Measured")) {
      this.measured = pairs.remove("Measured");
    }
    if (pairs.containsKey("Unmeasured")) {
      this.unmeasured = pairs.remove("Unmeasured") == 1L;
    }
    /* Ignore unknown key-value pair. */
  }

  private void parsePLine(String line, String[] parts)
      throws DescriptorParseException {
    this.parsedAtMostOnceKey(Key.P);
    boolean isValid = true;
    if (parts.length != 3) {
      isValid = false;
    } else {
      switch (parts[1]) {
        case "accept":
        case "reject":
          this.defaultPolicy = parts[1];
          this.portList = parts[2];
          String[] ports = parts[2].split(",", -1);
          for (String port : ports) {
            if (port.length() < 1) {
              isValid = false;
              break;
            }
          }
          break;
        default:
          isValid = false;
      }
    }
    if (!isValid) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
  }

  private void parseMLine(String line, String[] parts)
      throws DescriptorParseException {
    if (this.microdescriptorDigests == null) {
      this.microdescriptorDigests = new HashSet<>();
    }
    if (parts.length == 2) {
      ParseHelper.verifyThirtyTwoByteBase64String(line, parts[1]);
      this.microdescriptorDigests.add(parts[1]);
    } else if (parts.length == 3 && parts[2].length() > 7) {
      /* 7 == "sha256=".length() */
      ParseHelper.verifyThirtyTwoByteBase64String(line,
          parts[2].substring(7));
      this.microdescriptorDigests.add(parts[2].substring(7));
    }
  }

  private void parseIdLine(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length != 3 || !"ed25519".equals(parts[1])) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    } else if ("none".equals(parts[2])) {
      this.masterKeyEd25519 = "none";
    } else {
      ParseHelper.verifyThirtyTwoByteBase64String(line, parts[2]);
      this.masterKeyEd25519 = parts[2];
    }
  }

  private void clearAtMostOnceKeys() {
    this.atMostOnceKeys = null;
  }

  private String nickname;

  @Override
  public String getNickname() {
    return this.nickname;
  }

  private String fingerprint;

  @Override
  public String getFingerprint() {
    return this.fingerprint;
  }

  private String descriptor;

  @Override
  public String getDescriptor() {
    return this.descriptor;
  }

  private long publishedMillis;

  @Override
  public long getPublishedMillis() {
    return this.publishedMillis;
  }

  private String address;

  @Override
  public String getAddress() {
    return this.address;
  }

  private int orPort;

  @Override
  public int getOrPort() {
    return this.orPort;
  }

  private int dirPort;

  @Override
  public int getDirPort() {
    return this.dirPort;
  }

  private Set<String> microdescriptorDigests;

  @Override
  public Set<String> getMicrodescriptorDigestsSha256Base64() {
    return this.microdescriptorDigests == null ? null
        : new HashSet<>(this.microdescriptorDigests);
  }

  private List<String> orAddresses = new ArrayList<>();

  @Override
  public List<String> getOrAddresses() {
    return new ArrayList<>(this.orAddresses);
  }

  private BitSet flags;

  @Override
  public SortedSet<String> getFlags() {
    SortedSet<String> result = new TreeSet<>();
    if (this.flags != null) {
      for (int i = this.flags.nextSetBit(0); i >= 0;
          i = this.flags.nextSetBit(i + 1)) {
        result.add(this.flagStrings.get(i));
      }
    }
    return result;
  }

  private String version;

  @Override
  public String getVersion() {
    return this.version;
  }

  private SortedMap<String, SortedSet<Long>> protocols;

  @Override
  public SortedMap<String, SortedSet<Long>> getProtocols() {
    return this.protocols;
  }

  private long bandwidth = -1L;

  @Override
  public long getBandwidth() {
    return this.bandwidth;
  }

  private long measured = -1L;

  @Override
  public long getMeasured() {
    return this.measured;
  }

  private boolean unmeasured = false;

  @Override
  public boolean getUnmeasured() {
    return this.unmeasured;
  }

  private String defaultPolicy;

  @Override
  public String getDefaultPolicy() {
    return this.defaultPolicy;
  }

  private String portList;

  @Override
  public String getPortList() {
    return this.portList;
  }

  private String masterKeyEd25519;

  @Override
  public String getMasterKeyEd25519() {
    return this.masterKeyEd25519;
  }
}

