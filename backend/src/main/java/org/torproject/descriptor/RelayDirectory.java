/* Copyright 2012--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor;

import java.util.List;

/**
 * Contains a signed directory in the version 1 directory protocol.
 *
 * <p>Directory authorities in the (long outdated) version 1 of the
 * directory protocol served signed directory documents containing a list
 * of signed server descriptors ({@link ServerDescriptor}) along with
 * short summaries of the status of each server
 * ({@link RouterStatusEntry}).</p>
 *
 * <p>Clients in that version of the directory protocol would fetch this
 * signed directory to get up-to-date information on the state of the
 * network and be certain that the list was attested by a trusted
 * directory authority.</p>
 *
 * <p>Signed directories in the version 1 directory protocol have first
 * been superseded by network status documents in the version 2 directory
 * protocol ({@link RelayNetworkStatus}) and later by network status
 * consensuses ({@link RelayNetworkStatusConsensus}) in the version 3
 * directory protocol.</p> 
 *
 * @since 1.0.0
 */
public interface RelayDirectory extends Descriptor {

  /**
   * Return the time in milliseconds since the epoch when this descriptor
   * was published.
   *
   * @since 1.0.0
   */
  long getPublishedMillis();

  /**
   * Return the RSA-1024 public key in PEM format used by this authority
   * as long-term identity key and to sign network statuses, or null if
   * this key is not included in the descriptor header.
   *
   * @since 1.0.0
   */
  String getDirSigningKey();

  /**
   * Return recommended Tor versions.
   *
   * @since 1.0.0
   */
  List<String> getRecommendedSoftware();

  /**
   * Return the directory signature string made with the authority's
   * identity key.
   *
   * @since 1.0.0
   */
  String getDirectorySignature();

  /**
   * Return router status entries, one for each contained relay.
   *
   * @since 1.0.0
   */
  List<RouterStatusEntry> getRouterStatusEntries();

  /**
   * Return a list of server descriptors contained in the signed
   * directory.
   *
   * @since 1.0.0
   */
  List<ServerDescriptor> getServerDescriptors();

  /**
   * Return a (very likely empty) list of exceptions from parsing the
   * contained server descriptors.
   *
   * @since 1.0.0
   */
  List<Exception> getServerDescriptorParseExceptions();

  /**
   * Return the directory nickname consisting of 1 to 19 alphanumeric
   * characters.
   *
   * @since 1.0.0
   */
  String getNickname();

  /**
   * Return the SHA-1 directory digest, encoded as 40 lower-case
   * hexadecimal characters, that the directory authority used to sign the
   * directory.
   *
   * @since 1.7.0
   */
  String getDigestSha1Hex();
}

