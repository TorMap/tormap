/* Copyright 2012--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor;

import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Contains a network status document in the version 2 directory protocol.
 *
 * <p>Directory authorities in the (outdated) version 2 of the directory
 * protocol published signed network status documents.  Each network
 * status listed, for every relay in the network
 * ({@link NetworkStatusEntry}): a hash of its identity key, a hash of its
 * most recent server descriptor, and a summary of what the authority
 * believed about its status.</p>
 *
 * <p>Clients would download the authorities' network status documents in
 * turn, and believe statements about routers iff they were attested to by
 * more than half of the authorities.</p>
 *
 * <p>Network status documents in the version 2 directory protocol
 * supersede signed directories in the version 1 directory protocol
 * ({@link RelayDirectory}) and have been superseded by network status
 * consensuses ({@link RelayNetworkStatusConsensus}) in the version 3
 * directory protocol.</p>
 *
 * @since 1.0.0
 */
public interface RelayNetworkStatus extends Descriptor {

  /**
   * Return the document format version of this descriptor which is 2.
   *
   * @since 1.0.0
   */
  int getNetworkStatusVersion();

  /**
   * Return the authority's hostname.
   *
   * @since 1.0.0
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
   * Return a SHA-1 digest of the authority's public identity key,
   * encoded as 40 upper-case hexadecimal characters, which is also used
   * to sign network statuses.
   *
   * @since 1.0.0
   */
  String getFingerprint();

  /**
   * Return the contact information for this authority, which may contain
   * non-ASCII characters.
   *
   * @since 1.0.0
   */
  String getContactLine();

  /**
   * Return the RSA-1024 public key in PEM format used by this authority
   * as long-term identity key and to sign network statuses.
   *
   * @since 1.0.0
   */
  String getDirSigningKey();

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
   * Return the time in milliseconds since the epoch when this descriptor
   * was published.
   *
   * @since 1.0.0
   */
  long getPublishedMillis();

  /**
   * Return the set of flags that this directory assigns to relays, or
   * null if the status does not assign such flags.
   *
   * @since 1.0.0
   */
  SortedSet<String> getDirOptions();

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
   * Return the authority's nickname consisting of 1 to 19 alphanumeric
   * characters.
   *
   * @since 1.0.0
   */
  String getNickname();

  /**
   * Return the directory signature string made with the authority's
   * identity key.
   *
   * @since 1.0.0
   */
  String getDirectorySignature();

  /**
   * Return the SHA-1 status digest, encoded as 40 lower-case hexadecimal
   * characters, that the directory authority used to sign the network
   * status.
   *
   * @since 1.7.0
   */
  String getDigestSha1Hex();
}

