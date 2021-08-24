/* Copyright 2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.onionperf;

import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.torproject.descriptor.Descriptor;
import org.torproject.descriptor.DescriptorParseException;
import org.torproject.descriptor.impl.TorperfResultImpl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converter that takes an OnionPerf analysis document as input and provides one
 * or more {@link org.torproject.descriptor.TorperfResult} instances as output.
 *
 * <p>This conversion matches {@code tgen} transfers and {@code tor} streams by
 * stream port and transfer/stream end timestamps. This is different from the
 * approach taken in OnionPerf's analyze mode which only matches by stream
 * port. The result is that converted Torperf results might contain different
 * path or build time information as Torperf results written by OnionPerf.</p>
 */
public class OnionPerfAnalysisConverter {

  /**
   * Uncompressed OnionPerf analysis file bytes.
   */
  private final byte[] rawDescriptorBytes;

  /**
   * OnionPerf analysis file.
   */
  private final File descriptorFile;

  /**
   * Converted Torperf results.
   */
  private List<Descriptor> convertedTorperfResults;

  /**
   * Construct a new instance from the given bytes and file reference.
   *
   * @param rawDescriptorBytes Uncompressed document bytes.
   * @param descriptorFile Document file reference.
   */
  public OnionPerfAnalysisConverter(byte[] rawDescriptorBytes,
      File descriptorFile) {
    this.rawDescriptorBytes = rawDescriptorBytes;
    this.descriptorFile = descriptorFile;
  }

  /**
   * Parse the OnionPerf analysis JSON document, do some basic verification, and
   * convert its contents to {@link org.torproject.descriptor.TorperfResult}
   * descriptors.
   *
   * @return Converted transfers.
   * @throws DescriptorParseException Thrown if something goes wrong while
   *     parsing, verifying, or converting the OnionPerf analysis file to
   *     Torperf results.
   */
  public List<Descriptor> asTorperfResults() throws DescriptorParseException {
    ParsedOnionPerfAnalysis parsedOnionPerfAnalysis
        = this.parseOnionPerfAnalysis();
    this.verifyDocumentTypeAndVersion(parsedOnionPerfAnalysis);
    StringBuilder formattedTorperfResults
        = this.formatTorperfResults(parsedOnionPerfAnalysis);
    this.parseFormattedTorperfResults(formattedTorperfResults);
    return this.convertedTorperfResults;
  }

  /**
   * Parse the OnionPerf analysis JSON document.
   */
  private ParsedOnionPerfAnalysis parseOnionPerfAnalysis()
      throws DescriptorParseException {
    try {
      InputStream compressedInputStream = new ByteArrayInputStream(
          this.rawDescriptorBytes);
      InputStream decompressedInputStream = new XZCompressorInputStream(
          compressedInputStream);
      byte[] decompressedBytes = IOUtils.toByteArray(decompressedInputStream);
      return ParsedOnionPerfAnalysis.fromBytes(decompressedBytes);
    } catch (IOException ioException) {
      throw new DescriptorParseException("Ran into an I/O error while "
          + "attempting to parse an OnionPerf analysis document.",
          ioException);
    }
  }

  /**
   * Verify document type and version and throw an exception when either of the
   * two indicates that we cannot process the document.
   *
   * @param parsedOnionPerfAnalysis Parsed OnionPerf analysis document.
   * @throws DescriptorParseException Thrown if either type or version indicate
   *     that we cannot process the document.
   */
  private void verifyDocumentTypeAndVersion(
      ParsedOnionPerfAnalysis parsedOnionPerfAnalysis)
      throws DescriptorParseException {
    if (!"onionperf".equals(parsedOnionPerfAnalysis.type)) {
      throw new DescriptorParseException("Parsed OnionPerf analysis file does "
          + "not contain type information.");
    }
    if (null == parsedOnionPerfAnalysis.version) {
      throw new DescriptorParseException("Parsed OnionPerf analysis file does "
          + "not contain version information.");
    } else if ((parsedOnionPerfAnalysis.version instanceof Double
        && (double) parsedOnionPerfAnalysis.version > 3.999)
        || (parsedOnionPerfAnalysis.version instanceof String
        && ((String) parsedOnionPerfAnalysis.version).compareTo("4.") >= 0)) {
      throw new DescriptorParseException("Parsed OnionPerf analysis file "
          + "contains unsupported version " + parsedOnionPerfAnalysis.version
          + ".");
    }
  }

  /**
   * Format the parsed OnionPerf analysis file as one or more Torperf result
   * strings.
   *
   * @param parsedOnionPerfAnalysis Parsed OnionPerf analysis document.
   */
  private StringBuilder formatTorperfResults(
      ParsedOnionPerfAnalysis parsedOnionPerfAnalysis) {
    StringBuilder formattedTorperfResults = new StringBuilder();
    for (Map.Entry<String, ParsedOnionPerfAnalysis.MeasurementData> data
        : parsedOnionPerfAnalysis.data.entrySet()) {
      String nickname = data.getKey();
      ParsedOnionPerfAnalysis.MeasurementData measurements = data.getValue();
      if (null == measurements.tgen) {
        continue;
      }
      String measurementIp = measurements.measurementIp;
      Map<String, List<ParsedOnionPerfAnalysis.Stream>> streamsBySourcePort
          = new HashMap<>();
      Map<String, ParsedOnionPerfAnalysis.Circuit> circuitsByCircuitId
          = new HashMap<>();
      if (null != measurements.tor) {
        circuitsByCircuitId = measurements.tor.circuits;
        if (null != measurements.tor.streams) {
          for (ParsedOnionPerfAnalysis.Stream stream
              : measurements.tor.streams.values()) {
            if (null != stream.source && stream.source.contains(":")) {
              String sourcePort = stream.source.split(":")[1];
              streamsBySourcePort.putIfAbsent(sourcePort, new ArrayList<>());
              streamsBySourcePort.get(sourcePort).add(stream);
            }
          }
        }
      }
      if (null != measurements.tgen.transfers) {
        for (ParsedOnionPerfAnalysis.Transfer transfer
            : measurements.tgen.transfers.values()) {
          TorperfResultsBuilder torperfResultsBuilder
              = new TorperfResultsBuilder();
          boolean isFiltered = false;
          torperfResultsBuilder.addString("SOURCE", nickname);
          torperfResultsBuilder.addString("SOURCEADDRESS", measurementIp);
          this.formatTransferParts(torperfResultsBuilder, transfer);
          if (null != transfer.endpointLocal) {
            String[] endpointLocalParts = transfer.endpointLocal.split(":");
            if (endpointLocalParts.length >= 3) {
              String sourcePort = endpointLocalParts[2];
              if (streamsBySourcePort.containsKey(sourcePort)) {
                for (ParsedOnionPerfAnalysis.Stream stream
                    : streamsBySourcePort.get(sourcePort)) {
                  if (Math.abs(transfer.unixTsEnd - stream.unixTsEnd) < 150.0) {
                    this.formatStreamParts(torperfResultsBuilder, stream);
                    if (null != stream.circuitId
                        && circuitsByCircuitId.containsKey(stream.circuitId)) {
                      ParsedOnionPerfAnalysis.Circuit circuit
                          = circuitsByCircuitId.get(stream.circuitId);
                      isFiltered = circuit.filteredOut != null
                              && circuit.filteredOut.equalsIgnoreCase("TRUE");
                      this.formatCircuitParts(torperfResultsBuilder, circuit);
                    }
                  }
                }
              }
            }
          }
          if (!isFiltered) {
            formattedTorperfResults.append(torperfResultsBuilder.build());
          }
        }
      }
      if (null != measurements.tgen.streams) {
        for (ParsedOnionPerfAnalysis.TgenStream stream
             : measurements.tgen.streams.values()) {
          TorperfResultsBuilder torperfResultsBuilder
              = new TorperfResultsBuilder();
          boolean isFiltered = false;
          torperfResultsBuilder.addString("SOURCE", nickname);
          torperfResultsBuilder.addString("SOURCEADDRESS", measurementIp);
          this.formatTgenStreamParts(torperfResultsBuilder, stream);
          if (null != stream.transportInfo
              && null != stream.transportInfo.local) {
            String[] endpointLocalParts = stream.transportInfo.local.split(":");
            if (endpointLocalParts.length >= 3) {
              String sourcePort = endpointLocalParts[2];
              if (streamsBySourcePort.containsKey(sourcePort)) {
                for (ParsedOnionPerfAnalysis.Stream torStream
                    : streamsBySourcePort.get(sourcePort)) {
                  if (Math.abs(stream.unixTsEnd
                      - torStream.unixTsEnd) < 150.0) {
                    this.formatStreamParts(torperfResultsBuilder, torStream);
                    if (null != torStream.circuitId && circuitsByCircuitId
                        .containsKey(torStream.circuitId)) {
                      ParsedOnionPerfAnalysis.Circuit circuit
                          = circuitsByCircuitId.get(torStream.circuitId);
                      isFiltered = circuit.filteredOut != null
                          && circuit.filteredOut.equalsIgnoreCase("TRUE");
                      this.formatCircuitParts(torperfResultsBuilder, circuit);
                    }
                  }
                }
              }
            }
          }
          if (!isFiltered) {
            formattedTorperfResults.append(torperfResultsBuilder.build());
          }
        }
      }
    }
    return formattedTorperfResults;
  }

  /**
   * Parse the previously formatted Torperf results.
   *
   * @param formattedTorperfResults Formatted Torperf result strings.
   * @throws DescriptorParseException Thrown when an error occurs while parsing
   *     a previously formatted {@link org.torproject.descriptor.TorperfResult}
   *     string.
   */
  private void parseFormattedTorperfResults(
      StringBuilder formattedTorperfResults) throws DescriptorParseException {
    this.convertedTorperfResults = TorperfResultImpl.parseTorperfResults(
        formattedTorperfResults.toString().getBytes(), this.descriptorFile);
  }

  /**
   * Format relevant tgen transfer data as Torperf result key-value pairs.
   *
   * @param torperfResultsBuilder Torperf results builder to add key-value pairs
   *     to.
   * @param transfer Transfer data obtained from the parsed OnionPerf analysis
   *     file.
   */
  private void formatTransferParts(TorperfResultsBuilder torperfResultsBuilder,
      ParsedOnionPerfAnalysis.Transfer transfer) {
    torperfResultsBuilder.addString("ENDPOINTLOCAL", transfer.endpointLocal);
    torperfResultsBuilder.addString("ENDPOINTPROXY", transfer.endpointProxy);
    torperfResultsBuilder.addString("ENDPOINTREMOTE", transfer.endpointRemote);
    torperfResultsBuilder.addString("HOSTNAMELOCAL", transfer.hostnameLocal);
    torperfResultsBuilder.addString("HOSTNAMEREMOTE", transfer.hostnameRemote);
    torperfResultsBuilder.addInteger("FILESIZE", transfer.filesizeBytes);
    torperfResultsBuilder.addInteger("READBYTES", transfer.totalBytesRead);
    torperfResultsBuilder.addInteger("WRITEBYTES", transfer.totalBytesWrite);
    torperfResultsBuilder.addInteger("DIDTIMEOUT", 0);
    for (String key : new String[] { "START", "SOCKET", "CONNECT", "NEGOTIATE",
        "REQUEST", "RESPONSE", "DATAREQUEST", "DATARESPONSE", "DATACOMPLETE",
        "LAUNCH", "DATAPERC10", "DATAPERC20", "DATAPERC30", "DATAPERC40",
        "DATAPERC50", "DATAPERC60", "DATAPERC70", "DATAPERC80", "DATAPERC90",
        "DATAPERC100" }) {
      torperfResultsBuilder.addString(key, "0.0");
    }
    torperfResultsBuilder.addTimestamp("START", transfer.unixTsStart, 0.0);
    if (null != transfer.unixTsStart && null != transfer.elapsedSeconds) {
      torperfResultsBuilder.addTimestamp("SOCKET", transfer.unixTsStart,
          transfer.elapsedSeconds.socketCreate);
      torperfResultsBuilder.addTimestamp("CONNECT", transfer.unixTsStart,
          transfer.elapsedSeconds.socketConnect);
      torperfResultsBuilder.addTimestamp("NEGOTIATE", transfer.unixTsStart,
          transfer.elapsedSeconds.proxyChoice);
      torperfResultsBuilder.addTimestamp("REQUEST", transfer.unixTsStart,
          transfer.elapsedSeconds.proxyRequest);
      torperfResultsBuilder.addTimestamp("RESPONSE", transfer.unixTsStart,
          transfer.elapsedSeconds.proxyResponse);
      torperfResultsBuilder.addTimestamp("DATAREQUEST", transfer.unixTsStart,
          transfer.elapsedSeconds.command);
      torperfResultsBuilder.addTimestamp("DATARESPONSE", transfer.unixTsStart,
          transfer.elapsedSeconds.response);
      if (null != transfer.elapsedSeconds.payloadBytes) {
        for (Map.Entry<String, Double> payloadBytesEntry
            : transfer.elapsedSeconds.payloadBytes.entrySet()) {
          String key = String.format("PARTIAL%s", payloadBytesEntry.getKey());
          Double elapsedSeconds = payloadBytesEntry.getValue();
          torperfResultsBuilder.addTimestamp(key, transfer.unixTsStart,
              elapsedSeconds);
        }
      }
      if (null != transfer.elapsedSeconds.payloadProgress) {
        for (Map.Entry<String, Double> payloadProgressEntry
            : transfer.elapsedSeconds.payloadProgress.entrySet()) {
          String key = String.format("DATAPERC%.0f",
              Double.parseDouble(payloadProgressEntry.getKey()) * 100.0);
          Double elapsedSeconds = payloadProgressEntry.getValue();
          torperfResultsBuilder.addTimestamp(key, transfer.unixTsStart,
              elapsedSeconds);
        }
      }
      torperfResultsBuilder.addTimestamp("DATACOMPLETE", transfer.unixTsStart,
          transfer.elapsedSeconds.lastByte);
      if (transfer.isError) {
        torperfResultsBuilder.addInteger("DIDTIMEOUT", 1);
        if ("PROXY".equals(transfer.errorCode)) {
          torperfResultsBuilder.addErrorCodePart("TOR");
        } else {
          torperfResultsBuilder.addErrorCodePart("TGEN");
          torperfResultsBuilder.addErrorCodePart(transfer.errorCode);
        }
      }
    }
  }

  /**
   * Format relevant tgen stream data as Torperf result key-value pairs.
   *
   * @param torperfResultsBuilder Torperf results builder to add key-value pairs
   *     to.
   * @param stream Stream data obtained from the parsed OnionPerf analysis file.
   */
  private void formatTgenStreamParts(
      TorperfResultsBuilder torperfResultsBuilder,
      ParsedOnionPerfAnalysis.TgenStream stream) {
    torperfResultsBuilder.addString("READBYTES", "0");
    torperfResultsBuilder.addString("WRITEBYTES", "0");
    if (null != stream.byteInfo) {
      torperfResultsBuilder.addString("READBYTES",
          stream.byteInfo.totalBytesRecv);
      torperfResultsBuilder.addString("WRITEBYTES",
          stream.byteInfo.totalBytesSend);
    }
    if (null != stream.streamInfo) {
      torperfResultsBuilder.addString("FILESIZE", stream.streamInfo.recvsize);
      torperfResultsBuilder.addString("HOSTNAMELOCAL", stream.streamInfo.name);
      torperfResultsBuilder.addString("HOSTNAMEREMOTE",
          stream.streamInfo.peername);
    }
    if (null != stream.transportInfo) {
      torperfResultsBuilder.addString("ENDPOINTLOCAL",
          stream.transportInfo.local);
      torperfResultsBuilder.addString("ENDPOINTPROXY",
          stream.transportInfo.proxy);
      torperfResultsBuilder.addString("ENDPOINTREMOTE",
          stream.transportInfo.remote);
    }
    torperfResultsBuilder.addInteger("DIDTIMEOUT", 0);

    for (String key : new String[] { "START", "SOCKET", "CONNECT", "NEGOTIATE",
        "REQUEST", "RESPONSE", "DATAREQUEST", "DATARESPONSE",
        "DATACOMPLETE" }) {
      torperfResultsBuilder.addString(key, "0.0");
    }
    torperfResultsBuilder.addTimestamp("START", stream.unixTsStart, 0.0);
    if (null != stream.unixTsStart) {
      if (null != stream.timeInfo) {
        torperfResultsBuilder.addTimestamp("SOCKET", stream.unixTsStart,
            stream.timeInfo.usecsToSocketCreate);
        torperfResultsBuilder.addTimestamp("CONNECT", stream.unixTsStart,
            stream.timeInfo.usecsToSocketConnect);
        torperfResultsBuilder.addTimestamp("NEGOTIATE", stream.unixTsStart,
            stream.timeInfo.usecsToProxyChoice);
        torperfResultsBuilder.addTimestamp("REQUEST", stream.unixTsStart,
            stream.timeInfo.usecsToProxyRequest);
        torperfResultsBuilder.addTimestamp("RESPONSE", stream.unixTsStart,
            stream.timeInfo.usecsToProxyResponse);
        torperfResultsBuilder.addTimestamp("DATAREQUEST", stream.unixTsStart,
            stream.timeInfo.usecsToCommand);
        torperfResultsBuilder.addTimestamp("DATARESPONSE", stream.unixTsStart,
            stream.timeInfo.usecsToResponse);
        torperfResultsBuilder.addTimestamp("DATACOMPLETE", stream.unixTsStart,
            stream.timeInfo.usecsToLastByteRecv);
      }
      if (null != stream.elapsedSeconds) {
        if (null != stream.elapsedSeconds.payloadBytesRecv) {
          for (Map.Entry<String, Double> payloadBytesRecvEntry
              : stream.elapsedSeconds.payloadBytesRecv.entrySet()) {
            String key = String.format("PARTIAL%s",
                payloadBytesRecvEntry.getKey());
            Double elapsedSeconds = payloadBytesRecvEntry.getValue();
            torperfResultsBuilder.addTimestamp(key, stream.unixTsStart,
                elapsedSeconds);
          }
        }
        if (null != stream.elapsedSeconds.payloadProgressRecv) {
          for (Map.Entry<String, Double> payloadProgressRecvEntry
              : stream.elapsedSeconds.payloadProgressRecv.entrySet()) {
            String key = String.format("DATAPERC%.0f",
                Double.parseDouble(payloadProgressRecvEntry.getKey()) * 100.0);
            Double elapsedSeconds = payloadProgressRecvEntry.getValue();
            torperfResultsBuilder.addTimestamp(key, stream.unixTsStart,
                elapsedSeconds);
          }
        }
      }
      if (null != stream.isError && stream.isError) {
        torperfResultsBuilder.addInteger("DIDTIMEOUT", 1);
        if ("PROXY".equals(stream.streamInfo.error)) {
          torperfResultsBuilder.addErrorCodePart("TOR");
        } else {
          torperfResultsBuilder.addErrorCodePart("TGEN");
          torperfResultsBuilder.addErrorCodePart(stream.streamInfo.error);
        }
      }
    }
  }

  /**
   * Format relevant stream data as Torperf result key-value pairs.
   *
   * @param torperfResultsBuilder Torperf results builder to add key-value pairs
   *     to.
   * @param stream Stream data obtained from the parsed OnionPerf analysis file.
   */
  private void formatStreamParts(TorperfResultsBuilder torperfResultsBuilder,
      ParsedOnionPerfAnalysis.Stream stream) {
    if (null != stream.failureReasonLocal) {
      torperfResultsBuilder.addErrorCodePart(stream.failureReasonLocal);
      if (null != stream.failureReasonRemote) {
        torperfResultsBuilder.addErrorCodePart(stream.failureReasonRemote);
      }
    }
    torperfResultsBuilder.addTimestamp("USED_AT", stream.unixTsEnd, 0.0);
    torperfResultsBuilder.addInteger("USED_BY", stream.streamId);
  }

  /**
   * Format relevant circuit data as Torperf result key-value pairs.
   *
   * @param torperfResultsBuilder Torperf results builder to add key-value pairs
   *     to.
   * @param circuit Circuit data obtained from the parsed OnionPerf analysis
   *     file.
   */
  private void formatCircuitParts(TorperfResultsBuilder torperfResultsBuilder,
      ParsedOnionPerfAnalysis.Circuit circuit) {
    torperfResultsBuilder.addTimestamp("LAUNCH", circuit.unixTsStart, 0.0);
    if (null != circuit.path) {
      List<String> path = new ArrayList<>();
      List<String> buildTimes = new ArrayList<>();
      for (Object[] pathElement : circuit.path) {
        String fingerprintAndNickname = (String) pathElement[0];
        String fingerprint = fingerprintAndNickname.split("~")[0];
        path.add(fingerprint);
        buildTimes.add(String.format("%.2f", (Double) pathElement[1]));
      }
      torperfResultsBuilder.addString("PATH", String.join(",", path));
      torperfResultsBuilder.addString("BUILDTIMES",
          String.join(",", buildTimes));
      torperfResultsBuilder.addInteger("TIMEOUT", circuit.buildTimeout);
      torperfResultsBuilder.addDouble("QUANTILE", circuit.buildQuantile);
      torperfResultsBuilder.addInteger("CIRC_ID", circuit.circuitId);
    }
  }
}

