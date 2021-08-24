/* Copyright 2012--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.Descriptor;
import org.torproject.descriptor.DescriptorParseException;
import org.torproject.descriptor.TorperfResult;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TorperfResultImpl extends DescriptorImpl
    implements TorperfResult {

  private static final long serialVersionUID = 8961567618137500044L;

  /**
   * Parse the given descriptor to one or more {@link TorperfResult} instances.
   *
   * @param rawDescriptorBytes Bytes to parse
   * @param descriptorFile Descriptor file containing the given bytes
   * @return Parsed {@link TorperfResult} instances
   * @throws DescriptorParseException Thrown if any of the lines cannot be
   *     parsed.
   */
  public static List<Descriptor> parseTorperfResults(
      byte[] rawDescriptorBytes, File descriptorFile)
      throws DescriptorParseException {
    if (rawDescriptorBytes.length == 0) {
      throw new DescriptorParseException("Descriptor is empty.");
    }
    List<Descriptor> parsedDescriptors = new ArrayList<>();
    String descriptorString = new String(rawDescriptorBytes,
        StandardCharsets.UTF_8);
    try (Scanner scanner =
                 new Scanner(descriptorString).useDelimiter("\r?\n")) {
      String typeAnnotation = "";
      while (scanner.hasNext()) {
        String line = scanner.next();
        if (line.startsWith("@type torperf ")) {
          String[] parts = line.split(" ");
          if (parts.length != 3) {
            throw new DescriptorParseException("Illegal line '" + line
                    + "'.");
          }
          String version = parts[2];
          if (!version.startsWith("1.")) {
            throw new DescriptorParseException("Unsupported version in "
                    + " line '" + line + "'.");
          }
          typeAnnotation = line + "\n";
        } else {
          /* XXX21932 */
          parsedDescriptors.add(new TorperfResultImpl(
                  (typeAnnotation + line + "\n")
                          .getBytes(StandardCharsets.UTF_8),
                  descriptorFile));
          typeAnnotation = "";
        }
      }
    }
    return parsedDescriptors;
  }

  protected TorperfResultImpl(byte[] rawDescriptorBytes, File descriptorFile)
      throws DescriptorParseException {
    super(rawDescriptorBytes, new int[] { 0, rawDescriptorBytes.length },
        descriptorFile, false);
    this.parseTorperfResultLine();
  }

  private void parseTorperfResultLine()
      throws DescriptorParseException {
    String line = this.newScanner().nextLine();
    while (null != line && line.startsWith("@") && line.contains("\n")) {
      line = line.split("\n")[1];
    }
    if (null == line || line.isEmpty()) {
      throw new DescriptorParseException("Blank lines are not allowed.");
    }
    String[] parts = line.split(" ");
    for (String keyValue : parts) {
      String[] keyValueParts = keyValue.split("=");
      if (keyValueParts.length != 2) {
        throw new DescriptorParseException("Illegal key-value pair in "
            + "line '" + line + "'.");
      }
      String key = keyValueParts[0];
      this.markKeyAsParsed(key, line);
      String value = keyValueParts[1];
      switch (key) {
        case "SOURCE":
          this.parseSource(value);
          break;
        case "FILESIZE":
          this.parseFileSize(value, keyValue, line);
          break;
        case "START":
          this.parseStart(value, keyValue, line);
          break;
        case "SOCKET":
          this.parseSocket(value, keyValue, line);
          break;
        case "CONNECT":
          this.parseConnect(value, keyValue, line);
          break;
        case "NEGOTIATE":
          this.parseNegotiate(value, keyValue, line);
          break;
        case "REQUEST":
          this.parseRequest(value, keyValue, line);
          break;
        case "RESPONSE":
          this.parseResponse(value, keyValue, line);
          break;
        case "DATAREQUEST":
          this.parseDataRequest(value, keyValue, line);
          break;
        case "DATARESPONSE":
          this.parseDataResponse(value, keyValue, line);
          break;
        case "DATACOMPLETE":
          this.parseDataComplete(value, keyValue, line);
          break;
        case "WRITEBYTES":
          this.parseWriteBytes(value, keyValue, line);
          break;
        case "READBYTES":
          this.parseReadBytes(value, keyValue, line);
          break;
        case "DIDTIMEOUT":
          this.parseDidTimeout(value, keyValue, line);
          break;
        case "LAUNCH":
          this.parseLaunch(value, keyValue, line);
          break;
        case "USED_AT":
          this.parseUsedAt(value, keyValue, line);
          break;
        case "PATH":
          this.parsePath(value, keyValue, line);
          break;
        case "BUILDTIMES":
          this.parseBuildTimes(value, keyValue, line);
          break;
        case "TIMEOUT":
          this.parseTimeout(value, keyValue, line);
          break;
        case "QUANTILE":
          this.parseQuantile(value, keyValue, line);
          break;
        case "CIRC_ID":
          this.parseCircId(value, keyValue, line);
          break;
        case "USED_BY":
          this.parseUsedBy(value, keyValue, line);
          break;
        case "ENDPOINTLOCAL":
          this.parseEndpointLocal(value);
          break;
        case "ENDPOINTPROXY":
          this.parseEndpointProxy(value);
          break;
        case "ENDPOINTREMOTE":
          this.parseEndpointRemote(value);
          break;
        case "HOSTNAMELOCAL":
          this.parseHostnameLocal(value);
          break;
        case "HOSTNAMEREMOTE":
          this.parseHostnameRemote(value);
          break;
        case "SOURCEADDRESS":
          this.parseSourceAddress(value);
          break;
        case "ERRORCODE":
          this.parseErrorcode(value);
          break;
        default:
          if (key.startsWith("DATAPERC")) {
            this.parseDataPercentile(value, keyValue, line);
          } else if (key.startsWith("PARTIAL")) {
            this.parsePartial(value, keyValue, line);
          } else {
            if (this.unrecognizedKeys == null) {
              this.unrecognizedKeys = new TreeMap<>();
            }
            this.unrecognizedKeys.put(key, value);
            if (this.unrecognizedLines == null) {
              this.unrecognizedLines = new ArrayList<>();
            }
            if (!this.unrecognizedLines.contains(line)) {
              this.unrecognizedLines.add(line);
            }
          }
      }
    }
    this.checkAllRequiredKeysParsed(line);
  }

  private Set<String> parsedKeys = new HashSet<>();

  private Set<String> requiredKeys = new HashSet<>(Arrays.asList(
      ("SOURCE,FILESIZE,START,SOCKET,CONNECT,NEGOTIATE,REQUEST,RESPONSE,"
      + "DATAREQUEST,DATARESPONSE,DATACOMPLETE,WRITEBYTES,READBYTES")
      .split(",")));

  private void markKeyAsParsed(String key, String line)
      throws DescriptorParseException {
    if (this.parsedKeys.contains(key)) {
      throw new DescriptorParseException("Key '" + key + "' is contained "
          + "at least twice in line '" + line + "', but must be "
          + "contained at most once.");
    }
    this.parsedKeys.add(key);
    this.requiredKeys.remove(key);
  }

  private void checkAllRequiredKeysParsed(String line)
      throws DescriptorParseException {
    for (String key : this.requiredKeys) {
      throw new DescriptorParseException("Key '" + key + "' is contained "
          + "contained 0 times in line '" + line + "', but must be "
          + "contained exactly once.");
    }
  }

  private void parseSource(String value) {
    this.source = value;
  }

  private void parseFileSize(String value, String keyValue, String line)
      throws DescriptorParseException {
    try {
      this.fileSize = Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new DescriptorParseException("Illegal value in '" + keyValue
          + "' in line '" + line + "'.");
    }
  }

  private void parseStart(String value, String keyValue, String line)
      throws DescriptorParseException {
    this.startMillis = this.parseTimestamp(value, keyValue, line);
  }

  private void parseSocket(String value, String keyValue, String line)
      throws DescriptorParseException {
    this.socketMillis = this.parseTimestamp(value, keyValue, line);
  }

  private void parseConnect(String value, String keyValue, String line)
      throws DescriptorParseException {
    this.connectMillis = this.parseTimestamp(value, keyValue, line);
  }

  private void parseNegotiate(String value, String keyValue, String line)
      throws DescriptorParseException {
    this.negotiateMillis = this.parseTimestamp(value, keyValue, line);
  }

  private void parseRequest(String value, String keyValue, String line)
      throws DescriptorParseException {
    this.requestMillis = this.parseTimestamp(value, keyValue, line);
  }

  private void parseResponse(String value, String keyValue, String line)
      throws DescriptorParseException {
    this.responseMillis = this.parseTimestamp(value, keyValue, line);
  }

  private void parseDataRequest(String value, String keyValue,
      String line) throws DescriptorParseException {
    this.dataRequestMillis = this.parseTimestamp(value, keyValue, line);
  }

  private void parseDataResponse(String value, String keyValue,
      String line) throws DescriptorParseException {
    this.dataResponseMillis = this.parseTimestamp(value, keyValue, line);
  }

  private void parseDataComplete(String value, String keyValue,
      String line) throws DescriptorParseException {
    this.dataCompleteMillis = this.parseTimestamp(value, keyValue, line);
  }

  private void parseWriteBytes(String value, String keyValue, String line)
      throws DescriptorParseException {
    this.writeBytes = parseInt(value, keyValue, line);
  }

  private void parseReadBytes(String value, String keyValue, String line)
      throws DescriptorParseException {
    this.readBytes = parseInt(value, keyValue, line);
  }

  private void parseDidTimeout(String value, String keyValue, String line)
      throws DescriptorParseException {
    switch (value) {
      case "1":
        this.didTimeout = true;
        break;
      case "0":
        this.didTimeout = false;
        break;
      default:
        throw new DescriptorParseException("Illegal value in '" + keyValue
            + "' in line '" + line + "'.");
    }
  }

  private void parsePartial(String value, String keyValue, String line)
      throws DescriptorParseException {
    String key = keyValue.substring(0, keyValue.indexOf("="));
    String bytesString = key.substring("PARTIAL".length());
    int bytes;
    try {
      bytes = Integer.parseInt(bytesString);
    } catch (NumberFormatException e) {
      /* Treat key as unrecognized below. */
      bytes = -1;
    }
    if (bytes < 0) {
      if (this.unrecognizedKeys == null) {
        this.unrecognizedKeys = new TreeMap<>();
      }
      this.unrecognizedKeys.put(key, value);
    } else {
      long timestamp = this.parseTimestamp(value, keyValue, line);
      if (this.partials == null) {
        this.partials = new TreeMap<>();
      }
      this.partials.put(bytes, timestamp);
    }
  }

  private void parseDataPercentile(String value, String keyValue,
      String line) throws DescriptorParseException {
    String key = keyValue.substring(0, keyValue.indexOf("="));
    String percentileString = key.substring("DATAPERC".length());
    int percentile;
    try {
      percentile = Integer.parseInt(percentileString);
    } catch (NumberFormatException e) {
      /* Treat key as unrecognized below. */
      percentile = -1;
    }
    if (percentile < 0 || percentile > 100) {
      if (this.unrecognizedKeys == null) {
        this.unrecognizedKeys = new TreeMap<>();
      }
      this.unrecognizedKeys.put(key, value);
    } else {
      long timestamp = this.parseTimestamp(value, keyValue, line);
      if (this.dataPercentiles == null) {
        this.dataPercentiles = new TreeMap<>();
      }
      this.dataPercentiles.put(percentile, timestamp);
    }
  }

  private void parseLaunch(String value, String keyValue, String line)
      throws DescriptorParseException {
    this.launchMillis = this.parseTimestamp(value, keyValue, line);
  }

  private void parseUsedAt(String value, String keyValue, String line)
      throws DescriptorParseException {
    this.usedAtMillis = this.parseTimestamp(value, keyValue, line);
  }

  private void parsePath(String value, String keyValue, String line)
      throws DescriptorParseException {
    String[] valueParts = value.split(",");
    String[] result = new String[valueParts.length];
    for (int i = 0; i < valueParts.length; i++) {
      if (valueParts[i].length() != 41) {
        throw new DescriptorParseException("Illegal value in '" + keyValue
            + "' in line '" + line + "'.");
      }
      result[i] = ParseHelper.parseTwentyByteHexString(line,
          valueParts[i].substring(1));
    }
    this.path = result;
  }

  private void parseBuildTimes(String value, String keyValue, String line)
      throws DescriptorParseException {
    String[] valueParts = value.split(",");
    Long[] result = new Long[valueParts.length];
    for (int i = 0; i < valueParts.length; i++) {
      result[i] = this.parseTimestamp(valueParts[i], keyValue, line);
    }
    this.buildTimes = result;
  }

  private void parseTimeout(String value, String keyValue, String line)
      throws DescriptorParseException {
    this.timeout = this.parseInt(value, keyValue, line);
  }

  private void parseQuantile(String value, String keyValue, String line)
      throws DescriptorParseException {
    this.quantile = this.parseDouble(value, keyValue, line);
  }

  private void parseCircId(String value, String keyValue, String line)
      throws DescriptorParseException {
    this.circId = this.parseInt(value, keyValue, line);
  }

  private void parseUsedBy(String value, String keyValue, String line)
      throws DescriptorParseException {
    this.usedBy = this.parseInt(value, keyValue, line);
  }

  private void parseEndpointLocal(String value) {
    this.endpointLocal = value;
  }

  private void parseEndpointProxy(String value) {
    this.endpointProxy = value;
  }

  private void parseEndpointRemote(String value) {
    this.endpointRemote = value;
  }

  private void parseHostnameLocal(String value) {
    this.hostnameLocal = value;
  }

  private void parseHostnameRemote(String value) {
    this.hostnameRemote = value;
  }

  private void parseSourceAddress(String value) {
    this.sourceAddress = value;
  }

  private void parseErrorcode(String value) {
    this.errorCode = value;
  }

  private long parseTimestamp(String value, String keyValue, String line)
      throws DescriptorParseException {
    long timestamp = -1L;
    if (value.contains(".") && value.split("\\.").length == 2) {
      String zeroPaddedValue = (value + "000");
      String threeDecimalPlaces = zeroPaddedValue.substring(0,
          zeroPaddedValue.indexOf(".") + 4);
      String millisString = threeDecimalPlaces.replaceAll("\\.", "");
      try {
        timestamp = Long.parseLong(millisString);
      } catch (NumberFormatException e) {
        /* Handle below. */
      }
    }
    if (timestamp < 0L) {
      throw new DescriptorParseException("Illegal timestamp '" + value
          + "' in '" + keyValue + "' in line '" + line + "'.");
    }
    return timestamp;
  }

  private int parseInt(String value, String keyValue, String line)
      throws DescriptorParseException {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new DescriptorParseException("Illegal value in '" + keyValue
          + "' in line '" + line + "'.");
    }
  }

  private double parseDouble(String value, String keyValue, String line)
      throws DescriptorParseException {
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      throw new DescriptorParseException("Illegal value in '" + keyValue
          + "' in line '" + line + "'.");
    }
  }

  private SortedMap<String, String> unrecognizedKeys;

  @Override
  public SortedMap<String, String> getUnrecognizedKeys() {
    return this.unrecognizedKeys == null ? null
        : new TreeMap<>(this.unrecognizedKeys);
  }

  private String source;

  @Override
  public String getSource() {
    return this.source;
  }

  private int fileSize;

  @Override
  public int getFileSize() {
    return this.fileSize;
  }

  private long startMillis;

  @Override
  public long getStartMillis() {
    return this.startMillis;
  }

  private long socketMillis;

  @Override
  public long getSocketMillis() {
    return this.socketMillis;
  }

  private long connectMillis;

  @Override
  public long getConnectMillis() {
    return this.connectMillis;
  }

  private long negotiateMillis;

  @Override
  public long getNegotiateMillis() {
    return this.negotiateMillis;
  }

  private long requestMillis;

  @Override
  public long getRequestMillis() {
    return this.requestMillis;
  }

  private long responseMillis;

  @Override
  public long getResponseMillis() {
    return this.responseMillis;
  }

  private long dataRequestMillis;

  @Override
  public long getDataRequestMillis() {
    return this.dataRequestMillis;
  }

  private long dataResponseMillis;

  @Override
  public long getDataResponseMillis() {
    return this.dataResponseMillis;
  }

  private long dataCompleteMillis;

  @Override
  public long getDataCompleteMillis() {
    return this.dataCompleteMillis;
  }

  private int writeBytes;

  @Override
  public int getWriteBytes() {
    return this.writeBytes;
  }

  private int readBytes;

  @Override
  public int getReadBytes() {
    return this.readBytes;
  }

  private boolean didTimeout;

  @Override
  public Boolean didTimeout() {
    return this.didTimeout;
  }

  private SortedMap<Integer, Long> partials;

  @Override
  public SortedMap<Integer, Long> getPartials() {
    return this.partials == null ? null : new TreeMap<>(this.partials);
  }

  private SortedMap<Integer, Long> dataPercentiles;

  @Override
  public SortedMap<Integer, Long> getDataPercentiles() {
    return this.dataPercentiles == null ? null
        : new TreeMap<>(this.dataPercentiles);
  }

  private long launchMillis = -1L;

  @Override
  public long getLaunchMillis() {
    return this.launchMillis;
  }

  private long usedAtMillis = -1L;

  @Override
  public long getUsedAtMillis() {
    return this.usedAtMillis;
  }

  private String[] path;

  @Override
  public List<String> getPath() {
    return this.path == null ? null : Arrays.asList(this.path);
  }

  private Long[] buildTimes;

  @Override
  public List<Long> getBuildTimes() {
    return this.buildTimes == null ? null
        : Arrays.asList(this.buildTimes);
  }

  private long timeout = -1L;

  @Override
  public long getTimeout() {
    return this.timeout;
  }

  private double quantile = -1.0;

  @Override
  public double getQuantile() {
    return this.quantile;
  }

  private int circId = -1;

  @Override
  public int getCircId() {
    return this.circId;
  }

  private int usedBy = -1;

  @Override
  public int getUsedBy() {
    return this.usedBy;
  }

  private String endpointLocal;

  @Override
  public String getEndpointLocal() {
    return this.endpointLocal;
  }

  private String endpointProxy;

  @Override
  public String getEndpointProxy() {
    return this.endpointProxy;
  }

  private String endpointRemote;

  @Override
  public String getEndpointRemote() {
    return this.endpointRemote;
  }

  private String hostnameLocal;

  @Override
  public String getHostnameLocal() {
    return this.hostnameLocal;
  }

  private String hostnameRemote;

  @Override
  public String getHostnameRemote() {
    return this.hostnameRemote;
  }

  private String sourceAddress;

  @Override
  public String getSourceAddress() {
    return this.sourceAddress;
  }

  private String errorCode;

  @Override
  public String getErrorCode() {
    return this.errorCode;
  }
}

