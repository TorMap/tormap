/* Copyright 2019--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.BridgedbMetrics;
import org.torproject.descriptor.DescriptorParseException;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class BridgedbMetricsImpl extends DescriptorImpl
    implements BridgedbMetrics {

  private static final long serialVersionUID = 3899169611574577173L;

  private static final Set<Key> exactlyOnce = EnumSet.of(
      Key.BRIDGEDB_METRICS_END, Key.BRIDGEDB_METRICS_VERSION);

  BridgedbMetricsImpl(byte[] rawDescriptorBytes, int[] offsetAndLength,
      File descriptorFile) throws DescriptorParseException {
    super(rawDescriptorBytes, offsetAndLength, descriptorFile, false);
    this.parseDescriptorBytes();
    this.checkExactlyOnceKeys(exactlyOnce);
    this.checkFirstKey(Key.BRIDGEDB_METRICS_END);
    this.clearParsedKeys();
  }

  BridgedbMetricsImpl(byte[] rawDescriptorBytes, File descriptorFile)
      throws DescriptorParseException {
    this(rawDescriptorBytes, new int[] { 0, rawDescriptorBytes.length },
        descriptorFile);
  }

  private void parseDescriptorBytes() throws DescriptorParseException {
    Scanner scanner = this.newScanner().useDelimiter(NL);
    while (scanner.hasNext()) {
      String line = scanner.next();
      if (line.startsWith("@")) {
        continue;
      }
      String[] parts = line.split("[ \t]+");
      Key key = Key.get(parts[0]);
      switch (key) {
        case BRIDGEDB_METRICS_END:
          this.parseBridgedbMetricsEnd(line, parts);
          break;
        case BRIDGEDB_METRICS_VERSION:
          this.parseBridgedbMetricsVersion(line, parts);
          break;
        case BRIDGEDB_METRIC_COUNT:
          this.parseBridgedbMetricCount(line, parts);
          break;
        case INVALID:
        default:
          ParseHelper.parseKeyword(line, parts[0]);
          if (this.unrecognizedLines == null) {
            this.unrecognizedLines = new ArrayList<>();
          }
          this.unrecognizedLines.add(line);
      }
    }
  }

  private void parseBridgedbMetricsEnd(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length < 5 || parts[3].length() < 2 || !parts[3].startsWith("(")
        || !parts[4].equals("s)")) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    this.bridgedbMetricsEnd = ParseHelper.parseLocalDateTime(line, parts,
        1, 2);
    this.bridgedbMetricsIntervalLength = ParseHelper.parseDuration(line,
        parts[3].substring(1));
  }

  private void parseBridgedbMetricsVersion(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length < 2) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    this.bridgedbMetricsVersion = parts[1];
  }

  private void parseBridgedbMetricCount(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length < 3) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    if (null == this.bridgedbMetricCounts) {
      this.bridgedbMetricCounts = new LinkedHashMap<>();
    }
    String key = parts[1];
    if (this.bridgedbMetricCounts.containsKey(key)) {
      throw new DescriptorParseException("Duplicate key '" + key + "' in line '"
          + line + "'.");
    }
    long value = ParseHelper.parseLong(line, parts, 2);
    this.bridgedbMetricCounts.put(key, value);
  }

  private LocalDateTime bridgedbMetricsEnd;

  @Override
  public LocalDateTime bridgedbMetricsEnd() {
    return this.bridgedbMetricsEnd;
  }

  private Duration bridgedbMetricsIntervalLength;

  @Override
  public Duration bridgedbMetricsIntervalLength() {
    return this.bridgedbMetricsIntervalLength;
  }

  private String bridgedbMetricsVersion;

  @Override
  public String bridgedbMetricsVersion() {
    return this.bridgedbMetricsVersion;
  }

  private Map<String, Long> bridgedbMetricCounts;

  @Override
  public Optional<Map<String, Long>> bridgedbMetricCounts() {
    return Optional.ofNullable(this.bridgedbMetricCounts);
  }
}

