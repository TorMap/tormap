/* Copyright 2012--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor;

import java.io.Serializable;
import java.util.SortedMap;

/**
 * Contains the bandwidth history of a relay or bridge.
 *
 * <p>A bandwidth history is not a descriptor type of its own but usually
 * part of extra-info descriptors ({@link ExtraInfoDescriptor}) or server
 * descriptors ({@link ServerDescriptor}).</p>
 *
 * @since 1.0.0
 */
public interface BandwidthHistory extends Serializable {

  /**
   * Return the original bandwidth history line as contained in the
   * descriptor, possibly prefixed with {@code "opt "}.
   *
   * @since 1.0.0
   */
  String getLine();

  /**
   * Return the time in milliseconds since the epoch when the most recent
   * interval ends.
   *
   * @since 1.0.0
   */
  long getHistoryEndMillis();

  /**
   * Return the interval length in seconds.
   *
   * @since 1.0.0
   */
  long getIntervalLength();

  /**
   * Return the (possibly empty) bandwidth history with map keys being
   * times in milliseconds since the epoch when intervals end and map
   * values being number of bytes used in the interval, ordered from
   * oldest to newest interval.
   *
   * @since 1.0.0
   */
  SortedMap<Long, Long> getBandwidthValues();
}

