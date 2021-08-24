/* Copyright 2019--2020 The Tor Project
 * Copyright 2021 SR2 Communications Limited
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.BridgestrapStats;
import org.torproject.descriptor.BridgestrapTestResult;
import org.torproject.descriptor.DescriptorParseException;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class BridgestrapStatsImpl extends DescriptorImpl
    implements BridgestrapStats {

  private static final Set<Key> exactlyOnce = EnumSet.of(
      Key.BRIDGESTRAP_STATS_END);

  private static final Set<Key> atMostOnce = EnumSet.of(
      Key.BRIDGESTRAP_CACHED_REQUESTS);

  BridgestrapStatsImpl(byte[] rawDescriptorBytes, int[] offsetAndLength,
                       File descriptorFile) throws DescriptorParseException {
    super(rawDescriptorBytes, offsetAndLength, descriptorFile, false);
    this.parseDescriptorBytes();
    this.checkExactlyOnceKeys(exactlyOnce);
    this.checkFirstKey(Key.BRIDGESTRAP_STATS_END);
    this.checkAtMostOnceKeys(atMostOnce);
    this.clearParsedKeys();
  }

  BridgestrapStatsImpl(byte[] rawDescriptorBytes, File descriptorFile)
      throws DescriptorParseException {
    this(rawDescriptorBytes, new int[]{0, rawDescriptorBytes.length},
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
        case BRIDGESTRAP_STATS_END:
          this.parseBridgestrapStatsEnd(line, parts);
          break;
        case BRIDGESTRAP_CACHED_REQUESTS:
          this.parseBridgestrapCachedRequests(line, parts);
          break;
        case BRIDGESTRAP_TEST:
          this.parseBridgestrapTest(line, parts);
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

  private void parseBridgestrapStatsEnd(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length < 5 || parts[3].length() < 2 || !parts[3].startsWith("(")
        || !parts[4].equals("s)")) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    this.bridgestrapStatsEnd = ParseHelper.parseLocalDateTime(line, parts,
        1, 2);
    this.bridgestrapStatsIntervalLength = ParseHelper.parseDuration(line,
        parts[3].substring(1));
  }

  private void parseBridgestrapCachedRequests(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length < 2) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    try {
      this.bridgestrapCachedRequests = Integer.parseInt(parts[1]);
    } catch (NumberFormatException e) {
      throw new DescriptorParseException("Illegal number format '"
          + line + "'.");
    }
  }

  private void parseBridgestrapTest(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length < 2) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    if (null == this.bridgestrapTests) {
      this.bridgestrapTests = new ArrayList<>();
    }
    boolean isReachable = Boolean.parseBoolean(parts[1]);
    String fingerprint = null;
    if (parts.length >= 3) {
      fingerprint = parts[2];
    }
    this.bridgestrapTests.add(
        new BridgestrapTestResultImpl(isReachable,
            fingerprint));
  }

  private LocalDateTime bridgestrapStatsEnd;

  @Override
  public LocalDateTime bridgestrapStatsEnd() {
    return this.bridgestrapStatsEnd;
  }

  private Duration bridgestrapStatsIntervalLength;

  @Override
  public Duration bridgestrapStatsIntervalLength() {
    return this.bridgestrapStatsIntervalLength;
  }

  private int bridgestrapCachedRequests;

  @Override
  public int bridgestrapCachedRequests() {
    return this.bridgestrapCachedRequests;
  }

  private List<BridgestrapTestResult> bridgestrapTests;

  @Override
  public Optional<List<BridgestrapTestResult>> bridgestrapTests() {
    return Optional.ofNullable(this.bridgestrapTests);
  }
}
