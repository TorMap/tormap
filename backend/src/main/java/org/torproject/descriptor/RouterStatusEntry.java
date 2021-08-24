/* Copyright 2012--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor;

/**
 * Contains a router status entry contained in a signed directory in the
 * version 1 directory protocol.
 *
 * <p>Directory authorities in the (long outdated) version 1 of the
 * directory protocol included router status entries with short summaries
 * of the status of each server in the signed directories they produced
 * ({@link RelayDirectory}).  These entries contained references to server
 * descriptors published by relays together with the authorities' opinion
 * on whether relays were verified and live.</p>
 *
 * @since 1.0.0
 */
public interface RouterStatusEntry {

  /**
   * Return the relay nickname consisting of 1 to 19 alphanumeric
   * characters, or null if the relay is unverified.
   *
   * @since 1.0.0
   */
  String getNickname();

  /**
   * Return a SHA-1 digest of the relay's identity key, encoded as 40
   * upper-case hexadecimal characters.
   *
   * @since 1.0.0
   */
  String getFingerprint();

  /**
   * Return whether the relay is verified.
   *
   * @since 1.0.0
   */
  boolean isVerified();

  /**
   * Return whether the relay is live.
   *
   * @since 1.0.0
   */
  boolean isLive();
}

