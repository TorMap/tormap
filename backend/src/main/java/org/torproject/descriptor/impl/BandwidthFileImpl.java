/* Copyright 2019--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.BandwidthFile;
import org.torproject.descriptor.DescriptorParseException;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.*;

public class BandwidthFileImpl extends DescriptorImpl implements BandwidthFile {

  private enum KeyWithStringValue {
    version, software, software_version
  }

  private enum KeyWithLocalDateTimeValue {
    file_created, generator_started, earliest_bandwidth, latest_bandwidth
  }

  private enum KeyWithIntValue {
    number_eligible_relays, minimum_percent_eligible_relays,
    number_consensus_relays, percent_eligible_relays,
    minimum_number_eligible_relays, recent_consensus_count,
    recent_priority_list_count, recent_priority_relay_count,
    recent_measurement_attempt_count, recent_measurement_failure_count,
    recent_measurements_excluded_error_count,
    recent_measurements_excluded_near_count,
    recent_measurements_excluded_old_count,
    recent_measurements_excluded_few_count
  }

  BandwidthFileImpl(byte[] rawDescriptorBytes, File descriptorfile)
      throws DescriptorParseException {
    super(rawDescriptorBytes, new int[] { 0, rawDescriptorBytes.length },
        descriptorfile, false);
    Scanner scanner = this.newScanner().useDelimiter("\n");
    this.parseTimestampLine(scanner.nextLine());
    boolean haveFinishedParsingHeader = false;
    while (scanner.hasNext()) {
      String line = scanner.nextLine();
      if (!haveFinishedParsingHeader) {
        if (line.startsWith("bw=") || line.contains(" bw=")) {
          haveFinishedParsingHeader = true;
        } else if ("====".equals(line) || "=====".equals(line)) {
          haveFinishedParsingHeader = true;
          continue;
        }
      }
      if (!haveFinishedParsingHeader) {
        this.parseHeaderLine(line);
      } else {
        this.parseRelayLine(line);
      }
    }
    this.calculateDigestSha256Base64();
  }

  private void parseTimestampLine(String line) throws DescriptorParseException {
    try {
      this.timestamp = LocalDateTime.ofInstant(Instant.ofEpochSecond(
          Long.parseLong(line)), ZoneOffset.UTC);
    } catch (NumberFormatException | DateTimeParseException e) {
      throw new DescriptorParseException(String.format(
          "Unable to parse timestamp in first line: '%s'.", line), e);
    }
  }

  private void parseHeaderLine(String line) throws DescriptorParseException {
    String[] keyValueParts = line.split("=", 2);
    if (keyValueParts.length != 2) {
      throw new DescriptorParseException(String.format(
          "Unrecognized line '%s' without '=' character.", line));
    }
    String key = keyValueParts[0];
    if (key.length() < 1) {
      throw new DescriptorParseException(String.format(
          "Unrecognized line '%s' starting with '=' character.", line));
    }
    String value = keyValueParts[1];
    switch (key) {
      case "version":
      case "software":
      case "software_version":
        this.parsedStrings.put(KeyWithStringValue.valueOf(key), value);
        break;
      case "file_created":
      case "generator_started":
      case "earliest_bandwidth":
      case "latest_bandwidth":
        try {
          this.parsedLocalDateTimes.put(KeyWithLocalDateTimeValue.valueOf(key),
              LocalDateTime.parse(value));
        } catch (DateTimeParseException e) {
          throw new DescriptorParseException(String.format(
              "Unable to parse date-time string: '%s'.", value), e);
        }
        break;
      case "number_eligible_relays":
      case "minimum_percent_eligible_relays":
      case "number_consensus_relays":
      case "percent_eligible_relays":
      case "minimum_number_eligible_relays":
      case "recent_consensus_count":
      case "recent_priority_list_count":
      case "recent_priority_relay_count":
      case "recent_measurement_attempt_count":
      case "recent_measurement_failure_count":
      case "recent_measurements_excluded_error_count":
      case "recent_measurements_excluded_near_count":
      case "recent_measurements_excluded_old_count":
      case "recent_measurements_excluded_few_count":
        try {
          this.parsedInts.put(KeyWithIntValue.valueOf(key),
              Integer.parseInt(value));
        } catch (NumberFormatException e) {
          throw new DescriptorParseException(String.format(
              "Unable to parse int: '%s'.", value), e);
        }
        break;
      case "scanner_country":
        if (!value.matches("[A-Z]{2}")) {
          throw new DescriptorParseException(String.format(
              "Invalid country code '%s'.", value));
        }
        this.scannerCountry = value;
        break;
      case "destinations_countries":
        if (!value.matches("[A-Z]{2}(,[A-Z]{2})*")) {
          throw new DescriptorParseException(String.format(
              "Invalid country code list '%s'.", value));
        }
        this.destinationsCountries = value.split(",");
        break;
      case "time_to_report_half_network":
        try {
          this.timeToReportHalfNetwork
              = Duration.ofSeconds(Long.parseLong(value));
        } catch (NumberFormatException | DateTimeParseException e) {
          throw new DescriptorParseException(String.format(
              "Unable to parse duration: '%s'.", value), e);
        }
        break;
      case "node_id":
      case "master_key_ed25519":
      case "bw":
        throw new DescriptorParseException(String.format(
            "Either additional header line must not use keywords specified in "
            + "relay lines, or relay line is missing required keys: '%s'.",
            line));
      default:
        /* Ignore additional header lines. */
    }
  }

  private static class RelayLineImpl implements RelayLine {

    private String nodeId;

    @Override
    public Optional<String> nodeId() {
      return Optional.ofNullable(this.nodeId);
    }

    private String masterKeyEd25519;

    @Override
    public Optional<String> masterKeyEd25519() {
      return Optional.ofNullable(this.masterKeyEd25519);
    }

    private int bw;

    @Override
    public int bw() {
      return this.bw;
    }

    private Map<String, String> additionalKeyValues;

    @Override
    public Map<String, String> additionalKeyValues() {
      return null == this.additionalKeyValues ? Collections.emptyMap()
          : Collections.unmodifiableMap(this.additionalKeyValues);
    }

    private RelayLineImpl(String nodeId, String masterKeyEd25519, int bw,
        Map<String, String> additionalKeyValues) {
      this.nodeId = nodeId;
      this.masterKeyEd25519 = masterKeyEd25519;
      this.bw = bw;
      this.additionalKeyValues = additionalKeyValues;
    }
  }

  private void parseRelayLine(String line) throws DescriptorParseException {
    String[] spaceSeparatedLineParts = line.split(" ");
    String nodeId = null;
    String masterKeyEd25519 = null;
    Integer bw = null;
    Map<String, String> additionalKeyValues = new LinkedHashMap<>();
    for (String spaceSeparatedLinePart : spaceSeparatedLineParts) {
      String[] keyValueParts = spaceSeparatedLinePart.split("=", 2);
      if (keyValueParts.length != 2) {
        throw new DescriptorParseException(String.format(
            "Unrecognized space-separated line part '%s' without '=' "
                + "character in line '%s'.", spaceSeparatedLinePart, line));
      }
      String key = keyValueParts[0];
      if (key.length() < 1) {
        throw new DescriptorParseException(String.format(
            "Unrecognized space-separated line part '%s' starting with '=' "
                + "character in line '%s'.", spaceSeparatedLinePart, line));
      }
      String value = keyValueParts[1];
      switch (key) {
        case "node_id":
          nodeId = value;
          break;
        case "master_key_ed25519":
          masterKeyEd25519 = value;
          break;
        case "bw":
          try {
            bw = Integer.parseInt(value);
          } catch (NumberFormatException e) {
            throw new DescriptorParseException(String.format(
                "Unable to parse bw '%s' in line '%s'.", value, line), e);
          }
          break;
        default:
          additionalKeyValues.put(key, value);
      }
    }
    if (null == nodeId && null == masterKeyEd25519) {
      throw new DescriptorParseException(String.format(
          "Expected relay line, but line contains neither node_id nor "
          + "master_key_ed25519: '%s'.", line));
    }
    if (null == bw) {
      throw new DescriptorParseException(String.format(
          "Expected relay line, but line does not contain bw: '%s'.", line));
    }
    this.relayLines.add(new RelayLineImpl(nodeId, masterKeyEd25519, bw,
        additionalKeyValues.isEmpty() ? null : additionalKeyValues));
  }

  @Override
  public String digestSha256Base64() {
    return this.getDigestSha256Base64();
  }

  private LocalDateTime timestamp;

  @Override
  public LocalDateTime timestamp() {
    return this.timestamp;
  }

  private EnumMap<KeyWithStringValue, String> parsedStrings
      = new EnumMap<>(KeyWithStringValue.class);

  @Override
  public String version() {
    return this.parsedStrings.getOrDefault(KeyWithStringValue.version,
        "1.0.0");
  }

  @Override
  public String software() {
    return this.parsedStrings.getOrDefault(KeyWithStringValue.software,
        "torflow");
  }

  @Override
  public Optional<String> softwareVersion() {
    return Optional.ofNullable(
        this.parsedStrings.get(KeyWithStringValue.software_version));
  }

  private EnumMap<KeyWithLocalDateTimeValue, LocalDateTime> parsedLocalDateTimes
      = new EnumMap<>(KeyWithLocalDateTimeValue.class);

  @Override
  public Optional<LocalDateTime> fileCreated() {
    return Optional.ofNullable(this.parsedLocalDateTimes.get(
        KeyWithLocalDateTimeValue.file_created));
  }

  @Override
  public Optional<LocalDateTime> generatorStarted() {
    return Optional.ofNullable(this.parsedLocalDateTimes.get(
        KeyWithLocalDateTimeValue.generator_started));
  }

  @Override
  public Optional<LocalDateTime> earliestBandwidth() {
    return Optional.ofNullable(this.parsedLocalDateTimes.get(
        KeyWithLocalDateTimeValue.earliest_bandwidth));
  }

  @Override
  public Optional<LocalDateTime> latestBandwidth() {
    return Optional.ofNullable(this.parsedLocalDateTimes.get(
        KeyWithLocalDateTimeValue.latest_bandwidth));
  }

  private EnumMap<KeyWithIntValue, Integer> parsedInts
      = new EnumMap<>(KeyWithIntValue.class);

  @Override
  public Optional<Integer> numberEligibleRelays() {
    return Optional.ofNullable(this.parsedInts.get(
        KeyWithIntValue.number_eligible_relays));
  }

  @Override
  public Optional<Integer> minimumPercentEligibleRelays() {
    return Optional.ofNullable(this.parsedInts.get(
        KeyWithIntValue.minimum_percent_eligible_relays));
  }

  @Override
  public Optional<Integer> numberConsensusRelays() {
    return Optional.ofNullable(this.parsedInts.get(
        KeyWithIntValue.number_consensus_relays));
  }

  @Override
  public Optional<Integer> percentEligibleRelays() {
    return Optional.ofNullable(this.parsedInts.get(
        KeyWithIntValue.percent_eligible_relays));
  }

  @Override
  public Optional<Integer> minimumNumberEligibleRelays() {
    return Optional.ofNullable(this.parsedInts.get(
        KeyWithIntValue.minimum_number_eligible_relays));
  }

  private String scannerCountry;

  @Override
  public Optional<String> scannerCountry() {
    return Optional.ofNullable(this.scannerCountry);
  }

  private String[] destinationsCountries;

  @Override
  public Optional<String[]> destinationsCountries() {
    return Optional.ofNullable(this.destinationsCountries);
  }

  @Override
  public Optional<Integer> recentConsensusCount() {
    return Optional.ofNullable(this.parsedInts.get(
        KeyWithIntValue.recent_consensus_count));
  }

  @Override
  public Optional<Integer> recentPriorityListCount() {
    return Optional.ofNullable(this.parsedInts.get(
        KeyWithIntValue.recent_priority_list_count));
  }

  @Override
  public Optional<Integer> recentPriorityRelayCount() {
    return Optional.ofNullable(this.parsedInts.get(
        KeyWithIntValue.recent_priority_relay_count));
  }

  @Override
  public Optional<Integer> recentMeasurementAttemptCount() {
    return Optional.ofNullable(this.parsedInts.get(
        KeyWithIntValue.recent_measurement_attempt_count));
  }

  @Override
  public Optional<Integer> recentMeasurementFailureCount() {
    return Optional.ofNullable(this.parsedInts.get(
        KeyWithIntValue.recent_measurement_failure_count));
  }

  @Override
  public Optional<Integer> recentMeasurementsExcludedErrorCount() {
    return Optional.ofNullable(this.parsedInts.get(
        KeyWithIntValue.recent_measurements_excluded_error_count));
  }

  @Override
  public Optional<Integer> recentMeasurementsExcludedNearCount() {
    return Optional.ofNullable(this.parsedInts.get(
        KeyWithIntValue.recent_measurements_excluded_near_count));
  }

  @Override
  public Optional<Integer> recentMeasurementsExcludedOldCount() {
    return Optional.ofNullable(this.parsedInts.get(
        KeyWithIntValue.recent_measurements_excluded_old_count));
  }

  @Override
  public Optional<Integer> recentMeasurementsExcludedFewCount() {
    return Optional.ofNullable(this.parsedInts.get(
        KeyWithIntValue.recent_measurements_excluded_few_count));
  }

  private Duration timeToReportHalfNetwork;

  @Override
  public Optional<Duration> timeToReportHalfNetwork() {
    return Optional.ofNullable(this.timeToReportHalfNetwork);
  }

  private List<RelayLine> relayLines = new ArrayList<>();

  @Override
  public List<RelayLine> relayLines() {
    return this.relayLines.isEmpty() ? Collections.emptyList()
        : Collections.unmodifiableList(this.relayLines);
  }
}

