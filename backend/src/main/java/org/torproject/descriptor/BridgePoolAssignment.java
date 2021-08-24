/* Copyright 2012--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor;

import java.util.SortedMap;

/**
 * Contains a sanitized list of bridges together with the distribution
 * pools they have been assigned to by the bridge distribution service
 * BridgeDB.
 *
 * <p>BridgeDB receives bridge network statuses
 * ({@link BridgeNetworkStatus}) from the bridge authority, assigns these
 * bridges to persistent distribution rings, and hands them out to bridge
 * users.  BridgeDB periodically dumps the list of running bridges with
 * information about the rings, subrings, and file buckets to which they
 * are assigned to a local file.</p>
 *
 * <p>Details about sanitizing bridge pool assignments can be found
 * <a href="https://collector.torproject.org/#type-bridge-pool-assignment">here</a>.
 * </p>
 *
 * @since 1.0.0
 */
public interface BridgePoolAssignment extends Descriptor {

  /**
   * Return the time in milliseconds since the epoch when this descriptor
   * was published.
   *
   * @since 1.0.0
   */
  long getPublishedMillis();

  /**
   * Return the entries contained in this bridge pool assignment list
   * with map keys being SHA-1 digests of SHA-1 digest of the bridges'
   * public identity keys, encoded as 40 upper-case hexadecimal
   * characters, and map values being assignment strings, e.g.
   * {@code "https ring=3 flag=stable"}.
   *
   * @since 1.0.0
   */
  SortedMap<String, String> getEntries();
}

