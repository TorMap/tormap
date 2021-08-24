/* Copyright 2012--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor;

/**
 * Contains a key certificate in the version 3 directory protocol.
 *
 * <p>Every directory authority in the version 3 directory protocol uses
 * two keys: a medium-term signing key, and a long-term authority identity
 * key.  (Authorities also have a relay identity key used in their role as
 * a relay and by earlier versions of the directory protocol.)  The
 * identity key is used from time to time to sign new key certificates
 * containing signing keys.  The contained signing key is used to sign key
 * certificates and status documents.</p>
 *
 * @since 1.0.0
 */
public interface DirectoryKeyCertificate extends Descriptor {

  /**
   * Return the version of this descriptor, which must be 3 or higher.
   *
   * @since 1.0.0
   */
  int getDirKeyCertificateVersion();

  /**
   * Return the authority's primary IPv4 address in dotted-quad format,
   * or null if the certificate does not contain an address.
   *
   * @since 1.0.0
   */
  String getAddress();

  /**
   * Return the TCP port where this authority accepts directory-related
   * HTTP connections, or -1 if the certificate does not contain a port.
   *
   * @since 1.0.0
   */
  int getPort();

  /**
   * Return a SHA-1 digest of the authority's long-term authority
   * identity key used for the version 3 directory protocol, encoded as
   * 40 upper-case hexadecimal characters.
   *
   * @since 1.0.0
   */
  String getFingerprint();

  /**
   * Return the authority's identity key in PEM format.
   *
   * @since 1.0.0
   */
  String getDirIdentityKey();

  /**
   * Return the time in milliseconds since the epoch when the authority's
   * signing key and this key certificate were generated.
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
   * @since 1.0.0
   */
  String getDirSigningKey();

  /**
   * Return the signature of the authority's identity key made using the
   * authority's signing key, or null if the certificate does not contain
   * such a signature.
   *
   * @since 1.0.0
   */
  String getDirKeyCrosscert();

  /**
   * Return the certificate signature from the initial item
   * "dir-key-certificate-version" until the final item
   * "dir-key-certification", signed with the authority identity key.
   *
   * @since 1.0.0
   */
  String getDirKeyCertification();

  /**
   * Return the SHA-1 certificate digest, encoded as 40 lower-case
   * hexadecimal characters.
   *
   * @since 1.7.0
   */
  String getDigestSha1Hex();
}

