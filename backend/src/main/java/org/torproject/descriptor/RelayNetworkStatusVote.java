/* Copyright 2011--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor;

import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Contains a network status vote in the version 3 directory protocol.
 *
 * <p>Directory authorities in the version 3 of the directory protocol
 * periodically generate a view of the current descriptors and status for
 * known relays and send a signed summary of this view to the other
 * authorities, which is this document.  The authorities compute the
 * result of this vote and sign a network status consensus containing the
 * result of the vote ({@link RelayNetworkStatusConsensus}).</p>
 *
 * @since 1.0.0
 */
public interface RelayNetworkStatusVote extends Descriptor {

  /**
   * Return the document format version of this descriptor which is 3 or
   * higher.
   *
   * @since 1.0.0
   */
  int getNetworkStatusVersion();

  /**
   * Return the list of consensus method numbers supported by this
   * authority, or null if the descriptor doesn't say so, which would mean
   * that only method 1 is supported.
   *
   * @since 1.0.0
   */
  List<Integer> getConsensusMethods();

  /**
   * Return the time in milliseconds since the epoch when this descriptor
   * was published.
   *
   * @since 1.0.0
   */
  long getPublishedMillis();

  /**
   * Return the time in milliseconds since the epoch at which the
   * consensus is supposed to become valid.
   *
   * @since 1.0.0
   */
  long getValidAfterMillis();

  /**
   * Return the time in milliseconds since the epoch until which the
   * consensus is supposed to be the freshest that is available.
   *
   * @since 1.0.0
   */
  long getFreshUntilMillis();

  /**
   * Return the time in milliseconds since the epoch until which the
   * consensus is supposed to be valid.
   *
   * @since 1.0.0
   */
  long getValidUntilMillis();

  /**
   * Return the number of seconds that the directory authorities will
   * allow to collect votes from the other authorities when producing the
   * next consensus.
   *
   * @since 1.0.0
   */
  long getVoteSeconds();

  /**
   * Return the number of seconds that the directory authorities will
   * allow to collect signatures from the other authorities when producing
   * the next consensus.
   *
   * @since 1.0.0
   */
  long getDistSeconds();

  /**
   * Return recommended Tor versions for server usage, or null if the
   * authority does not recommend server versions.
   *
   * @since 1.0.0
   */
  List<String> getRecommendedServerVersions();

  /**
   * Return recommended Tor versions for client usage, or null if the
   * authority does not recommend client versions.
   *
   * @since 1.0.0
   */
  List<String> getRecommendedClientVersions();

  /**
   * Return the version numbers of all protocols that clients should support,
   * or null if the vote does not contain an opinion about protocol versions.
   *
   * @since 1.6.0
   */
  SortedMap<String, SortedSet<Long>> getRecommendedClientProtocols();

  /**
   * Return the version numbers of all protocols that relays should support,
   * or null if the vote does not contain an opinion about protocol versions.
   *
   * @since 1.6.0
   */
  SortedMap<String, SortedSet<Long>> getRecommendedRelayProtocols();

  /**
   * Return the version numbers of all protocols that clients must support,
   * or null if the vote does not contain an opinion about protocol versions.
   *
   * @since 1.6.0
   */
  SortedMap<String, SortedSet<Long>> getRequiredClientProtocols();

  /**
   * Return the version numbers of all protocols that relays must support,
   * or null if the vote does not contain an opinion about protocol versions.
   *
   * @since 1.6.0
   */
  SortedMap<String, SortedSet<Long>> getRequiredRelayProtocols();

  /**
   * Return a list of software packages and their versions together with a
   * URL and one or more digests in the format {@code PackageName Version
   * URL DIGESTS} that are known by this directory authority, or
   * null if this descriptor does not contain package information.
   *
   * @since 1.3.0
   */
  List<String> getPackageLines();

  /**
   * Return known relay flags by this authority.
   *
   * @since 1.0.0
   */
  SortedSet<String> getKnownFlags();

  /**
   * Return the minimum uptime in seconds that this authority requires
   * for assigning the Stable flag, or -1 if the authority doesn't report
   * this value.
   *
   * @since 1.0.0
   */
  long getStableUptime();

  /**
   * Return the minimum MTBF (mean time between failure) that this
   * authority requires for assigning the Stable flag, or -1 if the
   * authority doesn't report this value.
   *
   * @since 1.0.0
   */
  long getStableMtbf();

  /**
   * Return the minimum bandwidth that this authority requires for
   * assigning the Fast flag, or -1 if the authority doesn't report this
   * value.
   *
   * @since 1.0.0
   */
  long getFastBandwidth();

  /**
   * Return the minimum WFU (weighted fractional uptime) in percent that
   * this authority requires for assigning the Guard flag, or -1 if the
   * authority doesn't report this value.
   *
   * @since 1.0.0
   */
  double getGuardWfu();

  /**
   * Return the minimum weighted time in seconds that this authority
   * needs to know about a relay before assigning the Guard flag, or -1 if
   * the authority doesn't report this information.
   *
   * @since 1.0.0
   */
  long getGuardTk();

  /**
   * Return the minimum bandwidth that this authority requires for
   * assigning the Guard flag if exits can be guards, or -1 if the
   * authority doesn't report this value.
   *
   * @since 1.0.0
   */
  long getGuardBandwidthIncludingExits();

  /**
   * Return the minimum bandwidth that this authority requires for
   * assigning the Guard flag if exits can not be guards, or -1 if the
   * authority doesn't report this value.
   *
   * @since 1.0.0
   */
  long getGuardBandwidthExcludingExits();

  /**
   * Return 1 if the authority has measured enough MTBF info to use the
   * MTBF requirement instead of the uptime requirement for assigning the
   * Stable flag, 0 if not, or -1 if the authority doesn't report this
   * information.
   *
   * @since 1.0.0
   */
  int getEnoughMtbfInfo();

  /**
   * Return 1 if the authority has enough measured bandwidths that it'll
   * ignore the advertised bandwidth claims of routers without measured
   * bandwidth, 0 if not, or -1 if the authority doesn't report this
   * information.
   *
   * @since 1.1.0
   */
  int getIgnoringAdvertisedBws();

  /**
   * Return consensus parameters contained in this descriptor with map
   * keys being case-sensitive parameter identifiers and map values being
   * parameter values, or null if the authority doesn't include consensus
   * parameters in its vote.
   *
   * @since 1.0.0
   */
  SortedMap<String, Integer> getConsensusParams();

  /**
   * Return the authority's nickname consisting of 1 to 19 alphanumeric
   * characters.
   *
   * @since 1.0.0
   */
  String getNickname();

  /**
   * Return a SHA-1 digest of the authority's long-term authority
   * identity key used for the version 3 directory protocol, encoded as
   * 40 upper-case hexadecimal characters.
   *
   * @since 1.0.0
   */
  String getIdentity();

  /**
   * Return the authority's hostname.
   *
   * @since 1.2.0
   */
  String getHostname();

  /**
   * Return the authority's primary IPv4 address in dotted-quad format,
   * or null if the descriptor does not contain an address.
   *
   * @since 1.0.0
   */
  String getAddress();

  /**
   * Return the TCP port where this authority accepts directory-related
   * HTTP connections, or 0 if the authority does not accept such
   * connections.
   *
   * @since 1.0.0
   */
  int getDirport();

  /**
   * Return the TCP port where this authority accepts TLS connections for
   * the main OR protocol, or 0 if the authority does not accept such
   * connections.
   *
   * @since 1.0.0
   */
  int getOrport();

  /**
   * Return the contact information for this authority, which may contain
   * non-ASCII characters, or null if no contact information is included
   * in the descriptor.
   *
   * @since 1.0.0
   */
  String getContactLine();

  /**
   * Return whether this directory authority supports and can participate in
   * the shared random protocol.
   *
   * @since 1.6.0
   */
  boolean isSharedRandParticipate();

  /**
   * Return all currently known directory authority commit lines for the shared
   * randomness protocol in the original format as they are contained in this
   * vote, or null if this vote does not contain any such line.
   *
   * <pre>
   * "shared-rand-commit" SP Version SP AlgName SP Identity SP Commit
   *     [SP Reveal] NL
   * </pre>
   *
   * @since 1.6.0
   */
  List<String> getSharedRandCommitLines();

  /**
   * Return the number of commits used to generate the second-to-last shared
   * random value, or -1 if this vote does not contain a second-to-last shared
   * random value.
   *
   * @since 1.6.0
   */
  int getSharedRandPreviousNumReveals();

  /**
   * Return the second-to-last shared random value, encoded in base64, or null
   * if this vote does not contain a second-to-last shared random value.
   *
   * @since 1.6.0
   */
  String getSharedRandPreviousValue();

  /**
   * Return the number of commits used to generate the latest shared random
   * value, or -1 if this vote does not contain the latest shared random value.
   *
   * @since 1.6.0
   */
  int getSharedRandCurrentNumReveals();

  /**
   * Return the latest shared random value, encoded in base64, or null if this
   * vote does not contain the latest shared random value.
   *
   * @since 1.6.0
   */
  String getSharedRandCurrentValue();

  /**
   * Return the headers from the bandwidth file used to generate this vote, or
   * null if the authority producing this vote is not configured with a
   * bandwidth file or does not include the headers of the configured bandwidth
   * file in its vote.
   *
   * @since 2.11.0
   */
  SortedMap<String, String> getBandwidthFileHeaders();

  /**
   * Return the SHA256 digest of the bandwidth file, encoded as 43 base64
   * characters without padding characters, or null if the authority producing
   * this vote is not configured with a bandwidth file or does not include the
   * SHA256 digest of the configured bandwidth file in its vote.
   *
   * @since 2.11.0
   */
  String getBandwidthFileDigestSha256Base64();

  /**
   * Return the version of the directory key certificate used by this
   * authority, which must be 3 or higher.
   *
   * @since 1.0.0
   */
  int getDirKeyCertificateVersion();

  /**
   * Return the SHA-1 digest for an obsolete authority identity key still
   * used by this authority to keep older clients working, or null if this
   * authority does not use such a key.
   *
   * @since 1.0.0
   */
  String getLegacyDirKey();

  /**
   * Return the authority's identity key in PEM format.
   *
   * @since 1.2.0
   */
  String getDirIdentityKey();

  /**
   * Return the time in milliseconds since the epoch when the authority's
   * signing key and corresponding key certificate were generated.
   *
   * @since 1.0.0
   */
  long getDirKeyPublishedMillis();

  /**
   * Return the time in milliseconds since the epoch after which the
   * authority's signing key is no longer valid.
   *
   * @since 1.0.0
   */
  long getDirKeyExpiresMillis();

  /**
   * Return the authority's signing key in PEM format.
   *
   * @since 1.2.0
   */
  String getDirSigningKey();

  /**
   * Return the signature of the authority's identity key made using the
   * authority's signing key, or null if the vote does not contain such a
   * signature.
   *
   * @since 1.2.0
   */
  String getDirKeyCrosscert();

  /**
   * Return the certificate signature from the initial item
   * "dir-key-certificate-version" until the final item
   * "dir-key-certification", signed with the authority identity key.
   *
   * @since 1.2.0
   */
  String getDirKeyCertification();

  /**
   * Return status entries for each contained server, with map keys being
   * SHA-1 digests of the servers' public identity keys, encoded as 40
   * upper-case hexadecimal characters.
   *
   * @since 1.0.0
   */
  SortedMap<String, NetworkStatusEntry> getStatusEntries();

  /**
   * Return whether a status entry with the given relay fingerprint
   * (SHA-1 digest of the server's public identity key, encoded as 40
   * upper-case hexadecimal characters) exists; convenience method for
   * {@code getStatusEntries().containsKey(fingerprint)}.
   *
   * @since 1.0.0
   */
  boolean containsStatusEntry(String fingerprint);

  /**
   * Return a status entry by relay fingerprint (SHA-1 digest of the
   * server's public identity key, encoded as 40 upper-case hexadecimal
   * characters), or null if no such status entry exists; convenience
   * method for {@code getStatusEntries().get(fingerprint)}.
   *
   * @since 1.0.0
   */
  NetworkStatusEntry getStatusEntry(String fingerprint);

  /**
   * Return a list of signatures contained in this vote, which is
   * typically a single signature made by the authority but which may also
   * be more than one signature made with different keys or algorithms.
   *
   * @since 1.3.0
   */
  List<DirectorySignature> getSignatures();

  /**
   * Return the SHA-1 digest of this vote, encoded as 40 lower-case hexadecimal
   * characters that is used to reference this vote from a consensus.
   *
   * @since 1.7.0
   */
  String getDigestSha1Hex();
}

