/* Copyright 2012--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.DescriptorParseException;
import org.torproject.descriptor.RelayNetworkStatus;

import java.io.File;
import java.util.*;

public class RelayNetworkStatusImpl extends NetworkStatusImpl
    implements RelayNetworkStatus {

  private static final long serialVersionUID = 2872005332125710108L;

  protected RelayNetworkStatusImpl(byte[] statusBytes, int[] offsetAndLength,
      File descriptorFile) throws DescriptorParseException {
    super(statusBytes, offsetAndLength, descriptorFile, true);
    this.splitAndParseParts(false);
    Set<Key> exactlyOnceKeys = EnumSet.of(
        Key.NETWORK_STATUS_VERSION, Key.DIR_SOURCE, Key.FINGERPRINT,
        Key.CONTACT, Key.DIR_SIGNING_KEY, Key.PUBLISHED);
    this.checkExactlyOnceKeys(exactlyOnceKeys);
    Set<Key> atMostOnceKeys = EnumSet.of(
        Key.DIR_OPTIONS, Key.CLIENT_VERSIONS, Key.SERVER_VERSIONS);
    this.checkAtMostOnceKeys(atMostOnceKeys);
    this.checkFirstKey(Key.NETWORK_STATUS_VERSION);
    this.clearParsedKeys();
    this.calculateDigestSha1Hex(Key.NETWORK_STATUS_VERSION.keyword + SP,
        NL + Key.DIRECTORY_SIGNATURE.keyword + SP);
  }

  protected void parseHeader(int offset, int length)
      throws DescriptorParseException {
    Scanner scanner = this.newScanner(offset, length).useDelimiter(NL);
    Key nextCrypto = Key.EMPTY;
    StringBuilder crypto = null;
    while (scanner.hasNext()) {
      String line = scanner.next();
      if (line.isEmpty()) {
        continue;
      }
      String[] parts = line.split("[ \t]+");
      Key key = Key.get(parts[0]);
      switch (key) {
        case NETWORK_STATUS_VERSION:
          this.parseNetworkStatusVersionLine(line);
          break;
        case DIR_SOURCE:
          this.parseDirSourceLine(line, parts);
          break;
        case FINGERPRINT:
          this.parseFingerprintLine(line, parts);
          break;
        case CONTACT:
          this.parseContactLine(line);
          break;
        case DIR_SIGNING_KEY:
          this.parseDirSigningKeyLine(line);
          nextCrypto = key;
          break;
        case CLIENT_VERSIONS:
          this.parseClientVersionsLine(line, parts);
          break;
        case SERVER_VERSIONS:
          this.parseServerVersionsLine(line, parts);
          break;
        case PUBLISHED:
          this.parsePublishedLine(line, parts);
          break;
        case DIR_OPTIONS:
          this.parseDirOptionsLine(parts);
          break;
        case CRYPTO_BEGIN:
          crypto = new StringBuilder();
          crypto.append(line).append(NL);
          break;
        case CRYPTO_END:
          if (null == crypto) {
            throw new DescriptorParseException(Key.CRYPTO_END + " before "
                + Key.CRYPTO_BEGIN);
          }
          crypto.append(line).append(NL);
          String cryptoString = crypto.toString();
          crypto = null;
          if (nextCrypto.equals(Key.DIR_SIGNING_KEY)) {
            this.dirSigningKey = cryptoString;
          } else {
            throw new DescriptorParseException("Unrecognized crypto "
                + "block in v2 network status.");
          }
          nextCrypto = Key.EMPTY;
          break;
        default:
          if (crypto != null) {
            crypto.append(line).append(NL);
          } else {
            if (this.unrecognizedLines == null) {
              this.unrecognizedLines = new ArrayList<>();
            }
            this.unrecognizedLines.add(line);
          }
      }
    }
  }

  protected void parseFooter(int offset, int length)
      throws DescriptorParseException {
    throw new DescriptorParseException("No directory footer expected in "
        + "v2 network status.");
  }

  protected void parseDirectorySignature(int offset, int length)
      throws DescriptorParseException {
    Scanner scanner = this.newScanner(offset, length).useDelimiter(NL);
    Key nextCrypto = Key.EMPTY;
    StringBuilder crypto = null;
    while (scanner.hasNext()) {
      String line = scanner.next();
      String[] parts = line.split("[ \t]+");
      Key key = Key.get(parts[0]);
      switch (key) {
        case DIRECTORY_SIGNATURE:
          this.parseDirectorySignatureLine(line, parts);
          nextCrypto = key;
          break;
        case CRYPTO_BEGIN:
          crypto = new StringBuilder();
          crypto.append(line).append(NL);
          break;
        case CRYPTO_END:
          if (null == crypto) {
            throw new DescriptorParseException(Key.CRYPTO_END + " before "
                + Key.CRYPTO_BEGIN);
          }
          crypto.append(line).append(NL);
          String cryptoString = crypto.toString();
          crypto = null;
          if (nextCrypto.equals(Key.DIRECTORY_SIGNATURE)) {
            this.directorySignature = cryptoString;
          } else {
            throw new DescriptorParseException("Unrecognized crypto "
                + "block in v2 network status.");
          }
          nextCrypto = Key.EMPTY;
          break;
        default:
          if (crypto != null) {
            crypto.append(line).append(NL);
          } else {
            if (this.unrecognizedLines == null) {
              this.unrecognizedLines = new ArrayList<>();
            }
            this.unrecognizedLines.add(line);
          }
      }
    }
  }

  private void parseNetworkStatusVersionLine(String line)
      throws DescriptorParseException {
    if (!line.equals(Key.NETWORK_STATUS_VERSION.keyword + SP + "2")) {
      throw new DescriptorParseException("Illegal network status version "
          + "number in line '" + line + "'.");
    }
    this.networkStatusVersion = 2;
  }

  private void parseDirSourceLine(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length != 4) {
      throw new DescriptorParseException("Illegal line '" + line
          + "' in v2 network status.");
    }
    if (parts[1].length() < 1) {
      throw new DescriptorParseException("Illegal hostname in '" + line
          + "'.");
    }
    this.hostname = parts[1];
    this.address = ParseHelper.parseIpv4Address(line, parts[2]);
    this.dirPort = ParseHelper.parsePort(line, parts[3]);
  }


  private void parseFingerprintLine(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length != 2) {
      throw new DescriptorParseException("Illegal line '" + line
          + "' in v2 network status.");
    }
    this.fingerprint = ParseHelper.parseTwentyByteHexString(line,
        parts[1]);
  }

  private void parseContactLine(String line) {
    if (line.length() > Key.CONTACT.keyword.length() + 1) {
      this.contactLine = line.substring(Key.CONTACT.keyword.length() + 1);
    } else {
      this.contactLine = "";
    }
  }

  private void parseDirSigningKeyLine(String line)
      throws DescriptorParseException {
    if (!line.equals(Key.DIR_SIGNING_KEY.keyword)) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
  }

  private void parseClientVersionsLine(String line, String[] parts)
      throws DescriptorParseException {
    this.recommendedClientVersions = this.parseClientOrServerVersions(
        line, parts);
  }

  private void parseServerVersionsLine(String line, String[] parts)
      throws DescriptorParseException {
    this.recommendedServerVersions = this.parseClientOrServerVersions(
        line, parts);
  }

  private void parsePublishedLine(String line, String[] parts)
      throws DescriptorParseException {
    this.publishedMillis = ParseHelper.parseTimestampAtIndex(line, parts,
        1, 2);
  }

  private void parseDirOptionsLine(String[] parts) {
    String[] dirOptions = new String[parts.length - 1];
    System.arraycopy(parts, 1, dirOptions, 0, parts.length - 1);
    this.dirOptions = dirOptions;
  }

  private void parseDirectorySignatureLine(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length < 2) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    this.nickname = ParseHelper.parseNickname(line, parts[1]);
  }

  private int networkStatusVersion;

  @Override
  public int getNetworkStatusVersion() {
    return this.networkStatusVersion;
  }

  private String hostname;

  @Override
  public String getHostname() {
    return this.hostname;
  }

  private String address;

  @Override
  public String getAddress() {
    return this.address;
  }

  private int dirPort;

  @Override
  public int getDirport() {
    return this.dirPort;
  }

  private String fingerprint;

  @Override
  public String getFingerprint() {
    return this.fingerprint;
  }

  private String contactLine;

  @Override
  public String getContactLine() {
    return this.contactLine;
  }

  private String dirSigningKey;

  @Override
  public String getDirSigningKey() {
    return this.dirSigningKey;
  }

  private String[] recommendedClientVersions;

  @Override
  public List<String> getRecommendedClientVersions() {
    return this.recommendedClientVersions == null ? null
        : Arrays.asList(this.recommendedClientVersions);
  }

  private String[] recommendedServerVersions;

  @Override
  public List<String> getRecommendedServerVersions() {
    return this.recommendedServerVersions == null ? null
        : Arrays.asList(this.recommendedServerVersions);
  }

  private long publishedMillis;

  @Override
  public long getPublishedMillis() {
    return this.publishedMillis;
  }

  private String[] dirOptions;

  @Override
  public SortedSet<String> getDirOptions() {
    return new TreeSet<>(Arrays.asList(this.dirOptions));
  }

  private String nickname;

  @Override
  public String getNickname() {
    return this.nickname;
  }

  private String directorySignature;

  @Override
  public String getDirectorySignature() {
    return this.directorySignature;
  }
}

