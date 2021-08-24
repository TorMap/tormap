/* Copyright 2011--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.DescriptorParseException;
import org.torproject.descriptor.RelayNetworkStatusVote;

import java.io.File;
import java.util.*;

/* Contains a network status vote. */
public class RelayNetworkStatusVoteImpl extends NetworkStatusImpl
    implements RelayNetworkStatusVote {

  private static final long serialVersionUID = -39488588769922984L;

  protected RelayNetworkStatusVoteImpl(byte[] voteBytes, int[] offsetAndLength,
      File descriptorFile)
      throws DescriptorParseException {
    super(voteBytes, offsetAndLength, descriptorFile, false);
    this.splitAndParseParts(false);
    Set<Key> exactlyOnceKeys = EnumSet.of(
        Key.VOTE_STATUS, Key.PUBLISHED, Key.VALID_AFTER, Key.FRESH_UNTIL,
        Key.VALID_UNTIL, Key.VOTING_DELAY, Key.KNOWN_FLAGS, Key.DIR_SOURCE,
        Key.DIR_KEY_CERTIFICATE_VERSION, Key.FINGERPRINT, Key.DIR_KEY_PUBLISHED,
        Key.DIR_KEY_EXPIRES, Key.DIR_IDENTITY_KEY, Key.DIR_SIGNING_KEY,
        Key.DIR_KEY_CERTIFICATION);
    this.checkExactlyOnceKeys(exactlyOnceKeys);
    Set<Key> atMostOnceKeys = EnumSet.of(
        Key.CONSENSUS_METHODS, Key.CLIENT_VERSIONS, Key.SERVER_VERSIONS,
        Key.RECOMMENDED_CLIENT_PROTOCOLS, Key.RECOMMENDED_RELAY_PROTOCOLS,
        Key.REQUIRED_CLIENT_PROTOCOLS, Key.REQUIRED_RELAY_PROTOCOLS,
        Key.FLAG_THRESHOLDS, Key.PARAMS, Key.CONTACT,
        Key.SHARED_RAND_PARTICIPATE, Key.SHARED_RAND_PREVIOUS_VALUE,
        Key.SHARED_RAND_CURRENT_VALUE, Key.BANDWIDTH_FILE_HEADERS,
        Key.BANDWIDTH_FILE_DIGEST, Key.LEGACY_KEY, Key.DIR_KEY_CROSSCERT,
        Key.DIR_ADDRESS, Key.DIRECTORY_FOOTER);
    this.checkAtMostOnceKeys(atMostOnceKeys);
    this.checkAtLeastOnceKeys(EnumSet.of(Key.DIRECTORY_SIGNATURE));
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
      String[] parts = line.split("[ \t]+");
      Key key = Key.get(parts[0]);
      switch (key) {
        case NETWORK_STATUS_VERSION:
          this.parseNetworkStatusVersionLine(line);
          break;
        case VOTE_STATUS:
          this.parseVoteStatusLine(line, parts);
          break;
        case CONSENSUS_METHODS:
          this.parseConsensusMethodsLine(line, parts);
          break;
        case PUBLISHED:
          this.parsePublishedLine(line, parts);
          break;
        case VALID_AFTER:
          this.parseValidAfterLine(line, parts);
          break;
        case FRESH_UNTIL:
          this.parseFreshUntilLine(line, parts);
          break;
        case VALID_UNTIL:
          this.parseValidUntilLine(line, parts);
          break;
        case VOTING_DELAY:
          this.parseVotingDelayLine(line, parts);
          break;
        case CLIENT_VERSIONS:
          this.parseClientVersionsLine(line, parts);
          break;
        case SERVER_VERSIONS:
          this.parseServerVersionsLine(line, parts);
          break;
        case RECOMMENDED_CLIENT_PROTOCOLS:
          this.parseRecommendedClientProtocolsLine(line, parts);
          break;
        case RECOMMENDED_RELAY_PROTOCOLS:
          this.parseRecommendedRelayProtocolsLine(line, parts);
          break;
        case REQUIRED_CLIENT_PROTOCOLS:
          this.parseRequiredClientProtocolsLine(line, parts);
          break;
        case REQUIRED_RELAY_PROTOCOLS:
          this.parseRequiredRelayProtocolsLine(line, parts);
          break;
        case PACKAGE:
          this.parsePackageLine(line, parts);
          break;
        case KNOWN_FLAGS:
          this.parseKnownFlagsLine(line, parts);
          break;
        case FLAG_THRESHOLDS:
          this.parseFlagThresholdsLine(line, parts);
          break;
        case PARAMS:
          this.parseParamsLine(line, parts);
          break;
        case DIR_SOURCE:
          this.parseDirSourceLine(line, parts);
          break;
        case CONTACT:
          this.parseContactLine(line);
          break;
        case SHARED_RAND_PARTICIPATE:
          this.parseSharedRandParticipateLine(line, parts);
          break;
        case SHARED_RAND_COMMIT:
          this.parseSharedRandCommitLine(line);
          break;
        case SHARED_RAND_PREVIOUS_VALUE:
          this.parseSharedRandPreviousValueLine(line, parts);
          break;
        case SHARED_RAND_CURRENT_VALUE:
          this.parseSharedRandCurrentValueLine(line, parts);
          break;
        case BANDWIDTH_FILE_HEADERS:
          this.parseBandwidthFileHeaders(line, parts);
          break;
        case BANDWIDTH_FILE_DIGEST:
          this.parseBandwidthFileDigest(line, parts);
          break;
        case DIR_KEY_CERTIFICATE_VERSION:
          this.parseDirKeyCertificateVersionLine(line, parts);
          break;
        case DIR_ADDRESS:
          /* Nothing new to learn here.  Also, this line hasn't been observed
           * "in the wild" yet.  Maybe it's just an urban legend. */
          break;
        case FINGERPRINT:
          this.parseFingerprintLine(line, parts);
          break;
        case LEGACY_DIR_KEY:
          this.parseLegacyDirKeyLine(line, parts);
          break;
        case DIR_KEY_PUBLISHED:
          this.parseDirKeyPublished(line, parts);
          break;
        case DIR_KEY_EXPIRES:
          this.parseDirKeyExpiresLine(line, parts);
          break;
        case DIR_IDENTITY_KEY:
          this.parseDirIdentityKeyLine(line);
          nextCrypto = key;
          break;
        case DIR_SIGNING_KEY:
          this.parseDirSigningKeyLine(line);
          nextCrypto = key;
          break;
        case DIR_KEY_CROSSCERT:
          this.parseDirKeyCrosscertLine(line);
          nextCrypto = key;
          break;
        case DIR_KEY_CERTIFICATION:
          this.parseDirKeyCertificationLine(line);
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
          switch (nextCrypto) {
            case DIR_IDENTITY_KEY:
              this.dirIdentityKey = cryptoString;
              break;
            case DIR_SIGNING_KEY:
              this.dirSigningKey = cryptoString;
              break;
            case DIR_KEY_CROSSCERT:
              this.dirKeyCrosscert = cryptoString;
              break;
            case DIR_KEY_CERTIFICATION:
              this.dirKeyCertification = cryptoString;
              break;
            default:
              throw new DescriptorParseException("Unrecognized crypto "
                  + "block in vote.");
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
    if (!line.equals(Key.NETWORK_STATUS_VERSION.keyword + SP + "3")) {
      throw new DescriptorParseException("Illegal network status version "
          + "number in line '" + line + "'.");
    }
    this.networkStatusVersion = 3;
  }

  private void parseVoteStatusLine(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length != 2 || !parts[1].equals("vote")) {
      throw new DescriptorParseException("Line '" + line + "' indicates "
          + "that this is not a vote.");
    }
  }

  private void parseConsensusMethodsLine(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length < 2) {
      throw new DescriptorParseException("Illegal line '" + line
          + "' in vote.");
    }
    Integer[] consensusMethods = new Integer[parts.length - 1];
    for (int i = 1; i < parts.length; i++) {
      int consensusMethod = -1;
      try {
        consensusMethod = Integer.parseInt(parts[i]);
      } catch (NumberFormatException e) {
        /* We'll notice below that consensusMethod is still -1. */
      }
      if (consensusMethod < 1) {
        throw new DescriptorParseException("Illegal consensus method "
            + "number in line '" + line + "'.");
      }
      consensusMethods[i - 1] = consensusMethod;
    }
    this.consensusMethods = consensusMethods;
  }

  private void parsePublishedLine(String line, String[] parts)
      throws DescriptorParseException {
    this.publishedMillis = ParseHelper.parseTimestampAtIndex(line, parts,
        1, 2);
  }

  private void parseValidAfterLine(String line, String[] parts)
      throws DescriptorParseException {
    this.validAfterMillis = ParseHelper.parseTimestampAtIndex(line, parts,
        1, 2);
  }

  private void parseFreshUntilLine(String line, String[] parts)
      throws DescriptorParseException {
    this.freshUntilMillis = ParseHelper.parseTimestampAtIndex(line, parts,
        1, 2);
  }

  private void parseValidUntilLine(String line, String[] parts)
      throws DescriptorParseException {
    this.validUntilMillis = ParseHelper.parseTimestampAtIndex(line, parts,
        1, 2);
  }

  private void parseVotingDelayLine(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length != 3) {
      throw new DescriptorParseException("Wrong number of values in line "
          + "'" + line + "'.");
    }
    try {
      this.voteSeconds = Long.parseLong(parts[1]);
      this.distSeconds = Long.parseLong(parts[2]);
    } catch (NumberFormatException e) {
      throw new DescriptorParseException("Illegal values in line '" + line
          + "'.");
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

  private void parseRecommendedClientProtocolsLine(String line, String[] parts)
      throws DescriptorParseException {
    this.recommendedClientProtocols = ParseHelper.parseProtocolVersions(line,
        line, parts);
  }

  private void parseRecommendedRelayProtocolsLine(String line, String[] parts)
      throws DescriptorParseException {
    this.recommendedRelayProtocols = ParseHelper.parseProtocolVersions(line,
        line, parts);
  }

  private void parseRequiredClientProtocolsLine(String line, String[] parts)
      throws DescriptorParseException {
    this.requiredClientProtocols = ParseHelper.parseProtocolVersions(line,
        line, parts);
  }

  private void parseRequiredRelayProtocolsLine(String line, String[] parts)
      throws DescriptorParseException {
    this.requiredRelayProtocols = ParseHelper.parseProtocolVersions(line, line,
        parts);
  }

  private void parsePackageLine(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length < 5) {
      throw new DescriptorParseException("Wrong number of values in line "
          + "'" + line + "'.");
    }
    if (this.packageLines == null) {
      this.packageLines = new ArrayList<>();
    }
    this.packageLines.add(line.substring(Key.PACKAGE.keyword.length() + 1));
  }

  private void parseKnownFlagsLine(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length < 2) {
      throw new DescriptorParseException("No known flags in line '" + line
          + "'.");
    }
    String[] knownFlags = new String[parts.length - 1];
    System.arraycopy(parts, 1, knownFlags, 0, parts.length - 1);
    this.knownFlags = knownFlags;
  }

  private void parseFlagThresholdsLine(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length < 2) {
      throw new DescriptorParseException("No flag thresholds in line '"
          + line + "'.");
    }
    SortedMap<String, String> flagThresholds =
        ParseHelper.parseKeyValueStringPairs(line, parts, 1);
    try {
      for (Map.Entry<String, String> e : flagThresholds.entrySet()) {
        switch (e.getKey()) {
          case "stable-uptime":
            this.stableUptime = Long.parseLong(e.getValue());
            break;
          case "stable-mtbf":
            this.stableMtbf = Long.parseLong(e.getValue());
            break;
          case "fast-speed":
            this.fastBandwidth = Long.parseLong(e.getValue());
            break;
          case "guard-wfu":
            this.guardWfu = Double.parseDouble(e.getValue()
                .replaceAll("%", ""));
            break;
          case "guard-tk":
            this.guardTk = Long.parseLong(e.getValue());
            break;
          case "guard-bw-inc-exits":
            this.guardBandwidthIncludingExits =
                Long.parseLong(e.getValue());
            break;
          case "guard-bw-exc-exits":
            this.guardBandwidthExcludingExits =
                Long.parseLong(e.getValue());
            break;
          case "enough-mtbf":
            this.enoughMtbfInfo = Integer.parseInt(e.getValue());
            break;
          case "ignoring-advertised-bws":
            this.ignoringAdvertisedBws = Integer.parseInt(e.getValue());
            break;
          default:
            // empty
        }
      }
    } catch (NumberFormatException ex) {
      throw new DescriptorParseException("Illegal value in line '"
          + line + "'.");
    }
  }

  private void parseParamsLine(String line, String[] parts)
      throws DescriptorParseException {
    this.consensusParams = ParseHelper.parseKeyValueIntegerPairs(line,
        parts, 1);
  }

  private void parseDirSourceLine(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length != 7) {
      throw new DescriptorParseException("Illegal line '" + line
          + "' in vote.");
    }
    this.nickname = ParseHelper.parseNickname(line, parts[1]);
    this.identity = ParseHelper.parseTwentyByteHexString(line, parts[2]);
    if (parts[3].length() < 1) {
      throw new DescriptorParseException("Illegal hostname in '" + line
          + "'.");
    }
    this.hostname = parts[3];
    this.address = ParseHelper.parseIpv4Address(line, parts[4]);
    this.dirPort = ParseHelper.parsePort(line, parts[5]);
    this.orPort = ParseHelper.parsePort(line, parts[6]);
  }

  private void parseContactLine(String line) {
    if (line.length() > Key.CONTACT.keyword.length() + 1) {
      this.contactLine = line.substring(Key.CONTACT.keyword.length() + 1);
    } else {
      this.contactLine = "";
    }
  }

  private void parseSharedRandParticipateLine(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length != 1) {
      throw new DescriptorParseException("Illegal line '" + line
          + "' in vote.");
    }
    this.sharedRandParticipate = true;
  }

  private void parseSharedRandCommitLine(String line) {
    if (this.sharedRandCommitLines == null) {
      this.sharedRandCommitLines = new ArrayList<>();
    }
    this.sharedRandCommitLines.add(line);
  }

  private void parseSharedRandPreviousValueLine(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length != 3) {
      throw new DescriptorParseException("Illegal line '" + line
          + "' in vote.");
    }
    try {
      this.sharedRandPreviousNumReveals = Integer.parseInt(parts[1]);
    } catch (NumberFormatException e) {
      throw new DescriptorParseException("Illegal line '" + line
          + "' in vote.");
    }
    this.sharedRandPreviousValue = parts[2];
  }

  private void parseSharedRandCurrentValueLine(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length != 3) {
      throw new DescriptorParseException("Illegal line '" + line
          + "' in vote.");
    }
    try {
      this.sharedRandCurrentNumReveals = Integer.parseInt(parts[1]);
    } catch (NumberFormatException e) {
      throw new DescriptorParseException("Illegal line '" + line
          + "' in vote.");
    }
    this.sharedRandCurrentValue = parts[2];
  }

  protected void parseBandwidthFileHeaders(String line, String[] parts)
      throws DescriptorParseException {
    this.bandwidthFileHeaders
        = ParseHelper.parseKeyValueStringPairs(line, parts, 1);
  }

  protected void parseBandwidthFileDigest(String line, String[] parts)
      throws DescriptorParseException {
    for (int i = 1; i < parts.length; i++) {
      String part = parts[i];
      if (part.startsWith("sha256=")) {
        /* 7 == "sha256=".length() */
        ParseHelper.verifyThirtyTwoByteBase64String(line, part.substring(7));
        this.bandwidthFileDigestSha256Base64 = part.substring(7);
      }
    }
  }

  private void parseDirKeyCertificateVersionLine(String line,
      String[] parts) throws DescriptorParseException {
    if (parts.length != 2) {
      throw new DescriptorParseException("Illegal line '" + line
          + "' in vote.");
    }
    try {
      this.dirKeyCertificateVersion = Integer.parseInt(parts[1]);
    } catch (NumberFormatException e) {
      throw new DescriptorParseException("Illegal dir key certificate "
          + "version in line '" + line + "'.");
    }
    if (this.dirKeyCertificateVersion < 1) {
      throw new DescriptorParseException("Illegal dir key certificate "
          + "version in line '" + line + "'.");
    }
  }

  private void parseFingerprintLine(String line, String[] parts)
      throws DescriptorParseException {
    /* Nothing new to learn here.  We already know the fingerprint from
     * the dir-source line.  But we should at least check that there's a
     * valid fingerprint in this line. */
    if (parts.length != 2) {
      throw new DescriptorParseException("Illegal line '" + line
          + "' in vote.");
    }
    ParseHelper.parseTwentyByteHexString(line, parts[1]);
  }

  private void parseLegacyDirKeyLine(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length != 2) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    this.legacyDirKey = ParseHelper.parseTwentyByteHexString(line,
        parts[1]);
  }

  private void parseDirKeyPublished(String line, String[] parts)
      throws DescriptorParseException {
    this.dirKeyPublishedMillis = ParseHelper.parseTimestampAtIndex(line,
        parts, 1, 2);
  }

  private void parseDirKeyExpiresLine(String line, String[] parts)
      throws DescriptorParseException {
    this.dirKeyExpiresMillis = ParseHelper.parseTimestampAtIndex(line,
        parts, 1, 2);
  }

  private void parseDirIdentityKeyLine(String line)
      throws DescriptorParseException {
    if (!line.equals(Key.DIR_IDENTITY_KEY.keyword)) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
  }

  private void parseDirSigningKeyLine(String line)
      throws DescriptorParseException {
    if (!line.equals(Key.DIR_SIGNING_KEY.keyword)) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
  }

  private void parseDirKeyCrosscertLine(String line)
      throws DescriptorParseException {
    if (!line.equals(Key.DIR_KEY_CROSSCERT.keyword)) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
  }

  private void parseDirKeyCertificationLine(String line)
      throws DescriptorParseException {
    if (!line.equals(Key.DIR_KEY_CERTIFICATION.keyword)) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
  }

  protected void parseFooter(int offset, int length) {
    Scanner scanner = this.newScanner(offset, length).useDelimiter(NL);
    while (scanner.hasNext()) {
      String line = scanner.next();
      if (!line.equals(Key.DIRECTORY_FOOTER.keyword)) {
        if (this.unrecognizedLines == null) {
          this.unrecognizedLines = new ArrayList<>();
        }
        this.unrecognizedLines.add(line);
      }
    }
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

  private int orPort;

  @Override
  public int getOrport() {
    return this.orPort;
  }

  private String contactLine;

  @Override
  public String getContactLine() {
    return this.contactLine;
  }

  private boolean sharedRandParticipate = false;

  @Override
  public boolean isSharedRandParticipate() {
    return this.sharedRandParticipate;
  }

  private List<String> sharedRandCommitLines = null;

  @Override
  public List<String> getSharedRandCommitLines() {
    return this.sharedRandCommitLines;
  }

  private int sharedRandPreviousNumReveals = -1;

  @Override
  public int getSharedRandPreviousNumReveals() {
    return this.sharedRandPreviousNumReveals;
  }

  private String sharedRandPreviousValue = null;

  @Override
  public String getSharedRandPreviousValue() {
    return this.sharedRandPreviousValue;
  }

  private int sharedRandCurrentNumReveals = -1;

  @Override
  public int getSharedRandCurrentNumReveals() {
    return this.sharedRandCurrentNumReveals;
  }

  private String sharedRandCurrentValue = null;

  @Override
  public String getSharedRandCurrentValue() {
    return this.sharedRandCurrentValue;
  }

  private SortedMap<String, String> bandwidthFileHeaders;

  @Override
  public SortedMap<String, String> getBandwidthFileHeaders() {
    return this.bandwidthFileHeaders;
  }

  private String bandwidthFileDigestSha256Base64;

  @Override
  public String getBandwidthFileDigestSha256Base64() {
    return this.bandwidthFileDigestSha256Base64;
  }

  private int dirKeyCertificateVersion;

  @Override
  public int getDirKeyCertificateVersion() {
    return this.dirKeyCertificateVersion;
  }

  private String legacyDirKey;

  @Override
  public String getLegacyDirKey() {
    return this.legacyDirKey;
  }

  private long dirKeyPublishedMillis;

  @Override
  public long getDirKeyPublishedMillis() {
    return this.dirKeyPublishedMillis;
  }

  private long dirKeyExpiresMillis;

  @Override
  public long getDirKeyExpiresMillis() {
    return this.dirKeyExpiresMillis;
  }

  private String dirIdentityKey;

  @Override
  public String getDirIdentityKey() {
    return this.dirIdentityKey;
  }

  private String dirSigningKey;

  @Override
  public String getDirSigningKey() {
    return this.dirSigningKey;
  }

  private String dirKeyCrosscert;

  @Override
  public String getDirKeyCrosscert() {
    return this.dirKeyCrosscert;
  }

  private String dirKeyCertification;

  @Override
  public String getDirKeyCertification() {
    return this.dirKeyCertification;
  }

  private int networkStatusVersion;

  @Override
  public int getNetworkStatusVersion() {
    return this.networkStatusVersion;
  }

  private Integer[] consensusMethods;

  @Override
  public List<Integer> getConsensusMethods() {
    return this.consensusMethods == null ? null
        : Arrays.asList(this.consensusMethods);
  }

  private long publishedMillis;

  @Override
  public long getPublishedMillis() {
    return this.publishedMillis;
  }

  private long validAfterMillis;

  @Override
  public long getValidAfterMillis() {
    return this.validAfterMillis;
  }

  private long freshUntilMillis;

  @Override
  public long getFreshUntilMillis() {
    return this.freshUntilMillis;
  }

  private long validUntilMillis;

  @Override
  public long getValidUntilMillis() {
    return this.validUntilMillis;
  }

  private long voteSeconds;

  @Override
  public long getVoteSeconds() {
    return this.voteSeconds;
  }

  private long distSeconds;

  @Override
  public long getDistSeconds() {
    return this.distSeconds;
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

  private SortedMap<String, SortedSet<Long>> recommendedClientProtocols;

  @Override
  public SortedMap<String, SortedSet<Long>> getRecommendedClientProtocols() {
    return this.recommendedClientProtocols;
  }

  private SortedMap<String, SortedSet<Long>> recommendedRelayProtocols;

  @Override
  public SortedMap<String, SortedSet<Long>> getRecommendedRelayProtocols() {
    return this.recommendedRelayProtocols;
  }

  private SortedMap<String, SortedSet<Long>> requiredClientProtocols;

  @Override
  public SortedMap<String, SortedSet<Long>> getRequiredClientProtocols() {
    return this.requiredClientProtocols;
  }

  private SortedMap<String, SortedSet<Long>> requiredRelayProtocols;

  @Override
  public SortedMap<String, SortedSet<Long>> getRequiredRelayProtocols() {
    return this.requiredRelayProtocols;
  }

  private List<String> packageLines;

  @Override
  public List<String> getPackageLines() {
    return this.packageLines == null ? null
        : new ArrayList<>(this.packageLines);
  }

  private String[] knownFlags;

  @Override
  public SortedSet<String> getKnownFlags() {
    return new TreeSet<>(Arrays.asList(this.knownFlags));
  }

  private long stableUptime = -1L;

  @Override
  public long getStableUptime() {
    return this.stableUptime;
  }

  private long stableMtbf = -1L;

  @Override
  public long getStableMtbf() {
    return this.stableMtbf;
  }

  private long fastBandwidth = -1L;

  @Override
  public long getFastBandwidth() {
    return this.fastBandwidth;
  }

  private double guardWfu = -1.0;

  @Override
  public double getGuardWfu() {
    return this.guardWfu;
  }

  private long guardTk = -1L;

  @Override
  public long getGuardTk() {
    return this.guardTk;
  }

  private long guardBandwidthIncludingExits = -1L;

  @Override
  public long getGuardBandwidthIncludingExits() {
    return this.guardBandwidthIncludingExits;
  }

  private long guardBandwidthExcludingExits = -1L;

  @Override
  public long getGuardBandwidthExcludingExits() {
    return this.guardBandwidthExcludingExits;
  }

  private int enoughMtbfInfo = -1;

  @Override
  public int getEnoughMtbfInfo() {
    return this.enoughMtbfInfo;
  }

  private int ignoringAdvertisedBws = -1;

  @Override
  public int getIgnoringAdvertisedBws() {
    return this.ignoringAdvertisedBws;
  }

  private SortedMap<String, Integer> consensusParams;

  @Override
  public SortedMap<String, Integer> getConsensusParams() {
    return this.consensusParams == null ? null
        : new TreeMap<>(this.consensusParams);
  }
}

