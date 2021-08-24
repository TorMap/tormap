/* Copyright 2011--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.DescriptorParseException;
import org.torproject.descriptor.DirSourceEntry;

import java.util.*;

import static org.torproject.descriptor.impl.DescriptorImpl.NL;
import static org.torproject.descriptor.impl.DescriptorImpl.SP;

public class DirSourceEntryImpl implements DirSourceEntry {

  private static final long serialVersionUID = 7276698015074138852L;

  private DescriptorImpl parent;

  private int offset;

  private int length;

  @Override
  public byte[] getDirSourceEntryBytes() {
    /* We need to pass this.offset and this.length, because the overloaded
     * method without arguments would use this.parent.offset and
     * this.parent.length as bounds, which is not what we want! */
    return this.parent.getRawDescriptorBytes(this.offset, this.length);
  }

  private List<String> unrecognizedLines;

  protected List<String> getAndClearUnrecognizedLines() {
    List<String> lines = this.unrecognizedLines;
    this.unrecognizedLines = null;
    return lines;
  }

  protected DirSourceEntryImpl(DescriptorImpl parent, int offset, int length)
      throws DescriptorParseException {
    this.parent = parent;
    this.offset = offset;
    this.length = length;
    this.parseDirSourceEntryBytes();
    this.checkAndClearKeys();
  }

  private Set<Key> exactlyOnceKeys = EnumSet.of(
      Key.DIR_SOURCE, Key.VOTE_DIGEST);

  private Set<Key> atMostOnceKeys = EnumSet.of(Key.CONTACT);

  private void parsedExactlyOnceKey(Key key)
      throws DescriptorParseException {
    if (!this.exactlyOnceKeys.contains(key)) {
      throw new DescriptorParseException("Duplicate '" + key.keyword
          + "' line in dir-source.");
    }
    this.exactlyOnceKeys.remove(key);
  }

  private void parsedAtMostOnceKey(Key key)
      throws DescriptorParseException {
    if (!this.atMostOnceKeys.contains(key)) {
      throw new DescriptorParseException("Duplicate " + key.keyword + "line "
          + "in dir-source.");
    }
    this.atMostOnceKeys.remove(key);
  }

  private void checkAndClearKeys() throws DescriptorParseException {
    if (!this.exactlyOnceKeys.isEmpty()) {
      for (Key key : this.exactlyOnceKeys) {
        throw new DescriptorParseException("dir-source does not contain a '"
            + key.keyword + "' line.");
      }
    }
    this.exactlyOnceKeys = null;
    this.atMostOnceKeys = null;
  }

  private void parseDirSourceEntryBytes()
      throws DescriptorParseException {
    /* We need to pass this.offset and this.length, because the overloaded
     * method without arguments would use this.parent.offset and
     * this.parent.length as bounds, which is not what we want! */
    Scanner scanner = this.parent.newScanner(this.offset, this.length)
        .useDelimiter(NL);
    boolean skipCrypto = false;
    while (scanner.hasNext()) {
      String line = scanner.next();
      String[] parts = line.split(SP);
      Key key = Key.get(parts[0]);
      switch (key) {
        case DIR_SOURCE:
          this.parseDirSourceLine(line);
          break;
        case CONTACT:
          this.parseContactLine(line);
          break;
        case VOTE_DIGEST:
          this.parseVoteDigestLine(line);
          break;
        case CRYPTO_BEGIN:
          skipCrypto = true;
          break;
        case CRYPTO_END:
          skipCrypto = false;
          break;
        default:
          if (!skipCrypto) {
            if (this.unrecognizedLines == null) {
              this.unrecognizedLines = new ArrayList<>();
            }
            this.unrecognizedLines.add(line);
          }
      }
    }
  }

  private void parseDirSourceLine(String line)
      throws DescriptorParseException {
    this.parsedExactlyOnceKey(Key.DIR_SOURCE);
    String[] parts = line.split("[ \t]+");
    if (parts.length != 7) {
      throw new DescriptorParseException("Invalid line '" + line + "'.");
    }
    String nickname = parts[1];
    if (nickname.endsWith("-legacy")) {
      nickname = nickname.substring(0, nickname.length()
          - "-legacy".length());
      this.isLegacy = true;
      this.parsedExactlyOnceKey(Key.VOTE_DIGEST);
    }
    this.nickname = ParseHelper.parseNickname(line, nickname);
    this.identity = ParseHelper.parseTwentyByteHexString(line, parts[2]);
    if (parts[3].length() < 1) {
      throw new DescriptorParseException("Illegal hostname in '" + line
          + "'.");
    }
    this.hostname = parts[3];
    this.ip = ParseHelper.parseIpv4Address(line, parts[4]);
    this.dirPort = ParseHelper.parsePort(line, parts[5]);
    this.orPort = ParseHelper.parsePort(line, parts[6]);
  }

  private void parseContactLine(String line)
      throws DescriptorParseException {
    this.parsedAtMostOnceKey(Key.CONTACT);
    if (line.length() > Key.CONTACT.keyword.length() + 1) {
      this.contactLine = line.substring(Key.CONTACT.keyword.length() + 1);
    } else {
      this.contactLine = "";
    }
  }

  private void parseVoteDigestLine(String line)
      throws DescriptorParseException {
    this.parsedExactlyOnceKey(Key.VOTE_DIGEST);
    String[] parts = line.split("[ \t]+");
    if (parts.length != 2) {
      throw new DescriptorParseException("Invalid line '" + line + "'.");
    }
    this.voteDigest = ParseHelper.parseTwentyByteHexString(line,
        parts[1]);
  }

  private String nickname;

  @Override
  public String getNickname() {
    return this.nickname;
  }

  private String identity;

  @Override
  public String getIdentity() {
    return this.identity;
  }

  private boolean isLegacy;

  @Override
  public boolean isLegacy() {
    return this.isLegacy;
  }

  private String hostname;

  @Override
  public String getHostname() {
    return this.hostname;
  }

  private String ip;

  @Override
  public String getIp() {
    return this.ip;
  }

  private int dirPort;

  @Override
  public int getDirPort() {
    return this.dirPort;
  }

  private int orPort;

  @Override
  public int getOrPort() {
    return this.orPort;
  }

  private String contactLine;

  @Override
  public String getContactLine() {
    return this.contactLine;
  }

  private String voteDigest;

  @Override
  public String getVoteDigestSha1Hex() {
    return this.voteDigest;
  }
}

