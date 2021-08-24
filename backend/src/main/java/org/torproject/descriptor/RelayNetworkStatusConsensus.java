/* Copyright 2011--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor;

import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Contains a network status consensus in the version 3 directory protocol.
 *
 * <p>Directory authorities in the version 3 of the directory protocol
 * periodically generate a view of the current descriptors and status for
 * known relays and send a signed summary of this view to the other
 * authorities ({@link RelayNetworkStatusVote}).  The authorities compute
 * the result of this vote and sign a network status consensus containing
 * the result of the vote, which is this document.</p>
 *
 * <p>Clients use consensus documents to find out when their list of
 * relays is out-of-date by looking at the contained network status
 * entries ({@link NetworkStatusEntry}).  If it is, they download any
 * missing server descriptors ({@link ServerDescriptor}).</p>
 *
 * @since 1.0.0
 */
public interface RelayNetworkStatusConsensus extends Descriptor {

  /**
   * Return the document format version of this descriptor which is 3 or
   * higher.
   *
   * @since 1.0.0
   */
  int getNetworkStatusVersion();

  /**
   * Return the consensus flavor name, which denotes the variant of the
   * original, unflavored consensus, encoded as a string of alphanumeric
   * characters and dashes, or null if this descriptor is the unflavored
   * consensus.
   *
   * @since 1.0.0
   */
  String getConsensusFlavor();

  /**
   * Return the consensus method number of this descriptor, which is the
   * highest consensus method supported by more than 2/3 of voting
   * authorities, or 0 if no consensus method is contained in the
   * descriptor.
   *
   * @since 1.0.0
   */
  int getConsensusMethod();

  /**
   * Return the time in milliseconds since the epoch at which this
   * descriptor became valid.
   *
   * @since 1.0.0
   */
  long getValidAfterMillis();

  /**
   * Return the time in milliseconds since the epoch until which this
   * descriptor is the freshest that is available.
   *
   * @since 1.0.0
   */
  long getFreshUntilMillis();

  /**
   * Return the time in milliseconds since the epoch until which this
   * descriptor was valid.
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
   * consensus does not contain an opinion about server versions.
   *
   * @since 1.0.0
   */
  List<String> getRecommendedServerVersions();

  /**
   * Return recommended Tor versions for client usage, or null if the
   * consensus does not contain an opinion about client versions.
   *
   * @since 1.0.0
   */
  List<String> getRecommendedClientVersions();

  /**
   * Return the version numbers of all protocols that clients should support,
   * or null if the consensus does not contain an opinion about protocol
   * versions.
   *
   * @since 1.6.0
   */
  SortedMap<String, SortedSet<Long>> getRecommendedClientProtocols();

  /**
   * Return the version numbers of all protocols that relays should support,
   * or null if the consensus does not contain an opinion about protocol
   * versions.
   *
   * @since 1.6.0
   */
  SortedMap<String, SortedSet<Long>> getRecommendedRelayProtocols();

  /**
   * Return the version numbers of all protocols that clients must support,
   * or null if the consensus does not contain an opinion about protocol
   * versions.
   *
   * @since 1.6.0
   */
  SortedMap<String, SortedSet<Long>> getRequiredClientProtocols();

  /**
   * Return the version numbers of all protocols that relays must support,
   * or null if the consensus does not contain an opinion about protocol
   * versions.
   *
   * @since 1.6.0
   */
  SortedMap<String, SortedSet<Long>> getRequiredRelayProtocols();

  /**
   * Return a list of software packages and their versions together with a
   * URL and one or more digests in the format {@code PackageName Version
   * URL DIGESTS} that are known by at least three directory
   * authorities and agreed upon by the majority of directory authorities,
   * or null if the consensus does not contain package information.
   *
   * @since 1.3.0
   */
  List<String> getPackageLines();

  /**
   * Return known relay flags in this descriptor that were contained in
   * enough votes for this consensus to be an authoritative opinion for
   * these relay flags.
   *
   * @since 1.0.0
   */
  SortedSet<String> getKnownFlags();

  /**
   * Return consensus parameters contained in this descriptor with map
   * keys being case-sensitive parameter identifiers and map values being
   * parameter values, or null if the consensus doesn't contain consensus
   * parameters.
   *
   * @since 1.0.0
   */
  SortedMap<String, Integer> getConsensusParams();

  /**
   * Return the number of commits used to generate the second-to-last shared
   * random value, or -1 if the consensus does not contain a second-to-last
   * shared random value.
   *
   * @since 1.6.0
   */
  int getSharedRandPreviousNumReveals();

  /**
   * Return the second-to-last shared random value, encoded in base64, or null
   * if the consensus does not contain a second-to-last shared random value.
   *
   * @since 1.6.0
   */
  String getSharedRandPreviousValue();

  /**
   * Return the number of commits used to generate the latest shared random
   * value, or -1 if the consensus does not contain the latest shared random
   * value.
   *
   * @since 1.6.0
   */
  int getSharedRandCurrentNumReveals();

  /**
   * Return the latest shared random value, encoded in base64, or null if the
   * consensus does not contain the latest shared random value.
   *
   * @since 1.6.0
   */
  String getSharedRandCurrentValue();

  /**
   * Return directory source entries for each directory authority that
   * contributed to the consensus, with map keys being SHA-1 digests of
   * the authorities' identity keys in the version 3 directory protocol,
   * encoded as 40 upper-case hexadecimal characters.
   *
   * @since 1.0.0
   */
  SortedMap<String, DirSourceEntry> getDirSourceEntries();

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
   * Return the list of signatures contained in this consensus.
   *
   * @since 1.3.0
   */
  List<DirectorySignature> getSignatures();

  /**
   * Return optional weights to be applied to router bandwidths during
   * path selection with map keys being case-sensitive weight identifiers
   * and map values being weight values, or null if the consensus doesn't
   * contain such weights.
   *
   * @since 1.0.0
   */
  SortedMap<String, Integer> getBandwidthWeights();

  /**
   * Return the SHA-1 digest of this consensus, encoded as 40 lower-case
   * hexadecimal characters that directory authorities use to sign the
   * consensus.
   *
   * @since 1.7.0
   */
  String getDigestSha1Hex();
}

