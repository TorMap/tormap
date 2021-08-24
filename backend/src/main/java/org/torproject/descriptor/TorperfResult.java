/* Copyright 2012--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor;

import java.util.List;
import java.util.SortedMap;

/**
 * Contains performance measurement results from making simple HTTP
 * requests over the Tor network.
 *
 * <p>The performance measurement service Torperf publishes performance
 * data from making simple HTTP requests over the Tor network.  Torperf
 * uses a trivial SOCKS client to download files of various sizes over the
 * Tor network and notes how long substeps take.</p>
 *
 * @since 1.0.0
 */
public interface TorperfResult extends Descriptor {

  /**
   * Return all unrecognized keys together with their values, or null if
   * all keys were recognized.
   *
   * @since 1.2.0
   */
  SortedMap<String, String> getUnrecognizedKeys();

  /**
   * Return the configured name of the data source.
   *
   * @since 1.0.0
   */
  String getSource();

  /**
   * Return the configured file size in bytes.
   *
   * @since 1.0.0
   */
  int getFileSize();

  /**
   * Return the time in milliseconds since the epoch when the connection
   * process started.
   *
   * @since 1.0.0
   */
  long getStartMillis();

  /**
   * Return the time in milliseconds since the epoch when the socket was
   * created.
   *
   * @since 1.0.0
   */
  long getSocketMillis();

  /**
   * Return the time in milliseconds since the epoch when the socket was
   * connected.
   *
   * @since 1.0.0
   */
  long getConnectMillis();

  /**
   * Return the time in milliseconds since the epoch when SOCKS 5
   * authentication methods have been negotiated.
   *
   * @since 1.0.0
   */
  long getNegotiateMillis();

  /**
   * Return the time in milliseconds since the epoch when the SOCKS
   * request was sent.
   *
   * @since 1.0.0
   */
  long getRequestMillis();

  /**
   * Return the time in milliseconds since the epoch when the SOCKS
   * response was received.
   *
   * @since 1.0.0
   */
  long getResponseMillis();

  /**
   * Return the time in milliseconds since the epoch when the HTTP
   * request was written.
   *
   * @since 1.0.0
   */
  long getDataRequestMillis();

  /**
   * Return the time in milliseconds since the epoch when the first
   * response was received.
   *
   * @since 1.0.0
   */
  long getDataResponseMillis();

  /**
   * Return the time in milliseconds since the epoch when the payload was
   * complete.
   *
   * @since 1.0.0
   */
  long getDataCompleteMillis();

  /**
   * Return the total number of bytes written.
   *
   * @since 1.0.0
   */
  int getWriteBytes();

  /**
   * Return the total number of bytes read.
   *
   * @since 1.0.0
   */
  int getReadBytes();

  /**
   * Return whether the request timed out (as opposed to failing), or
   * null if the torperf line didn't contain that information.
   *
   * @since 1.0.0
   */
  Boolean didTimeout();

  /**
   * Return the times in milliseconds since the epoch when the given number of
   * bytes were read, or null if the torperf line didn't contain that
   * information.
   *
   * @since 2.13.0
   */
  SortedMap<Integer, Long> getPartials();

  /**
   * Return the times in milliseconds since the epoch when {@code x%} of
   * expected bytes were read for {@code 0 <= x <= 100}, or null if the
   * torperf line didn't contain that information.
   *
   * @since 1.0.0
   */
  SortedMap<Integer, Long> getDataPercentiles();

  /**
   * Return the time in milliseconds since the epoch when the circuit was
   * launched, or -1 if the torperf line didn't contain that
   * information.
   *
   * @since 1.0.0
   */
  long getLaunchMillis();

  /**
   * Return the time in milliseconds since the epoch when the circuit was
   * used, or -1 if the torperf line didn't contain that information.
   *
   * @since 1.0.0
   */
  long getUsedAtMillis();

  /**
   * Return a list of fingerprints of the relays in the circuit, or null
   * if the torperf line didn't contain that information.
   *
   * @since 1.0.0
   */
  List<String> getPath();

  /**
   * Return a list of times in milliseconds between launching the circuit and
   * extending to the next circuit hop, or null if the torperf line didn't
   * contain that information.
   *
   * @since 1.0.0
   */
  List<Long> getBuildTimes();

  /**
   * Return the circuit build timeout that the Tor client used when
   * building this circuit, or -1 if the torperf line didn't contain that
   * information.
   *
   * @since 1.0.0
   */
  long getTimeout();

  /**
   * Return the circuit build time quantile that the Tor client uses to
   * determine its circuit-build timeout, or -1 if the torperf line
   * didn't contain that information.
   *
   * @since 1.0.0
   */
  double getQuantile();

  /**
   * Return the identifier of the circuit used for this measurement, or
   * -1 if the torperf line didn't contain that information.
   *
   * @since 1.0.0
   */
  int getCircId();

  /**
   * Return the identifier of the stream used for this measurement, or -1
   * if the torperf line didn't contain that information.
   *
   * @since 1.0.0
   */
  int getUsedBy();

  /**
   * Return the hostname, IP address, and port that the TGen client used to
   * connect to the local tor SOCKS port, formatted as
   * {@code hostname:ip:port}, which may be {@code "NULL:0.0.0.0:0"}
   * if TGen was not able to find this information or {@code null} if the
   * OnionPerf line didn't contain this information.
   *
   * @since 1.7.0
   */
  String getEndpointLocal();

  /**
   * Return the hostname, IP address, and port that the TGen client used to
   * connect to the SOCKS proxy server that tor runs, formatted as
   * {@code hostname:ip:port}, which may be {@code "NULL:0.0.0.0:0"}
   * if TGen was not able to find this information or {@code null} if the
   * OnionPerf line didn't contain this information.
   *
   * @since 1.7.0
   */
  String getEndpointProxy();

  /**
   * Return the hostname, IP address, and port that the TGen client used to
   * connect to the remote server, formatted as {@code hostname:ip:port},
   * which may be {@code "NULL:0.0.0.0:0"} if TGen was not able to find
   * this information or {@code null} if the OnionPerf line didn't contain
   * this information.
   *
   * @since 1.7.0
   */
  String getEndpointRemote();

  /**
   * Return the client machine hostname, which may be {@code "(NULL)"} if
   * the TGen client was not able to find this information or {@code null}
   * if the OnionPerf line didn't contain this information.
   *
   * @since 1.7.0
   */
  String getHostnameLocal();

  /**
   * Return the server machine hostname, which may be {@code "(NULL)"} if
   * the TGen server was not able to find this information or {@code null}
   * if the OnionPerf line didn't contain this information.
   *
   * @since 1.7.0
   */
  String getHostnameRemote();

  /**
   * Return the public IP address of the OnionPerf host obtained by connecting
   * to well-known servers and finding the IP address in the result, which may
   * be {@code "unknown"} if OnionPerf was not able to find this
   * information or {@code null} if the OnionPerf line didn't contain this
   * information.
   *
   * @since 1.7.0
   */
  String getSourceAddress();

  /**
   * Return the combined error code contained in the {@code tgen} client logs
   * and the {@code tor} client logs, or {@code null} if no error occured or if
   * the OnionPerf line didn't contain this information.
   *
   * @since 2.14.0
   */
  String getErrorCode();
}

