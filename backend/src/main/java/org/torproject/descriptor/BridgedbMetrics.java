/* Copyright 2019--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Contains aggregated information about requests to the BridgeDB service.
 *
 * @since 2.8.0
 */
public interface BridgedbMetrics extends Descriptor {

  /**
   * Return the end of the included measurement interval.
   *
   * @return End of the included measurement interval.
   * @since 2.8.0
   */
  LocalDateTime bridgedbMetricsEnd();

  /**
   * Return the length of the included measurement interval.
   *
   * @return Length of the included measurement interval.
   * @since 2.8.0
   */
  Duration bridgedbMetricsIntervalLength();

  /**
   * Return the BridgeDB metrics format version.
   *
   * @return BridgeDB metrics format version.
   * @since 2.8.0
   */
  String bridgedbMetricsVersion();

  /**
   * Return approximate request numbers to the BridgeDB service in the
   * measurement interval broken down by distribution mechanism, obfuscation
   * protocol, and country code.
   *
   * <p>Keys are formatted as {@code DIST.PROTO.CC/EMAIL.[success|fail].none}
   * where:</p>
   * <ul>
   * <li>{@code DIST} is BridgeDB's distribution mechanism, for example,
   * {@code http}, {@code email}, or {@code moat};</li>
   * <li>{@code PROTO} is the obfuscation protocol, for example, {@code obfs2},
   * {@code obfs3}, {@code obfs4}, {@code scramblesuit}, or {@code fte};</li>
   * <li>{@code CC/EMAIL} is either a two-letter country code or an email
   * provider;</li>
   * <li>the second-to-last field is either {@code success} or {@code fail}
   * depending on if the BridgeDB request succeeded or not; and</li>
   * <li>the last field is reserved for an anomaly score to be added in the
   * future.</li>
   * </ul>
   *
   * <p>Values are approximate request numbers, rounded up to the next multiple
   * of 10.</p>
   *
   * @return Map of approximate request numbers.
   * @since 2.8.0
   */
  Optional<Map<String, Long>> bridgedbMetricCounts();
}

