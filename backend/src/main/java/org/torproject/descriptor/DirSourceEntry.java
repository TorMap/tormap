/* Copyright 2011--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor;

import java.io.Serializable;

/**
 * Contains details about an authority and its vote that contributed to a
 * consensus.
 *
 * <p>A directory source entry is not a descriptor type of its own but is
 * part of a network status consensus
 * ({@link RelayNetworkStatusConsensus}).</p>
 *
 * @since 1.0.0
 */
public interface DirSourceEntry extends Serializable {

  /**
   * Return the raw directory source entry bytes.
   *
   * @since 1.0.0
   */
  byte[] getDirSourceEntryBytes();

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
   * Return the authority's primary IPv4 address in dotted-quad format.
   *
   * @since 1.0.0
   */
  String getIp();

  /**
   * Return the TCP port where this authority accepts directory-related
   * HTTP connections.
   *
   * @since 1.0.0
   */
  int getDirPort();

  /**
   * Return the TCP port where this authority accepts TLS connections for
   * the main OR protocol.
   *
   * @since 1.0.0
   */
  int getOrPort();

  /**
   * Return whether this directory source entry was created using a
   * legacy key.
   *
   * @since 1.0.0
   */
  boolean isLegacy();

  /**
   * Return the contact information for this authority, which may contain
   * non-ASCII characters.
   *
   * @since 1.0.0
   */
  String getContactLine();

  /**
   * Return the SHA-1 vote digest, encoded as 40 lower-case hexadecimal
   * characters.
   *
   * @since 1.7.0
   */
  String getVoteDigestSha1Hex();
}

