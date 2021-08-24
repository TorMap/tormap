/* Copyright 2011--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor;

import java.util.SortedMap;

/**
 * Contains a sanitized bridge network status document.
 *
 * <p>The bridge directory authority periodically publishes a network
 * status document with one entry per known bridge in the network
 * ({@link NetworkStatusEntry}) containing: a hash of its identity key, a
 * hash of its most recent server descriptor, and a summary of what the
 * bridge authority believed about its status.</p>
 *
 * <p>The main purpose of this document is to get an authoritative list of
 * running bridges to the bridge distribution service BridgeDB.</p>
 *
 * <p>Details about sanitizing bridge network statuses can be found
 * <a href="https://collector.torproject.org/#type-bridge-network-status">here</a>.
 * </p>
 *
 * @since 1.0.0
 */
public interface BridgeNetworkStatus extends Descriptor {

  /**
   * Return the time in milliseconds since the epoch when this descriptor
   * was published.
   *
   * @since 1.0.0
   */
  long getPublishedMillis();

  /**
   * Return the minimum uptime in seconds that this authority requires
   * for assigning the Stable flag, or -1 if the authority doesn't report
   * this value.
   *
   * @since 1.1.0
   */
  long getStableUptime();

  /**
   * Return the minimum MTBF (mean time between failure) that this
   * authority requires for assigning the Stable flag, or -1 if the
   * authority doesn't report this value.
   *
   * @since 1.1.0
   */
  long getStableMtbf();

  /**
   * Return the minimum bandwidth that this authority requires for
   * assigning the Fast flag, or -1 if the authority doesn't report this
   * value.
   *
   * @since 1.1.0
   */
  long getFastBandwidth();

  /**
   * Return the minimum WFU (weighted fractional uptime) in percent that
   * this authority requires for assigning the Guard flag, or -1 if the
   * authority doesn't report this value.
   *
   * @since 1.1.0
   */
  double getGuardWfu();

  /**
   * Return the minimum weighted time in seconds that this authority
   * needs to know about a relay before assigning the Guard flag, or -1 if
   * the authority doesn't report this information.
   *
   * @since 1.1.0
   */
  long getGuardTk();

  /**
   * Return the minimum bandwidth that this authority requires for
   * assigning the Guard flag if exits can be guards, or -1 if the
   * authority doesn't report this value.
   *
   * @since 1.1.0
   */
  long getGuardBandwidthIncludingExits();

  /**
   * Return the minimum bandwidth that this authority requires for
   * assigning the Guard flag if exits can not be guards, or -1 if the
   * authority doesn't report this value.
   *
   * @since 1.1.0
   */
  long getGuardBandwidthExcludingExits();

  /**
   * Return 1 if the authority has measured enough MTBF info to use the
   * MTBF requirement instead of the uptime requirement for assigning the
   * Stable flag, 0 if not, or -1 if the authority doesn't report this
   * information.
   *
   * @since 1.1.0
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
   * Return a SHA-1 digest of the bridge authority's identity key, encoded as 40
   * upper-case hexadecimal characters.
   *
   * @since 2.11.0
   */
  String getFingerprint();

  /**
   * Return status entries for each contained bridge, with map keys being
   * SHA-1 digests of SHA-1 digest of the bridges' public identity keys,
   * encoded as 40 upper-case hexadecimal characters.
   *
   * @since 1.0.0
   */
  SortedMap<String, NetworkStatusEntry> getStatusEntries();
}

