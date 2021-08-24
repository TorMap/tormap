/* Copyright 2019--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A bandwidth file contains information on relays' bandwidth capacities and is
 * produced by bandwidth generators, previously known as bandwidth scanners.
 *
 * @since 2.6.0
 */
public interface BandwidthFile extends Descriptor {

  /**
   * Return the SHA-256 bandwidth file digest, encoded as 43 base64 characters
   * without padding characters, that is used to reference this bandwidth file
   * from a vote.
   *
   * @since 2.11.0
   */
  String digestSha256Base64();

  /**
   * Time of the most recent generator bandwidth result.
   *
   * @since 2.6.0
   */
  LocalDateTime timestamp();

  /**
   * Document format version.
   *
   * @since 2.6.0
   */
  String version();

  /**
   * Name of the software that created the document.
   *
   * @since 2.6.0
   */
  String software();

  /**
   * Version of the software that created the document.
   *
   * @since 2.6.0
   */
  Optional<String> softwareVersion();

  /**
   * Timestamp in UTC time zone when the file was created.
   *
   * @since 2.6.0
   */
  Optional<LocalDateTime> fileCreated();

  /**
   * Timestamp in UTC time zone when the generator was started.
   *
   * @since 2.6.0
   */
  Optional<LocalDateTime> generatorStarted();

  /**
   * Timestamp in UTC time zone when the first relay bandwidth was obtained.
   *
   * @since 2.6.0
   */
  Optional<LocalDateTime> earliestBandwidth();

  /**
   * Timestamp in UTC time zone of the most recent generator bandwidth result.
   *
   * @since 2.6.0
   */
  Optional<LocalDateTime> latestBandwidth();

  /**
   * Number of relays that have enough measurements to be included in the
   * bandwidth file.
   *
   * @since 2.6.0
   */
  Optional<Integer> numberEligibleRelays();

  /**
   * Percentage of relays in the consensus that should be included in every
   * generated bandwidth file.
   *
   * @since 2.6.0
   */
  Optional<Integer> minimumPercentEligibleRelays();

  /**
   * Number of relays in the consensus.
   *
   * @since 2.6.0
   */
  Optional<Integer> numberConsensusRelays();

  /**
   * The number of eligible relays, as a percentage of the number of relays in
   * the consensus.
   *
   * @since 2.6.0
   */
  Optional<Integer> percentEligibleRelays();

  /**
   * Minimum number of relays that should be included in the bandwidth file.
   *
   * @since 2.6.0
   */
  Optional<Integer> minimumNumberEligibleRelays();

  /**
   * Country, as in political geolocation, where the generator is run.
   *
   * @since 2.6.0
   */
  Optional<String> scannerCountry();

  /**
   * Country, as in political geolocation, or countries where the destination
   * web server(s) are located.
   *
   * @since 2.6.0
   */
  Optional<String[]> destinationsCountries();

  /**
   * Number of the different consensuses seen in the last data period.
   *
   * @since 2.6.0
   */
  Optional<Integer> recentConsensusCount();

  /**
   * Number of times that a list with a subset of relays prioritized to be
   * measured has been created in the last data period.
   *
   * @since 2.6.0
   */
  Optional<Integer> recentPriorityListCount();

  /**
   * Number of relays that has been in in the list of relays prioritized to be
   * measured in the last data period.
   *
   * @since 2.6.0
   */
  Optional<Integer> recentPriorityRelayCount();

  /**
   * Number of times that any relay has been queued to be measured in the last
   * data period.
   *
   * @since 2.6.0
   */
  Optional<Integer> recentMeasurementAttemptCount();

  /**
   * Number of times that the scanner attempted to measure a relay in the last
   * data period, but the relay has not been measured because of system, network
   * or implementation issues.
   *
   * @since 2.6.0
   */
  Optional<Integer> recentMeasurementFailureCount();

  /**
   * Number of relays that have no successful measurements in the last data
   * period.
   *
   * @since 2.6.0
   */
  Optional<Integer> recentMeasurementsExcludedErrorCount();

  /**
   * Number of relays that have some successful measurements in the last data
   * period, but all those measurements were performed in a period of time that
   * was too short.
   *
   * @since 2.6.0
   */
  Optional<Integer> recentMeasurementsExcludedNearCount();

  /**
   * Number of relays that have some successful measurements, but all those
   * measurements are too old.
   *
   * @since 2.6.0
   */
  Optional<Integer> recentMeasurementsExcludedOldCount();

  /**
   * Number of relays that don't have enough recent successful measurements.
   *
   * @since 2.6.0
   */
  Optional<Integer> recentMeasurementsExcludedFewCount();

  /**
   * Time that it would take to report measurements about half of the network,
   * given the number of eligible relays and the time it took in the last days.
   *
   * @since 2.6.0
   */
  Optional<Duration> timeToReportHalfNetwork();

  /**
   * List of zero or more {@link RelayLine}s containing relay identities and
   * bandwidths in the order as they are contained in the bandwidth file.
   *
   * @since 2.6.0
   */
  List<RelayLine> relayLines();

  interface RelayLine extends Serializable {

    /**
     * Fingerprint for the relay's RSA identity key.
     *
     * @since 2.6.0
     */
    Optional<String> nodeId();

    /**
     * Relays's master Ed25519 key, base64 encoded, without trailing "="s.
     *
     * @since 2.6.0
     */
    Optional<String> masterKeyEd25519();

    /**
     * Bandwidth of this relay in kilobytes per second.
     *
     * @since 2.6.0
     */
    int bw();

    /**
     * Additional relay key-value pairs, excluding the key value pairs already
     * parsed for relay identities and bandwidths.
     *
     * @since 2.6.0
     */
    Map<String, String> additionalKeyValues();
  }
}

