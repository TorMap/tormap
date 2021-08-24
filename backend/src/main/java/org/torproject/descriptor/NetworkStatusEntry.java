/* Copyright 2011--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor;

import java.io.Serializable;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * Contains an entry in a network status in the version 2 or 3 directory
 * protocol or in a bridge network status.
 *
 * <p>A network status entry is not a descriptor type of its own but is
 * part of a network status in the version 2 directory protocol
 * ({@link RelayNetworkStatus}), a vote ({@link RelayNetworkStatusVote})
 * or flavored/unflavored consensus (@link RelayNetworkStatusConsensus})
 * in the version 3 directory protocol, or a bridge network status
 * ({@link BridgeNetworkStatus}).  Entries in signed directories in the
 * version 1 directory protocol are represented by router status entries
 * ({@link RouterStatusEntry}).</p>
 *
 * @since 1.0.0
 */
public interface NetworkStatusEntry extends Serializable {

  /**
   * Return the raw network status entry bytes.
   *
   * @since 1.0.0
   */
  byte[] getStatusEntryBytes();

  /**
   * Return the server nickname consisting of 1 to 19 alphanumeric
   * characters.
   *
   * @since 1.0.0
   */
  String getNickname();

  /**
   * Return a SHA-1 digest of the server's identity key, encoded as 40
   * upper-case hexadecimal characters.
   *
   * @since 1.0.0
   */
  String getFingerprint();

  /**
   * Return the SHA-1 digest of the server descriptor, or null if the
   * containing network status does not contain server descriptor
   * references, like a microdesc consensus.
   *
   * @since 1.0.0
   */
  String getDescriptor();

  /**
   * Return the time in milliseconds since the epoch when this descriptor
   * was published.
   *
   * @since 1.0.0
   */
  long getPublishedMillis();

  /**
   * Return the server's primary IPv4 address in dotted-quad format.
   *
   * @since 1.0.0
   */
  String getAddress();

  /**
   * Return the TCP port where this server accepts TLS connections for
   * the main OR protocol.
   *
   * @since 1.0.0
   */
  int getOrPort();

  /**
   * Return the TCP port where this server accepts directory-related HTTP
   * connections.
   *
   * @since 1.0.0
   */
  int getDirPort();

  /**
   * Return the (possibly empty) set of microdescriptor digests, encoded as 43
   * base64 characters without padding characters, if the containing network
   * status is a vote or microdesc consensus, or null otherwise.
   *
   * @since 1.7.0
   */
  Set<String> getMicrodescriptorDigestsSha256Base64();

  /**
   * Return additional IP addresses and TCP ports where this server
   * accepts TLS connections for the main OR protocol, or an empty list if
   * the network status doesn't contain any such additional addresses and
   * ports.
   *
   * @since 1.0.0
   */
  List<String> getOrAddresses();

  /**
   * Return the relay flags assigned to this server, or null if the
   * status entry didn't contain any relay flags.
   *
   * @since 1.0.0
   */
  SortedSet<String> getFlags();

  /**
   * Return the Tor software version, or null if the status entry didn't
   * contain version information.
   *
   * @since 1.0.0
   */
  String getVersion();

  /**
   * Return the version numbers of all protocols supported by this server, or
   * null if the status entry does not specify supported protocol versions.
   *
   * @since 1.6.0
   */
  SortedMap<String, SortedSet<Long>> getProtocols();

  /**
   * Return the bandwidth weight of this server or -1 if the status entry
   * didn't contain a bandwidth line.
   *
   * @since 1.0.0
   */
  long getBandwidth();

  /**
   * Return the measured bandwidth or -1 if the status entry either
   * didn't contain bandwidth information or didn't contain an indication
   * that this information is based on measured bandwidth.
   *
   * @since 1.0.0
   */
  long getMeasured();

  /**
   * Return whether the status entry is yet unmeasured by the bandwidth
   * authorities; only included in consensuses using method 17 or higher.
   *
   * @since 1.0.0
   */
  boolean getUnmeasured();

  /**
   * Return the default policy of the port summary, which can be either
   * {@code "accept"} or {@code "reject"}, or null if the status entry
   * didn't contain an exit policy summary.
   *
   * @since 1.0.0
   */
  String getDefaultPolicy();

  /**
   * Return the list of ports or port intervals of the exit port summary,
   * or null if the status entry didn't contain an exit policy summary.
   *
   * @since 1.0.0
   */
  String getPortList();

  /**
   * Return the server's Ed25519 master key, encoded as 43 base64
   * characters without padding characters, "none" if the relay doesn't
   * have an Ed25519 identity, or null if the status entry didn't contain
   * this information or if the status is not a vote.
   *
   * @since 1.1.0
   */
  String getMasterKeyEd25519();
}

