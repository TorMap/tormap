/* Copyright 2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.onionperf;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import java.io.IOException;
import java.util.Map;

/**
 * Parsed OnionPerf analysis document with all relevant fields for
 * {@link OnionPerfAnalysisConverter} to convert contained measurements to
 * {@link org.torproject.descriptor.TorperfResult} instances.
 */
public class ParsedOnionPerfAnalysis {

  /**
   * Object mapper for deserializing OnionPerf analysis documents to instances
   * of this class.
   */
  private static final ObjectMapper objectMapper = new ObjectMapper()
      .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
      .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
      .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  /**
   * Deserialize an OnionPerf analysis document from the given uncompressed
   * bytes.
   *
   * @param bytes Uncompressed contents of the OnionPerf analysis to
   *     deserialize.
   * @return Parsed OnionPerf analysis document.
   * @throws IOException Thrown if something goes wrong while deserializing the
   *     given JSON document, but before doing any verification or
   *     postprocessing.
   */
  static ParsedOnionPerfAnalysis fromBytes(byte[] bytes) throws IOException {
    return objectMapper.readValue(bytes, ParsedOnionPerfAnalysis.class);
  }

  /**
   * OnionPerf measurement data by source nickname.
   */
  Map<String, MeasurementData> data;

  /**
   * Descriptor type, which should always be {@code "onionperf"} for OnionPerf
   * analysis documents.
   */
  String type;

  /**
   * Document version, which is either a {@link Double} in version 1.0 or a
   * {@link String} in subsequent versions.
   */
  Object version;

  /**
   * Measurement data obtained from client-side {@code tgen} and {@code tor}
   * controller event logs.
   */
  static class MeasurementData {

    /**
     * Public IP address of the OnionPerf host obtained by connecting to
     * well-known servers and finding the IP address in the result, which may be
     * {@code "unknown"} if OnionPerf was not able to find this information.
     */
    String measurementIp;

    /**
     * Measurement data obtained from client-side {@code tgen} logs.
     */
    TgenData tgen;

    /**
     * Measurement data obtained from client-side {@code tor} controller event
     * logs.
     */
    TorData tor;
  }

  /**
   * Measurement data obtained from client-side {@code tgen} logs.
   */
  static class TgenData {

    /**
     * Measurement data by transfer identifier.
     */
    Map<String, Transfer> transfers;

    /**
     * Measurement data by stream identifier.
     */
    Map<String, TgenStream> streams;
  }

  /**
   * Measurement data related to a single transfer obtained from client-side
   * {@code tgen} logs.
   */
  static class Transfer {

    /**
     * Elapsed seconds between starting a transfer at {@link #unixTsStart} and
     * reaching a set of pre-defined states.
     */
    ElapsedSeconds elapsedSeconds;

    /**
     * Hostname, IP address, and port that the {@code tgen} client used to
     * connect to the local {@code tor} SOCKS port, formatted as
     * {@code "hostname:ip:port"}, which may be {@code "NULL:0.0.0.0:0"} if
     * {@code tgen} was not able to find this information.
     */
    String endpointLocal;

    /**
     * Hostname, IP address, and port that the {@code tgen} client used to
     * connect to the SOCKS proxy server that {@code tor} runs, formatted as
     * {@code "hostname:ip:port"}, which may be {@code "NULL:0.0.0.0:0"} if
     * {@code tgen} was not able to find this information.
     */
    String endpointProxy;

    /**
     * Hostname, IP address, and port that the {@code tgen} client used to
     * connect to the remote server, formatted as {@code "hostname:ip:port"},
     * which may be {@code "NULL:0.0.0.0:0"} if {@code tgen} was not able to
     * find this information.
     */
    String endpointRemote;

    /**
     * Error code reported in the client {@code tgen} logs, which can be
     * {@code "NONE"} if no error was encountered, {@code "PROXY"} in case of an
     * error in {@code tor}, or something else for {@code tgen}-specific errors.
     */
    String errorCode;

    /**
     * File size in bytes of the requested file in this transfer.
     */
    Integer filesizeBytes;

    /**
     * Client machine hostname, which may be {@code "(NULL)"} if the
     * {@code tgen} client was not able to find this information.
     */
    String hostnameLocal;

    /**
     * Server machine hostname, which may be {@code "(NULL)"} if the
     * {@code tgen} server was not able to find this information.
     */
    String hostnameRemote;

    /**
     * Whether or not an error was encountered in this transfer.
     */
    Boolean isError;

    /**
     * Total number of bytes read in this transfer.
     */
    Integer totalBytesRead;

    /**
     * Total number of bytes written in this transfer.
     */
    Integer totalBytesWrite;

    /**
     * Unix timestamp when this transfer started.
     */
    Double unixTsStart;

    /**
     * Unix timestamp when this transfer ended.
     */
    Double unixTsEnd;
  }

  /**
   * Elapsed seconds between starting a transfer and reaching a set of
   * pre-defined states.
   */
  static class ElapsedSeconds {

    /**
     * Time until the HTTP request was written.
     */
    Double command;

    /**
     * Time until the payload was complete.
     */
    Double lastByte;

    /**
     * Time until the given number of bytes were read.
     */
    Map<String, Double> payloadBytes;

    /**
     * Time until the given fraction of expected bytes were read.
     */
    Map<String, Double> payloadProgress;

    /**
     * Time until SOCKS 5 authentication methods have been negotiated.
     */
    Double proxyChoice;

    /**
     * Time until the SOCKS request was sent.
     */
    Double proxyRequest;

    /**
     * Time until the SOCKS response was received.
     */
    Double proxyResponse;

    /**
     * Time until the first response was received.
     */
    Double response;

    /**
     * Time until the socket was connected.
     */
    Double socketConnect;

    /**
     * Time until the socket was created.
     */
    Double socketCreate;
  }

  /**
   * Measurement data related to a single stream obtained from client-side
   * {@code tgen} logs.
   */
  static class TgenStream {

    /**
     * Information on sent and received bytes.
     */
    ByteInfo byteInfo;

    /**
     * Elapsed seconds until a given number or fraction of payload bytes have
     * been received or sent, obtained from {@code [stream-status]},
     * {@code [stream-success]}, and {@code [stream-error]} log messages, only
     * included if the measurement was a success.
     */
    ElapsedSecondsPayload elapsedSeconds;

    /**
     * Whether an error occurred.
     */
    Boolean isError;

    /**
     * Information about the TGen stream.
     */
    StreamInfo streamInfo;

    /**
     * Elapsed time until reaching given substeps in a measurement.
     */
    TimeInfo timeInfo;

    /**
     * Information about the TGen transport.
     */
    TransportInfo transportInfo;

    /**
     * Initial start time of the measurement, obtained by subtracting the
     * largest number of elapsed microseconds in {@code time_info} from
     * {@code unix_ts_end}, given in seconds since the epoch.
     */
    Double unixTsStart;

    /**
     * Final end time of the measurement, obtained from the log time of the
     * {@code [stream-success]} or {@code [stream-error]} log message, given in
     * seconds since the epoch.
     */
    Double unixTsEnd;
  }

  /**
   * Information on sent and received bytes.
   */
  static class ByteInfo {

    /**
     * Total number of bytes received.
     */
    @JsonProperty("total-bytes-recv")
    String totalBytesRecv;

    /**
     * Total number of bytes sent.
     */
    @JsonProperty("total-bytes-send")
    String totalBytesSend;
  }

  /**
   * Elapsed seconds until a given number or fraction of payload bytes have been
   * received or sent, obtained from {@code [stream-status]},
   * {@code [stream-success]}, and {@code [stream-error]} log messages, only
   * included if the measurement was a success.
   */
  static class ElapsedSecondsPayload {

    /**
     * Number of received payload bytes.
     */
    Map<String, Double> payloadBytesRecv;

    /**
     * Fraction of received payload bytes.
     */
    Map<String, Double> payloadProgressRecv;
  }

  /**
   * Information about the TGen stream.
   */
  static class StreamInfo {

    /**
     * Error code, or {@code NONE} if no error occurred.
     */
    String error;

    /**
     * Hostname of the TGen client.
     */
    String name;

    /**
     * Hostname of the TGen server.
     */
    String peername;

    /**
     * Number of expected payload bytes in the response.
     */
    String recvsize;
  }

  /**
   * Elapsed time until reaching given substeps in a measurement.
   */
  static class TimeInfo {

    /**
     * Elapsed microseconds until the TGen client has sent the command to the
     * TGen server, or -1 if missing (step 7).
     */
    @JsonProperty("usecs-to-command")
    String usecsToCommand;

    /**
     * Elapsed microseconds until the TGen client has received the last payload
     * byte, or -1 if missing (step 10).
     */
    @JsonProperty("usecs-to-last-byte-recv")
    String usecsToLastByteRecv;

    /**
     * Elapsed microseconds until the TGen client has received the SOCKS choice
     * from the Tor client, or -1 if missing (step 4).
     */
    @JsonProperty("usecs-to-proxy-choice")
    String usecsToProxyChoice;

    /**
     * Elapsed microseconds until the TGen client has sent the SOCKS request to
     * the Tor client, or -1 if missing (step 5).
     */
    @JsonProperty("usecs-to-proxy-request")
    String usecsToProxyRequest;

    /**
     * Elapsed microseconds until the TGen client has received the SOCKS
     * response from the Tor client, or -1 if missing (step 6).
     */
    @JsonProperty("usecs-to-proxy-response")
    String usecsToProxyResponse;

    /**
     * Elapsed microseconds until the TGen client has received the command from
     * the TGen server, or -1 if missing (step 8).
     */
    @JsonProperty("usecs-to-response")
    String usecsToResponse;

    /**
     * Elapsed microseconds until the TGen client has connected to the Tor
     * client's SOCKS port, or -1 if missing (step 2).
     */
    @JsonProperty("usecs-to-socket-connect")
    String usecsToSocketConnect;

    /**
     * Elapsed microseconds until the TGen client has opened a TCP connection
     * to the Tor client's SOCKS port, or -1 if missing (step 1).
     */
    @JsonProperty("usecs-to-socket-create")
    String usecsToSocketCreate;
  }

  /**
   * Information about the TGen transport.
   */
  static class TransportInfo {

    /**
     * Local host name, IP address, and TCP port.
     */
    String local;

    /**
     * Proxy host name, IP address, and TCP port.
     */
    String proxy;

    /**
     * Remote host name, IP address, and TCP port.
     */
    String remote;
  }

  /**
   * Measurement data obtained from client-side {@code tor} controller event
   * logs.
   */
  static class TorData {

    /**
     * Circuits by identifier.
     */
    Map<String, Circuit> circuits;

    /**
     * Streams by identifier.
     */
    Map<String, Stream> streams;
  }

  /**
   * Measurement data related to a single circuit obtained from client-side
   * {@code tor} controller event logs.
   */
  static class Circuit {

    /**
     * Circuit build time quantile that the {@code tor} client uses to determine
     * its circuit-build timeout.
     */
    Double buildQuantile;

    /**
     * Circuit build timeout in milliseconds that the {@code tor} client used
     * when building this circuit.
     */
    Integer buildTimeout;

    /**
     * Circuit identifier.
     */
    Integer circuitId;

    /**
     * Path information as two-dimensional array with a mixed-type
     * {@link Object[]} for each hop with {@code "$fingerprint~nickname"} as
     * first element and elapsed seconds between creating and extending the
     * circuit as second element.
     */
    Object[][] path;

    /**
     * Unix timestamp at the start of this circuit's lifetime.
     */
    Double unixTsStart;

    /**
     * Whether the circuit matched the filter criteria defined at measurement
     * runtime.
     */
    String filteredOut;
  }

  /**
   * Measurement data related to a single stream obtained from client-side
   * {@code tor} controller event logs.
   */
  static class Stream {

    /**
     * Circuit identifier of the circuit that this stream was attached to.
     */
    String circuitId;

    /**
     * Local reason why this stream failed.
     */
    String failureReasonLocal;

    /**
     * Remote reason why this stream failed.
     */
    String failureReasonRemote;

    /**
     * Source address and port that requested the connection.
     */
    String source;

    /**
     * Stream identifier.
     */
    Integer streamId;

    /**
     * Unix timestamp at the end of this stream's lifetime.
     */
    Double unixTsEnd;
  }
}

