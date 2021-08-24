/* Copyright 2019--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.SortedMap;

/**
 * Contain aggregated information about snowflake proxies and snowflake clients.
 *
 * @since 2.7.0
 */
public interface SnowflakeStats extends Descriptor {

  /**
   * Return the end of the included measurement interval.
   *
   * @return End of the included measurement interval.
   * @since 2.7.0
   */
  LocalDateTime snowflakeStatsEnd();

  /**
   * Return the length of the included measurement interval.
   *
   * @return Length of the included measurement interval.
   * @since 2.7.0
   */
  Duration snowflakeStatsIntervalLength();

  /**
   * Return a list of mappings from two-letter country codes to the number of
   * unique IP addresses of snowflake proxies that have polled.
   *
   * @return List of mappings from two-letter country codes to the number of
   *     unique IP addresses of snowflake proxies that have polled.
   * @since 2.7.0
   */
  Optional<SortedMap<String, Long>> snowflakeIps();

  /**
   * Return a count of the total number of unique IP addresses of snowflake
   * proxies that have polled.
   *
   * @return Count of the total number of unique IP addresses of snowflake
   *     proxies that have polled.
   * @since 2.7.0
   */
  Optional<Long> snowflakeIpsTotal();

  /**
   * Return a count of the total number of unique IP addresses of snowflake
   * proxies of type "standalone" that have polled.
   *
   * @return Count of the total number of unique IP addresses of snowflake
   *     proxies of type "standalone" that have polled.
   * @since 2.10.0
   */
  Optional<Long> snowflakeIpsStandalone();

  /**
   * Return a count of the total number of unique IP addresses of snowflake
   * proxies of type "badge" that have polled.
   *
   * @return Count of the total number of unique IP addresses of snowflake
   *     proxies of type "badge" that have polled.
   * @since 2.10.0
   */
  Optional<Long> snowflakeIpsBadge();

  /**
   * Return a count of the total number of unique IP addresses of snowflake
   * proxies of type "webext" that have polled.
   *
   * @return Count of the total number of unique IP addresses of snowflake
   *     proxies of type "webext" that have polled.
   * @since 2.10.0
   */
  Optional<Long> snowflakeIpsWebext();

  /**
   * Return a count of the number of times a proxy has polled but received no
   * client offer, rounded up to the nearest multiple of 8.
   *
   * @return Count of the number of times a proxy has polled but received no
   *     client offer, rounded up to the nearest multiple of 8.
   * @since 2.7.0
   */
  Optional<Long> snowflakeIdleCount();

  /**
   * Return a count of the number of times a client has requested a proxy from
   * the broker but no proxies were available, rounded up to the nearest
   * multiple of 8.
   *
   * @return Count of the number of times a client has requested a proxy from
   *     the broker but no proxies were available, rounded up to the nearest
   *     multiple of 8.
   * @since 2.7.0
   */
  Optional<Long> clientDeniedCount();

  /**
   * Return a count of the number of times a client with a restricted or unknown
   * NAT type has requested a proxy from the broker but no proxies were
   * available, rounded up to the nearest multiple of 8.
   *
   * @return Count of the number of times a client with a restricted or unknown
   *     NAT type has requested a proxy from the broker but no proxies were
   *     available, rounded up to the nearest multiple of 8.
   * @since 2.16.0
   */
  Optional<Long> clientRestrictedDeniedCount();

  /**
   * Return a count of the number of times a client with an unrestricted NAT
   * type has requested a proxy from the broker but no proxies were available,
   * rounded up to the nearest multiple of 8.
   *
   * @return Count of the number of times a client with an unrestricted NAT type
   *     has requested a proxy from the broker but no proxies were available,
   *     rounded up to the nearest multiple of 8.
   * @since 2.16.0
   */
  Optional<Long> clientUnrestrictedDeniedCount();

  /**
   * Return a count of the number of times a client successfully received a
   * proxy from the broker, rounded up to the nearest multiple of 8.
   *
   * @return Count of the number of times a client successfully received a proxy
   *     from the broker, rounded up to the nearest multiple of 8.
   * @since 2.7.0
   */
  Optional<Long> clientSnowflakeMatchCount();

  /**
   * Return a count of the total number of unique IP addresses of snowflake
   * proxies that have a restricted NAT type.
   *
   * @return Count of the total number of unique IP addresses of snowflake
   *     proxies that have a restricted NAT type.
   * @since 2.16.0
   */
  Optional<Long> snowflakeIpsNatRestricted();

  /**
   * Return a count of the total number of unique IP addresses of snowflake
   * proxies that have an unrestricted NAT type.
   *
   * @return Count of the total number of unique IP addresses of snowflake
   *     proxies that have an unrestricted NAT type.
   * @since 2.16.0
   */
  Optional<Long> snowflakeIpsNatUnrestricted();

  /**
   * Return a count of the total number of unique IP addresses of snowflake
   * proxies that have an unknown NAT type.
   *
   * @return Count of the total number of unique IP addresses of snowflake
   *     proxies that have an unknown NAT type.
   * @since 2.16.0
   */
  Optional<Long> snowflakeIpsNatUnknown();
}

