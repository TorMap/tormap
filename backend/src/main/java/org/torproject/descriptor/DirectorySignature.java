/* Copyright 2012--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor;

import java.io.Serializable;

/**
 * Contains the signature of a network status consensus or vote.
 *
 * <p>A directory signature is not a descriptor type of its own but is
 * part of a network status consensus
 * ({@link RelayNetworkStatusConsensus}) or vote
 * ({@link RelayNetworkStatusVote}).</p>
 *
 * @since 1.0.0
 */
public interface DirectorySignature extends Serializable {

  /**
   * Return the digest algorithm, which is "sha1" by default and which
   * can be "sha256" or another digest algorithm.
   *
   * @since 1.0.0
   */
  String getAlgorithm();

  /**
   * Return the SHA-1 digest of the authority's long-term identity key in
   * the version 3 directory protocol, encoded as 40 upper-case
   * hexadecimal characters.
   *
   * @since 1.0.0
   */
  String getIdentity();

  /**
   * Return the SHA-1 digest of the authority's medium-term signing key
   * in the version 3 directory protocol, encoded as 40 upper-case
   * hexadecimal characters.
   *
   * @since 1.7.0
   */
  String getSigningKeyDigestSha1Hex();

  /**
   * Return the directory signature string made with the authority's
   * identity key in the version 3 directory protocol.
   *
   * @since 1.0.0
   */
  String getSignature();
}

