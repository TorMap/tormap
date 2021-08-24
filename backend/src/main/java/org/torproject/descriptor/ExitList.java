/* Copyright 2012--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * Contains an exit list containing the IP addresses of relays that the
 * exit list service TorDNSEL found when exiting through them.
 *
 * @since 1.0.0
 */
public interface ExitList extends Descriptor {

  /**
   * End-of-line character expected in exit lists.
   *
   * @since 1.0.0
   */
  String EOL = "\n";

  /**
   * Exit list entry containing results from a single exit scan.
   *
   * @since 1.1.0
   */
  interface Entry extends Serializable {

    /**
     * Return the scanned relay's fingerprint, which is a SHA-1 digest of
     * the relays's public identity key, encoded as 40 upper-case
     * hexadecimal characters.
     *
     * @since 1.1.0
     */
    String getFingerprint();

    /**
     * Return the time in milliseconds since the epoch when the scanned
     * relay's last known descriptor was published.
     *
     * @since 1.1.0
     */
    long getPublishedMillis();

    /**
     * Return the time in milliseconds since the epoch when the network
     * status that this scan was based on was published.
     *
     * @since 1.1.0
     */
    long getLastStatusMillis();

    /**
     * Return the IP addresses that were determined in the scan with map
     * keys being IPv4 addresses in dotted-quad format and map values
     * being scan times in milliseconds since the epoch.
     *
     * @since 1.1.0
     */
    Map<String, Long> getExitAddresses();
  }

  /**
   * Return the time in milliseconds since the epoch when this descriptor
   * was downloaded.
   *
   * @since 1.0.0
   */
  long getDownloadedMillis();

  /**
   * Return the unordered set of exit scan results.
   *
   * @since 1.1.0
   */
  Set<Entry> getEntries();
}

