/* Copyright 2012--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * Contains a relay or sanitized bridge extra-info descriptor.
 *
 * <p>Relays publish extra-info descriptors as an addendum to server
 * descriptors ({@link ServerDescriptor}) to report extraneous information
 * to the directory authorities that clients do not need to download in
 * order to function.  This information primarily consists of statistics
 * gathered by the relay about its usage and can take up a lot of
 * descriptor space.  The separation of server descriptors and extra-info
 * descriptors has become less relevant with the introduction of
 * microdescriptors ({@link Microdescriptor}) that are derived from server
 * descriptors by the directory authority and which clients download
 * instead of server descriptors, but it persists.</p>
 *
 * <p>Bridges publish extra-info descriptors to the bridge authority for
 * the same reason, to include statistics about their usage without
 * increasing the directory protocol overhead for bridge clients.  In this
 * case, the separation of server descriptors and extra-info descriptors
 * is slightly more relevant, because there are no microdescriptors for
 * bridges, so that bridge clients still download server descriptors of
 * bridges they're using.  Another reason is that bridges need to include
 * information like details of all the transports they support in their
 * descriptors, and bridge clients using one such transport are not
 * supposed to learn the details of the other transports.</p>
 *
 * <p>It's worth noting that all contents of extra-info descriptors are
 * written and signed by relays and bridges without a third party
 * verifying their correctness.  The (bridge) directory authorities may
 * decide to exclude dishonest servers from the network statuses they
 * produce, but that wouldn't be reflected in extra-info descriptors.</p>
 *
 * @since 1.0.0
 */
public interface ExtraInfoDescriptor extends Descriptor {

  /**
   * Get the SHA-1 descriptor digest, encoded as 40 lower-case (relay
   * descriptors) or upper-case (bridge descriptors) hexadecimal
   * characters, that is used to reference this descriptor from a server
   * descriptor.
   *
   * @return SHA-1 descriptor digest or bridge descriptor
   *
   * @since 1.7.0
   */
  String getDigestSha1Hex();

  /**
   * Get the SHA-256 descriptor digest, encoded as 43 base64
   * characters without padding characters, that may be used to reference
   * this descriptor from a server descriptor.
   *
   * @return SHA-256 descriptor digest
   *
   * @since 1.7.0
   */
  String getDigestSha256Base64();

  /**
   * Get the server's nickname consisting of 1 to 19 alphanumeric
   * characters.
   *
   * @return nickname
   *
   * @since 1.0.0
   */
  String getNickname();

  /**
   * Get the SHA-1 digest of the server's public identity key, encoded
   * as 40 upper-case hexadecimal characters, that is typically used to
   * uniquely identify the server.
   *
   * @return SHA-1 digest of the server's public identity key
   *
   * @since 1.0.0
   */
  String getFingerprint();

  /**
   * Get the time in milliseconds since the epoch when this descriptor
   * and the corresponding server descriptor were generated.
   *
   * @return time since the epoch
   *
   * @since 1.0.0
   */
  long getPublishedMillis();

  /**
   * Get the server's history of read bytes, or {@code null} if the descriptor
   * does not contain a bandwidth history; older Tor versions included
   * bandwidth histories in their server descriptors
   * ({@link ServerDescriptor#getReadHistory()}).
   *
   * @return read bytes or {@code null}
   *
   * @since 1.0.0
   */
  BandwidthHistory getReadHistory();

  /**
   * Get the server's history of written bytes, or {@code null} if the
   * descriptor does not contain a bandwidth history; older Tor versions
   * included bandwidth histories in their server descriptors
   * ({@link ServerDescriptor#getWriteHistory()}).
   *
   * @return written bytes or {@code null}
   *
   * @since 1.0.0
   */
  BandwidthHistory getWriteHistory();

  /**
   * Get the server's history of written IPv6 bytes, or {@code null} if the
   * descriptor does not contain a bandwidth history.
   *
   * @return written IPv6 bytes or {@code null}
   *
   * @since 2.14.0
   */
  BandwidthHistory getIpv6WriteHistory();

  /**
   * Get the server's history of read IPv6 bytes, or {@code null} if the
   * descriptor does not contain a bandwidth history.
   *
   * @return read IPv6 bytes or {@code null}
   *
   * @since 2.14.0
   */
  BandwidthHistory getIpv6ReadHistory();

  /**
   * Get the version number in the overload-ratelimits line or
   * {@code 0} if no overload-ratelimits line is present.
   *
   * @return version number or {@code 0}
   *
   * @since 2.18.0
   */
  int getOverloadRatelimitsVersion();

  /**
   * Get the server's history exhausted bandwidth as a timestamp of the
   * last time this happened, or -1 if the descriptor does not
   * contain a bandwidth overload rate limit.
   *
   * @return timestamp or {@code -1L}
   *
   * @since 2.18.0
   */
  long getOverloadRatelimitsTimestamp();

  /**
   * Get the server's rate-limit, or {@code -1L} if the descriptor does not
   * contain a bandwidth overload rate limit.
   *
   * @return rate-limit or {@code -1L}
   *
   * @since 2.18.0
   */
  long getOverloadRatelimitsRateLimit();

  /**
   * Get the server's burst-limit, or {@code -1L} if the descriptor does not
   * contain a bandwidth overload rate limit.
   *
   * @return burst-limit or {@code -1L}
   *
   * @since 2.18.0
   */
  long getOverloadRatelimitsBurstLimit();

  /**
   * Get the server's read-overload-count, or {@code -1L} if the descriptor
   * does not contain a bandwidth overload rate limit.
   *
   * @return burst-limit or {@code -1L}
   *
   * @since 2.18.0
   */
  int getOverloadRatelimitsReadCount();

  /**
   * Get the server's write-overload-count, or -1 if the descriptor does not
   * contain a bandwidth overload rate limit.
   *
   * @return write-overload-count or {@code -1}
   *
   * @since 2.18.0
   */
  int getOverloadRatelimitsWriteCount();

  /**
   * Get the version number in the overload-fd-exhausted line.
   *
   * @return version or {@code 0}
   *
   * @since 2.18.0
   */
  int getOverloadFdExhaustedVersion();

  /**
   * Get the server's  descriptor exhaustion as a timestamp.
   * The timestamp indicates that the maximum was reached between the
   * timestamp and the "published" timestamp of the document.
   *
   * {@code -1L} is returned if the descriptor does not contain a
   * file descriptor exhaustion.
   *
   * @return timestamp or {@code -1L}
   *
   * @since 2.18.0
   */
  long getOverloadFdExhaustedTimestamp();

  /**
   * Get the SHA-1 digest of the GeoIP database file used by this server
   * to resolve client IP addresses to country codes, encoded as 40
   * upper-case hexadecimal characters, or {@code null} if no GeoIP database
   * digest is included.
   *
   * @return SHA-1 digest or {@code null}
   *
   * @since 1.7.0
   */
  String getGeoipDbDigestSha1Hex();

  /**
   * Get the SHA-1 digest of the GeoIPv6 database file used by this
   * server to resolve client IP addresses to country codes, encoded as 40
   * upper-case hexadecimal characters, or {@code null} if no GeoIPv6 database
   * digest is included.
   *
   * @return SHA-1 digest or {@code null}
   *
   * @since 1.7.0
   */
  String getGeoip6DbDigestSha1Hex();

  /**
   * Get the time in milliseconds since the epoch when the included
   * directory request statistics interval ended, or {@code -1L} if no
   * such statistics are included.
   *
   * @return time since the epoch or {@code -1}
   *
   * @since 1.0.0
   */
  long getDirreqStatsEndMillis();

  /**
   * Get the interval length of the included directory request
   * statistics in seconds, or -1 if no such statistics are included.
   *
   * @return interval length or {@code -1L}
   *
   * @since 1.0.0
   */
  long getDirreqStatsIntervalLength();

  /**
   * Get statistics on unique IP addresses requesting v2 network
   * statuses with map keys being country codes and map values being
   * numbers of unique IP addresses rounded up to the nearest multiple of
   * 8, or {@code null} if no such statistics are included (which is the
   * case with recent Tor versions).
   *
   * @return SortedMap ("contry code", "number of unique IPs"} or {@code null}
   *
   * @since 1.0.0
   */
  SortedMap<String, Integer> getDirreqV2Ips();

  /**
   * Get statistics on unique IP addresses requesting v3 network
   * status consensuses of any flavor with map keys being country codes
   * and map values being numbers of unique IP addresses rounded up to the
   * nearest multiple of 8, or {@code null} if no such statistics are included.
   *
   * @return SortedMap ("contry code", "number of unique IPs"} or {@code null}
   *
   * @since 1.0.0
   */
  SortedMap<String, Integer> getDirreqV3Ips();

  /**
   * Get statistics on directory requests for v2 network statuses with
   * map keys being country codes and map values being request numbers
   * rounded up to the nearest multiple of 8, or {@code null} if no such
   * statistics are included (which is the case with recent Tor
   * versions).
   *
   * @return SortedMap ("contry code", "number of requests"} or {@code null}
   *
   * @since 1.0.0
   */
  SortedMap<String, Integer> getDirreqV2Reqs();

  /**
   * Get statistics on directory requests for v3 network status
   * consensuses of any flavor with map keys being country codes and map
   * values being request numbers rounded up to the nearest multiple of 8,
   * or {@code null} if no such statistics are included.
   *
   * @return SortedMap ("contry code", "number of requests"} or {@code null}
   *
   * @since 1.0.0
   */
  SortedMap<String, Integer> getDirreqV3Reqs();

  /**
   * Get the share of requests for v2 network statuses that the server
   * expects to receive from clients, or -1.0 if this share is not
   * included (which is the case with recent Tor versions).
   *
   * @return share of request or {@code -1L}
   *
   * @since 1.0.0
   */
  double getDirreqV2Share();

  /**
   * Get the share of requests for v3 network status consensuses of
   * any flavor that the server expects to receive from clients, or -1.0
   * if this share is not included (which is the case with recent Tor
   * versions).
   *
   * @return share of request or {@code -1L}
   *
   * @since 1.0.0
   */
  double getDirreqV3Share();

  /**
   * Get statistics on responses to directory requests for v2 network
   * statuses with map keys being response strings and map values being
   * response numbers rounded up to the nearest multiple of 4, or
   * {@code null} if no such statistics are included (which is the case
   * with recent Tor versions).
   *
   * @return SortedMap ("response string","response number") or {@code null}
   *
   * @since 1.0.0
   */
  SortedMap<String, Integer> getDirreqV2Resp();

  /**
   * Get statistics on responses to directory requests for v3 network
   * status consensuses of any flavor with map keys being response strings
   * and map values being response numbers rounded up to the nearest
   * multiple of 4, or null if no such statistics are included.
   *
   * @return SortedMap ("response string","response number") or {@code null}
   *
   * @since 1.0.0
   */
  SortedMap<String, Integer> getDirreqV3Resp();

  /**
   * Get statistics on directory requests for v2 network statuses to
   * the server's directory port with map keys being statistic keys and
   * map values being statistic values like counts or quantiles, or
   * {@code null} if no such statistics are included (which is the case
   * with recent Tor versions).
   *
   * @return SortedMap ("key","value") or {@code null}
   *
   * @since 1.0.0
   */
  SortedMap<String, Integer> getDirreqV2DirectDl();

  /**
   * Get statistics on directory requests for v3 network status
   * consensuses of any flavor to the server's directory port with map
   * keys being statistic keys and map values being statistic values like
   * counts or quantiles, or {@code null} if no such statistics are included.
   *
   * @return SortedMap ("key","value") or {@code null}
   *
   * @since 1.0.0
   */
  SortedMap<String, Integer> getDirreqV3DirectDl();

  /**
   * Get statistics on directory requests for v2 network statuses
   * tunneled through a circuit with map keys being statistic keys and map
   * values being statistic values, or {@code null} if no such statistics
   * are included (which is the case with recent Tor versions).
   *
   * @return SortedMap ("key","value") or {@code null}
   *
   * @since 1.0.0
   */
  SortedMap<String, Integer> getDirreqV2TunneledDl();

  /**
   * Get statistics on directory requests for v3 network status
   * consensuses of any flavor tunneled through a circuit with map keys
   * being statistic keys and map values being statistic values, or
   * {@code null} if no such statistics are included.
   *
   * @return SortedMap ("key","value") or {@code null}
   *
   * @since 1.0.0
   */
  SortedMap<String, Integer> getDirreqV3TunneledDl();

  /**
   * Get the directory request read history contained in this
   * descriptor, or{@code null} if no such history is contained.
   *
   * @return read history or {@code null}
   *
   * @since 1.0.0
   */
  BandwidthHistory getDirreqReadHistory();

  /**
   * Get the directory request write history contained in this
   * descriptor, or {@code null} if no such history is contained.
   *
   * @return read history or {@code null}
   *
   * @since 1.0.0
   */
  BandwidthHistory getDirreqWriteHistory();

  /**
   * Get the time in milliseconds since the epoch when the included
   * entry statistics interval ended, or {@code -1L} if no such statistics are
   * included.
   *
   * @return time or {@code -1L}
   * @since 1.0.0
   */
  long getEntryStatsEndMillis();

  /**
   * Get the interval length of the included entry statistics in
   * seconds, or {@code -1L} if no such statistics are included.
   *
   * @return time interval or {@code -1L}
   *
   * @since 1.0.0
   */
  long getEntryStatsIntervalLength();

  /**
   * Get statistics on client IP addresses with map keys being country
   * codes and map values being the number of unique IP addresses that
   * have connected from that country rounded up to the nearest multiple
   * of 8, or {@code null} if no such statistics are included.
   *
   * @return SrotedMap ("country code", "number of unique IPs") or {@code null}
   *
   * @since 1.0.0
   */
  SortedMap<String, Integer> getEntryIps();

  /**
   * Get the time in milliseconds since the epoch when the included
   * cell statistics interval ended, or {@code -1L} if no such
   * statistics are included.
   *
   * @return time since the epoch or {@code -1L}
   *
   * @since 1.0.0
   */
  long getCellStatsEndMillis();

  /**
   * Get the interval length of the included cell statistics in
   * seconds, or {@code -1L} if no such statistics are included.
   *
   * @return time interval or {@code -1L}
   *
   * @since 1.0.0
   */
  long getCellStatsIntervalLength();

  /**
   * Get the mean number of processed cells per circuit by circuit
   * decile starting with the loudest decile at index 0 and the quietest
   * decile at index 8, or {@code null} if no such statistics are included.
   *
   * @return mean of processed cells per circuit or {@code null}
   *
   * @since 1.0.0
   */
  List<Integer> getCellProcessedCells();

  /**
   * Get the mean number of cells contained in circuit queues by
   * circuit decile starting with the loudest decile at index 0 and the
   * quietest decile at index 8, or {@code null} if no such statistics are
   * included.
   *
   * @return mean of processed cells or {@code null}
   *
   * @since 1.0.0
   */
  List<Double> getCellQueuedCells();

  /**
   * Get the mean times in milliseconds that cells spend in circuit
   * queues by circuit decile starting with the loudest decile at index 0
   * and the quietest decile at index 8, or {@code null} if no such
   * statistics are included.
   *
   * @return mean of time or {@code null}
   *
   * @since 1.0.0
   */
  List<Integer> getCellTimeInQueue();

  /**
   * Get the mean number of circuits included in any of the cell
   * statistics deciles, or {@code -1} if no such statistics are included.
   *
   * @return mean number of circuits or {@code -1}
   *
   * @since 1.0.0
   */
  int getCellCircuitsPerDecile();

  /**
   * Get the time in milliseconds since the epoch when the included
   * statistics on bi-directional connection usage ended, or {@code -1L}
   * if no such statistics are included.
   *
   * @return time or {@code -1L}
   *
   * @since 1.0.0
   */
  long getConnBiDirectStatsEndMillis();

  /**
   * Get the interval length of the included statistics on
   * bi-directional connection usage in seconds, or {@code -1L} if
   * no such statistics are included.
   *
   * @return interval or {@code -1L}
   *
   * @since 1.0.0
   */
  long getConnBiDirectStatsIntervalLength();

  /**
   * Get the number of connections on which this server read and wrote
   * less than 2 KiB/s in a 10-second interval, or {@code -1} if no such
   * statistics are included.
   *
   * @return number of connections or {@code -1}
   *
   * @since 1.0.0
   */
  int getConnBiDirectBelow();

  /**
   * Get the number of connections on which this server read and wrote
   * at least 2 KiB/s in a 10-second interval and at least 10 times more
   * in read direction than in write direction, or -1 if no such
   * statistics are included.
   *
   * @return number of connections or {@code -1}
   *
   * @since 1.0.0
   */
  int getConnBiDirectRead();

  /**
   * Get the number of connections on which this server read and wrote
   * at least 2 KiB/s in a 10-second interval and at least 10 times more
   * in write direction than in read direction, or {@code -1} if no such
   * statistics are included.
   *
   * @return number of connections or {@code -1}
   *
   * @since 1.0.0
   */
  int getConnBiDirectWrite();

  /**
   * Get the number of connections on which this server read and wrote
   * at least 2 KiB/s in a 10-second interval but not 10 times more in
   * either direction, or {@code -1} if no such statistics are included.
   *
   * @return number of connections or {@code -1}
   *
   * @since 1.0.0
   */
  int getConnBiDirectBoth();

  /**
   * Get the time in milliseconds since the epoch when the included
   * statistics on bi-directional IPv6 connection usage ended, or {@code -1L}
   * if no such statistics are included.
   *
   * @return time or {@code -1L}
   *
   * @since 2.14.0
   */
  long getIpv6ConnBiDirectStatsEndMillis();

  /**
   * Get the interval length of the included statistics on
   * bi-directional IPv6 connection usage in seconds, or {@code -1L} if no such
   * statistics are included.
   *
   * @return inteval or {@code -1L}
   *
   * @since 2.14.0
   */
  long getIpv6ConnBiDirectStatsIntervalLength();

  /**
   * Get the number of IPv6 connections on which this server read and wrote
   * less than 2 KiB/s in a 10-second interval, or {@code -1} if no such
   * statistics are included.
   *
   * @return number of connections or {@code -1}
   *
   * @since 2.14.0
   */
  int getIpv6ConnBiDirectBelow();

  /**
   * Get the number of IPv6 connections on which this server read and wrote
   * at least 2 KiB/s in a 10-second interval and at least 10 times more
   * in read direction than in write direction, or -1 if no such
   * statistics are included.
   *
   * @return number of connections or {@code -1}
   *
   * @since 2.14.0
   */
  int getIpv6ConnBiDirectRead();

  /**
   * Get the number of IPv6 connections on which this server read and wrote
   * at least 2 KiB/s in a 10-second interval and at least 10 times more
   * in write direction than in read direction, or {@code -1} if no such
   * statistics are included.
   *
   * @return number of connections or {@code -1}
   * @since 2.14.0
   */
  int getIpv6ConnBiDirectWrite();

  /**
   * Get the number of IPv6 connections on which this server read and wrote
   * at least 2 KiB/s in a 10-second interval but not 10 times more in
   * either direction, or {@code -1} if no such statistics are included.
   *
   * @return number of connections or {@code -1}
   *
   * @since 2.14.0
   */
  int getIpv6ConnBiDirectBoth();

  /**
   * Get the time in milliseconds since the epoch when the included
   * exit statistics interval ended, or -1 if no such statistics are
   * included.
   *
   * @return time in milliseconds or -1
   *
   * @since 1.0.0
   */
  long getExitStatsEndMillis();

  /**
   * Get the interval length of the included exit statistics in
   * seconds, or -1 if no such statistics are included.
   *
   * @return interval length or -1
   *
   * @since 1.0.0
   */
  long getExitStatsIntervalLength();

  /**
   * Get statistics on KiB written to streams exiting the Tor network
   * by target TCP port with map keys being string representations of
   * ports (or {@code "other"}) and map values being KiB rounded up to the
   * next full KiB, or {@code null} if no such statistics are included.
   *
   * @return KiB written {@code null}
   *
   * @since 1.0.0
   */
  SortedMap<String, Long> getExitKibibytesWritten();

  /**
   * Get statistics on KiB read from streams exiting the Tor network
   * by target TCP port with map keys being string representations of
   * ports (or {@code "other"}) and map values being KiB rounded up to the
   * next full KiB, or {@code null} if no such statistics are included.
   *
   * @return KiB read {@code null}
   *
   * @since 1.0.0
   */
  SortedMap<String, Long> getExitKibibytesRead();

  /**
   * Get statistics on opened streams exiting the Tor network by
   * target TCP port with map keys being string representations of ports
   * (or {@code "other"}) and map values being the number of opened
   * streams, rounded up to the nearest multiple of 4, or {@code null}
   * if no such statistics are included.
   *
   * @return SortedMap ("port","opened streams") or {@code null}
   * @since 1.0.0
   */
  SortedMap<String, Long> getExitStreamsOpened();

  /**
   * Get the time in milliseconds since the epoch when the included
   * "geoip" statistics interval started, or {@code -1L} if no such
   * statistics are included (which is the case except for very old
   * Tor versions).
   *
   * @return time or {@code -1L}
   *
   * @since 1.0.0
   */
  long getGeoipStartTimeMillis();

  /**
   * Get statistics on the origin of client IP addresses with map keys
   * being country codes and map values being the number of unique IP
   * addresses that have connected from that country between the start of
   * the statistics interval and the descriptor publication time rounded
   * up to the nearest multiple of 8, or {@code null} if no such statistics are
   * included (which is the case except for very old Tor versions).
   *
   * @return SortedMap of ("country code", "number of unique IP addresses") or
   * {@code null}
   *
   * @since 1.0.0
   */
  SortedMap<String, Integer> getGeoipClientOrigins();

  /**
   * Get the time in milliseconds since the epoch when the included
   * bridge statistics interval ended, or -1 if no such statistics are
   * included.
   *
   * @return time or {@code -1}
   *
   * @since 1.0.0
   */
  long getBridgeStatsEndMillis();

  /**
   * Get the interval length of the included bridge statistics in
   * seconds, or {@code -1L} if no such statistics are included.
   *
   * @return interval or {@code -1L}
   *
   * @since 1.0.0
   */
  long getBridgeStatsIntervalLength();

  /**
   * Get statistics on bridge client IP addresses by country with map
   * keys being country codes and map values being the number of unique IP
   * addresses that have connected from that country rounded up to the
   * nearest multiple of 8, or null if no such statistics are included.
   *
   * @since 1.0.0
   */
  SortedMap<String, Integer> getBridgeIps();

  /**
   * Get statistics on bridge client IP addresses by IP version with
   * map keys being protocol families, e.g., {@code "v4"} or {@code "v6"},
   * and map values being the number of unique IP addresses rounded up to
   * the nearest multiple of 8, or {@code null} if no such statistics are
   * included.
   *
   * @return SortedMap ("protocol","number of unique IPs") or {@code null}
   *
   * @since 1.0.0
   */
  SortedMap<String, Integer> getBridgeIpVersions();

  /**
   * Get statistics on bridge client IP addresses by transport with
   * map keys being pluggable transport names, e.g., {@code "obfs2"} or
   * {@code "obfs3"} for known transports, {@code "<OR>"} for the default
   * onion routing protocol, or {@code "<??>"} for an unknown transport,
   * and map values being the number of unique IP addresses rounded up to
   * the nearest multiple of 8, or {@code null} if no such statistics are
   * included.
   *
   * @return SortedMap ("transport","number of unique IPs") or {@code null}
   *
   * @since 1.0.0
   */
  SortedMap<String, Integer> getBridgeIpTransports();

  /**
   * Get the (possibly empty) list of pluggable transports supported
   * by this server.
   *
   * @return list of pluggable transports
   *
   * @since 1.0.0
   */
  List<String> getTransports();

  /**
   * Get the time in milliseconds since the epoch when the included
   * hidden-service statistics interval ended, or {@code -1L} if no
   * such statistics are included.
   *
   * @return time since the epoch or {@code -1L}
   *
   * @since 1.1.0
   */
  long getHidservStatsEndMillis();

  /**
   * Get the interval length of the included hidden-service statistics
   * in seconds, or {@code -1L} if no such statistics are included.
   *
   * @return interval or {@code -1L}
   *
   * @since 1.1.0
   */
  long getHidservStatsIntervalLength();

  /**
   * Get the approximate number of RELAY cells seen in either
   * direction on a circuit after receiving and successfully processing a
   * RENDEZVOUS1 cell, or {@code null} if no such statistics are included.
   *
   * @return relay cells or {@code null}
   *
   * @since 1.1.0
   */
  Double getHidservRendRelayedCells();

  /**
   * Get the obfuscation parameters applied to the original
   * measurement value of RELAY cells seen in either direction on a
   * circuit after receiving and successfully processing a RENDEZVOUS1
   * cell, or {@code null} if no such statistics are included.
   *
   * @return Map ("parameter", "value of RELAY cells") or {@code null}
   *
   * @since 1.1.0
   */
  Map<String, Double> getHidservRendRelayedCellsParameters();

  /**
   * Get the approximate number of unique hidden-service identities
   * seen in descriptors published to and accepted by this hidden-service
   * directory, or {@code null} if no such statistics are included.
   *
   * @return number of unique hidden service identities or {@code null}
   *
   * @since 1.1.0
   */
  Double getHidservDirOnionsSeen();

  /**
   * Get the obfuscation parameters applied to the original
   * measurement value of unique hidden-service identities seen in
   * descriptors published to and accepted by this hidden-service
   * directory, or {@code null} if no such statistics are included.
   *
   * @return Map ("parameter", "identities") or {@code null}
   *
   * @since 1.1.0
   */
  Map<String, Double> getHidservDirOnionsSeenParameters();

  /**
   * Get the time in milliseconds since the epoch when the included version 3
   * onion service statistics interval ended, or {@code -1L} if no such
   * statistics are included.
   *
   * @return time or {@code -1L}
   *
   * @since 2.15.0
   */
  long getHidservV3StatsEndMillis();

  /**
   * Get the interval length of the included version 3 onion service
   * statistics in seconds, or {@code -1L} if no such statistics are included.
   *
   * @return interval or {@code -1L}
   *
   * @since 2.15.0
   */
  long getHidservV3StatsIntervalLength();

  /**
   * Get the approximate number of RELAY cells seen in either direction on a
   * version 3 onion service circuit after receiving and successfully processing
   * a RENDEZVOUS1 cell, or {@code null} if no such statistics are included.
   *
   * @return relay cells or {@code null}
   *
   * @since 2.15.0
   */
  Double getHidservRendV3RelayedCells();

  /**
   * Get the obfuscation parameters applied to the original measurement value
   * of RELAY cells seen in either direction on a version 3 onion service
   * circuit after receiving and successfully processing a RENDEZVOUS1 cell, or
   * {@code null} if no such statistics are included.
   *
   * @return Map ("parameter", "REALY cells") or {@code null}
   *
   * @since 2.15.0
   */
  Map<String, Double> getHidservRendV3RelayedCellsParameters();

  /**
   * Get the approximate number of unique version 3 onion service identities
   * seen in descriptors published to and accepted by this onion service
   * directory, or {@code null} if no such statistics are included.
   *
   * @return onion service identities or {@code null}
   *
   * @since 2.15.0
   */
  Double getHidservDirV3OnionsSeen();

  /**
   * Get the obfuscation parameters applied to the original measurement value
   * of unique version 3 onion service identities seen in descriptors published
   * to and accepted by this onion service directory, or {@code null} if no such
   * statistics are included.
   *
   * @return Map ("parameter", "onion service identities") or {@code null}
   *
   * @since 2.15.0
   */
  Map<String, Double> getHidservDirV3OnionsSeenParameters();

  /**
   * Get the time in milliseconds since the epoch when the included
   * padding-counts statistics ended, or {@code -1L} if no such statistics
   * are included.
   *
   * @return time or {@code -1L}
   *
   * @since 1.7.0
   */
  long getPaddingCountsStatsEndMillis();

  /**
   * Get the interval length of the included padding-counts statistics in
   * seconds, or {@code -1} if no such statistics are included.
   *
   * @return interval or {@code -1L}
   *
   * @since 1.7.0
   */
  long getPaddingCountsStatsIntervalLength();

  /**
   * Get padding-counts statistics, or {@code null} if no such
   * statistics are included.
   *
   * @return Map ("key","statistics") or {@code null}
   *
   * @since 1.7.0
   */
  Map<String, Long> getPaddingCounts();

  /**
   * Get the RSA-1024 signature of the PKCS1-padded descriptor digest,
   * taken from the beginning of the router line through the newline after
   * the router-signature line, or {@code null} if the descriptor
   * doesn't contain a signature (which is the case in sanitized bridge
   * descriptors).
   *
   * @return RSA-1024 signature or {@code null}
   *
   * @since 1.1.0
   */
  String getRouterSignature();

  /**
   * Get the Ed25519 certificate in PEM format, or {@code null} if the
   * descriptor doesn't contain one.
   *
   * @retunr Ed25519 certificate or {@code null}
   *
   * @since 1.1.0
   */
  String getIdentityEd25519();

  /**
   * Get the Ed25519 master key, encoded as 43 base64 characters
   * without padding characters, which was either parsed from the optional
   * {@code "master-key-ed25519"} line or derived from the (likewise
   * optional) Ed25519 certificate following the
   * {@code "identity-ed25519"} line, or {@code null} if the descriptor contains
   * neither Ed25519 master key nor Ed25519 certificate.
   *
   * @return Ed25519 master key or {@code null}
   *
   * @since 1.1.0
   */
  String getMasterKeyEd25519();

  /**
   * Get the Ed25519 signature of the SHA-256 digest of the entire
   * descriptor, encoded as 86 base64 characters without padding
   * characters, from the first character up to and including the first
   * space after the {@code "router-sig-ed25519"} string, prefixed with
   * the string {@code "Tor router descriptor signature v1"}.
   *
   * @return Ed25519 signature
   *
   * @since 1.1.0
   */
  String getRouterSignatureEd25519();
}
