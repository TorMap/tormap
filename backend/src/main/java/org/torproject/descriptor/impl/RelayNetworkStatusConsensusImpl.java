/* Copyright 2011--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.DescriptorParseException;
import org.torproject.descriptor.RelayNetworkStatusConsensus;

import java.io.File;
import java.util.*;

/* Contains a network status consensus or microdesc consensus. */
public class RelayNetworkStatusConsensusImpl extends NetworkStatusImpl
    implements RelayNetworkStatusConsensus {

  private static final long serialVersionUID = -2852336205396172171L;

  protected RelayNetworkStatusConsensusImpl(byte[] consensusBytes,
      int[] offsetAndLimit, File descriptorFile)
      throws DescriptorParseException {
    super(consensusBytes, offsetAndLimit, descriptorFile, false);
    this.splitAndParseParts(true);
    Set<Key> exactlyOnceKeys = EnumSet.of(
        Key.VOTE_STATUS, Key.CONSENSUS_METHOD, Key.VALID_AFTER, Key.FRESH_UNTIL,
        Key.VALID_UNTIL, Key.VOTING_DELAY, Key.KNOWN_FLAGS);
    this.checkExactlyOnceKeys(exactlyOnceKeys);
    Set<Key> atMostOnceKeys = EnumSet.of(
        Key.CLIENT_VERSIONS, Key.SERVER_VERSIONS,
        Key.RECOMMENDED_CLIENT_PROTOCOLS, Key.RECOMMENDED_RELAY_PROTOCOLS,
        Key.REQUIRED_CLIENT_PROTOCOLS, Key.REQUIRED_RELAY_PROTOCOLS, Key.PARAMS,
        Key.SHARED_RAND_PREVIOUS_VALUE, Key.SHARED_RAND_CURRENT_VALUE,
        Key.DIRECTORY_FOOTER, Key.BANDWIDTH_WEIGHTS);
    this.checkAtMostOnceKeys(atMostOnceKeys);
    this.checkFirstKey(Key.NETWORK_STATUS_VERSION);
    this.clearParsedKeys();
    this.calculateDigestSha1Hex(Key.NETWORK_STATUS_VERSION.keyword + SP,
        NL + Key.DIRECTORY_SIGNATURE.keyword + SP);
  }

  protected void parseHeader(int offset, int length)
      throws DescriptorParseException {
    Scanner scanner = this.newScanner(offset, length).useDelimiter(NL);
    while (scanner.hasNext()) {
      String line = scanner.next();
      String[] parts = line.split("[ \t]+");
      Key key = Key.get(parts[0]);
      switch (key) {
        case NETWORK_STATUS_VERSION:
          this.parseNetworkStatusVersionLine(line, parts);
          break;
        case VOTE_STATUS:
          this.parseVoteStatusLine(line, parts);
          break;
        case CONSENSUS_METHOD:
          this.parseConsensusMethodLine(line, parts);
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
        case PARAMS:
          this.parseParamsLine(line, parts);
          break;
        case SHARED_RAND_PREVIOUS_VALUE:
          this.parseSharedRandPreviousValueLine(line, parts);
          break;
        case SHARED_RAND_CURRENT_VALUE:
          this.parseSharedRandCurrentValueLine(line, parts);
          break;
        default:
          if (this.unrecognizedLines == null) {
            this.unrecognizedLines = new ArrayList<>();
          }
          this.unrecognizedLines.add(line);
      }
    }
  }

  private boolean microdescConsensus = false;

  protected void parseStatusEntry(int offset, int length)
      throws DescriptorParseException {
    NetworkStatusEntryImpl statusEntry = new NetworkStatusEntryImpl(this,
        offset, length, this.microdescConsensus, this.flagIndexes,
        this.flagStrings);
    this.statusEntries.put(statusEntry.getFingerprint(), statusEntry);
    List<String> unrecognizedStatusEntryLines = statusEntry
        .getAndClearUnrecognizedLines();
    if (unrecognizedStatusEntryLines != null) {
      if (this.unrecognizedLines == null) {
        this.unrecognizedLines = new ArrayList<>();
      }
      this.unrecognizedLines.addAll(unrecognizedStatusEntryLines);
    }
  }

  protected void parseFooter(int offset, int length)
      throws DescriptorParseException {
    Scanner scanner = this.newScanner(offset, length).useDelimiter(NL);
    while (scanner.hasNext()) {
      String line = scanner.next();
      String[] parts = line.split("[ \t]+");
      Key key = Key.get(parts[0]);
      switch (key) {
        case DIRECTORY_FOOTER:
          break;
        case BANDWIDTH_WEIGHTS:
          this.parseBandwidthWeightsLine(line, parts);
          break;
        default:
          if (this.unrecognizedLines == null) {
            this.unrecognizedLines = new ArrayList<>();
          }
          this.unrecognizedLines.add(line);
      }
    }
  }

  private void parseNetworkStatusVersionLine(String line, String[] parts)
      throws DescriptorParseException {
    if (!line.startsWith(Key.NETWORK_STATUS_VERSION.keyword + SP + "3")) {
      throw new DescriptorParseException("Illegal network status version "
          + "number in line '" + line + "'.");
    }
    this.networkStatusVersion = 3;
    if (parts.length == 3) {
      this.consensusFlavor = parts[2];
      if (this.consensusFlavor.equals("microdesc")) {
        this.microdescConsensus = true;
      }
    } else if (parts.length != 2) {
      throw new DescriptorParseException("Illegal network status version "
          + "line '" + line + "'.");
    }
  }

  private void parseVoteStatusLine(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length != 2 || !parts[1].equals("consensus")) {
      throw new DescriptorParseException("Line '" + line + "' indicates "
          + "that this is not a consensus.");
    }
  }

  private void parseConsensusMethodLine(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length != 2) {
      throw new DescriptorParseException("Illegal line '" + line
          + "' in consensus.");
    }
    try {
      this.consensusMethod = Integer.parseInt(parts[1]);
    } catch (NumberFormatException e) {
      throw new DescriptorParseException("Illegal consensus method "
          + "number in line '" + line + "'.");
    }
    if (this.consensusMethod < 1) {
      throw new DescriptorParseException("Illegal consensus method "
          + "number in line '" + line + "'.");
    }
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

  private void parseParamsLine(String line, String[] parts)
      throws DescriptorParseException {
    this.consensusParams = ParseHelper.parseKeyValueIntegerPairs(line,
        parts, 1);
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

  private void parseBandwidthWeightsLine(String line, String[] parts)
      throws DescriptorParseException {
    this.bandwidthWeights = ParseHelper.parseKeyValueIntegerPairs(line,
        parts, 1);
  }

  private int networkStatusVersion;

  @Override
  public int getNetworkStatusVersion() {
    return this.networkStatusVersion;
  }

  private String consensusFlavor;

  @Override
  public String getConsensusFlavor() {
    return this.consensusFlavor;
  }

  private int consensusMethod;

  @Override
  public int getConsensusMethod() {
    return this.consensusMethod;
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

  private SortedMap<String, Integer> consensusParams;

  @Override
  public SortedMap<String, Integer> getConsensusParams() {
    return this.consensusParams == null ? null
        : new TreeMap<>(this.consensusParams);
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

  private SortedMap<String, Integer> bandwidthWeights;

  @Override
  public SortedMap<String, Integer> getBandwidthWeights() {
    return this.bandwidthWeights == null ? null
        : new TreeMap<>(this.bandwidthWeights);
  }
}

