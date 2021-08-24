/* Copyright 2019--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.DescriptorParseException;
import org.torproject.descriptor.SnowflakeStats;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

public class SnowflakeStatsImpl extends DescriptorImpl
    implements SnowflakeStats {

  private static final long serialVersionUID = 5588809239715099933L;

  private static final Set<Key> atMostOnce = EnumSet.of(
      Key.SNOWFLAKE_IPS, Key.SNOWFLAKE_IPS_TOTAL, Key.SNOWFLAKE_IPS_STANDALONE,
      Key.SNOWFLAKE_IPS_BADGE, Key.SNOWFLAKE_IPS_WEBEXT,
      Key.SNOWFLAKE_IDLE_COUNT, Key.CLIENT_DENIED_COUNT,
      Key.CLIENT_SNOWFLAKE_MATCH_COUNT);

  private static final Set<Key> exactlyOnce = EnumSet.of(
      Key.SNOWFLAKE_STATS_END);

  SnowflakeStatsImpl(byte[] rawDescriptorBytes, int[] offsetAndLength,
      File descriptorFile) throws DescriptorParseException {
    super(rawDescriptorBytes, offsetAndLength, descriptorFile, false);
    this.parseDescriptorBytes();
    this.checkExactlyOnceKeys(exactlyOnce);
    this.checkAtMostOnceKeys(atMostOnce);
    this.checkFirstKey(Key.SNOWFLAKE_STATS_END);
    this.clearParsedKeys();
  }

  SnowflakeStatsImpl(byte[] rawDescriptorBytes, File descriptorFile)
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
        case SNOWFLAKE_STATS_END:
          this.parseSnowflakeStatsEnd(line, parts);
          break;
        case SNOWFLAKE_IPS:
          this.parseSnowflakeIps(line, parts);
          break;
        case SNOWFLAKE_IPS_TOTAL:
          this.parseSnowflakeIpsTotal(line, parts);
          break;
        case SNOWFLAKE_IPS_STANDALONE:
          this.parseSnowflakeIpsStandalone(line, parts);
          break;
        case SNOWFLAKE_IPS_BADGE:
          this.parseSnowflakeIpsBadge(line, parts);
          break;
        case SNOWFLAKE_IPS_WEBEXT:
          this.parseSnowflakeIpsWebext(line, parts);
          break;
        case SNOWFLAKE_IDLE_COUNT:
          this.parseSnowflakeIdleCount(line, parts);
          break;
        case CLIENT_DENIED_COUNT:
          this.parseClientDeniedCount(line, parts);
          break;
        case CLIENT_RESTRICTED_DENIED_COUNT:
          this.parseClientRestrictedDeniedCount(line, parts);
          break;
        case CLIENT_UNRESTRICTED_DENIED_COUNT:
          this.parseClientUnrestrictedDeniedCount(line, parts);
          break;
        case CLIENT_SNOWFLAKE_MATCH_COUNT:
          this.parseClientSnowflakeMatchCount(line, parts);
          break;
        case SNOWFLAKE_IPS_NAT_RESTRICTED:
          this.parseSnowflakeIpsNatRestricted(line, parts);
          break;
        case SNOWFLAKE_IPS_NAT_UNRESTRICTED:
          this.parseSnowflakeIpsNatUnrestricted(line, parts);
          break;
        case SNOWFLAKE_IPS_NAT_UNKNOWN:
          this.parseSnowflakeIpsNatUnknown(line, parts);
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

  private void parseSnowflakeStatsEnd(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length < 5 || parts[3].length() < 2 || !parts[3].startsWith("(")
        || !parts[4].equals("s)")) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    this.snowflakeStatsEnd = ParseHelper.parseLocalDateTime(line, parts,
        1, 2);
    this.snowflakeStatsIntervalLength = ParseHelper.parseDuration(line,
        parts[3].substring(1));
  }

  private void parseSnowflakeIps(String line, String[] parts)
      throws DescriptorParseException {
    this.snowflakeIps = ParseHelper.parseCommaSeparatedKeyLongValueList(line,
        parts, 1, 2);
  }

  private void parseSnowflakeIpsTotal(String line, String[] parts)
      throws DescriptorParseException {
    this.snowflakeIpsTotal = ParseHelper.parseLong(line, parts, 1);
  }

  private void parseSnowflakeIpsStandalone(String line, String[] parts)
      throws DescriptorParseException {
    this.snowflakeIpsStandalone = ParseHelper.parseLong(line, parts, 1);
  }

  private void parseSnowflakeIpsBadge(String line, String[] parts)
      throws DescriptorParseException {
    this.snowflakeIpsBadge = ParseHelper.parseLong(line, parts, 1);
  }

  private void parseSnowflakeIpsWebext(String line, String[] parts)
      throws DescriptorParseException {
    this.snowflakeIpsWebext = ParseHelper.parseLong(line, parts, 1);
  }

  private void parseSnowflakeIdleCount(String line, String[] parts)
      throws DescriptorParseException {
    this.snowflakeIdleCount = ParseHelper.parseLong(line, parts, 1);
  }

  private void parseClientDeniedCount(String line, String[] parts)
      throws DescriptorParseException {
    this.clientDeniedCount = ParseHelper.parseLong(line, parts, 1);
  }

  private void parseClientRestrictedDeniedCount(String line, String[] parts)
      throws DescriptorParseException {
    this.clientRestrictedDeniedCount = ParseHelper.parseLong(line, parts, 1);
  }

  private void parseClientUnrestrictedDeniedCount(String line, String[] parts)
      throws DescriptorParseException {
    this.clientUnrestrictedDeniedCount = ParseHelper.parseLong(line, parts, 1);
  }

  private void parseClientSnowflakeMatchCount(String line, String[] parts)
      throws DescriptorParseException {
    this.clientSnowflakeMatchCount = ParseHelper.parseLong(line, parts, 1);
  }

  private void parseSnowflakeIpsNatRestricted(String line, String[] parts)
      throws DescriptorParseException {
    this.snowflakeIpsNatRestricted = ParseHelper.parseLong(line, parts, 1);
  }

  private void parseSnowflakeIpsNatUnrestricted(String line, String[] parts)
      throws DescriptorParseException {
    this.snowflakeIpsNatUnrestricted = ParseHelper.parseLong(line, parts, 1);
  }

  private void parseSnowflakeIpsNatUnknown(String line, String[] parts)
      throws DescriptorParseException {
    this.snowflakeIpsNatUnknown = ParseHelper.parseLong(line, parts, 1);
  }

  private LocalDateTime snowflakeStatsEnd;

  @Override
  public LocalDateTime snowflakeStatsEnd() {
    return this.snowflakeStatsEnd;
  }

  private Duration snowflakeStatsIntervalLength;

  @Override
  public Duration snowflakeStatsIntervalLength() {
    return this.snowflakeStatsIntervalLength;
  }

  private SortedMap<String, Long> snowflakeIps;

  @Override
  public Optional<SortedMap<String, Long>> snowflakeIps() {
    return Optional.ofNullable(this.snowflakeIps);
  }

  private Long snowflakeIpsTotal;

  @Override
  public Optional<Long> snowflakeIpsTotal() {
    return Optional.ofNullable(this.snowflakeIpsTotal);
  }

  private Long snowflakeIpsStandalone;

  @Override
  public Optional<Long> snowflakeIpsStandalone() {
    return Optional.ofNullable(this.snowflakeIpsStandalone);
  }

  private Long snowflakeIpsBadge;

  @Override
  public Optional<Long> snowflakeIpsBadge() {
    return Optional.ofNullable(this.snowflakeIpsBadge);
  }

  private Long snowflakeIpsWebext;

  @Override
  public Optional<Long> snowflakeIpsWebext() {
    return Optional.ofNullable(this.snowflakeIpsWebext);
  }

  private Long snowflakeIdleCount;

  @Override
  public Optional<Long> snowflakeIdleCount() {
    return Optional.ofNullable(this.snowflakeIdleCount);
  }

  private Long clientDeniedCount;

  @Override
  public Optional<Long> clientDeniedCount() {
    return Optional.ofNullable(this.clientDeniedCount);
  }

  private Long clientRestrictedDeniedCount;

  @Override
  public Optional<Long> clientRestrictedDeniedCount() {
    return Optional.ofNullable(this.clientRestrictedDeniedCount);
  }

  private Long clientUnrestrictedDeniedCount;

  @Override
  public Optional<Long> clientUnrestrictedDeniedCount() {
    return Optional.ofNullable(this.clientUnrestrictedDeniedCount);
  }

  private Long clientSnowflakeMatchCount;

  @Override
  public Optional<Long> clientSnowflakeMatchCount() {
    return Optional.ofNullable(this.clientSnowflakeMatchCount);
  }

  private Long snowflakeIpsNatRestricted;

  @Override
  public Optional<Long> snowflakeIpsNatRestricted() {
    return Optional.ofNullable(this.snowflakeIpsNatRestricted);
  }

  private Long snowflakeIpsNatUnrestricted;

  @Override
  public Optional<Long> snowflakeIpsNatUnrestricted() {
    return Optional.ofNullable(this.snowflakeIpsNatUnrestricted);
  }

  private Long snowflakeIpsNatUnknown;

  @Override
  public Optional<Long> snowflakeIpsNatUnknown() {
    return Optional.ofNullable(this.snowflakeIpsNatUnknown);
  }
}

