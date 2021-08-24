/* Copyright 2012--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.DescriptorParseException;
import org.torproject.descriptor.ExitList;

import java.util.*;

public class ExitListEntryImpl implements ExitList.Entry {

  private static final long serialVersionUID = 9014559583423738584L;

  private String exitListEntryString;

  private List<String> unrecognizedLines;

  protected List<String> getAndClearUnrecognizedLines() {
    List<String> lines = this.unrecognizedLines;
    this.unrecognizedLines = null;
    return lines;
  }

  protected ExitListEntryImpl(String exitListEntryString)
      throws DescriptorParseException {
    this.exitListEntryString = exitListEntryString;
    this.initializeKeywords();
    this.parseExitListEntry();
    this.checkAndClearKeywords();
  }

  private SortedSet<String> keywordCountingSet;

  private void initializeKeywords() {
    this.keywordCountingSet = new TreeSet<>();
    this.keywordCountingSet.add("ExitNode");
    this.keywordCountingSet.add("Published");
    this.keywordCountingSet.add("LastStatus");
    this.keywordCountingSet.add("ExitAddress");
  }

  private void parsedExactlyOnceKeyword(String keyword)
      throws DescriptorParseException {
    if (!this.keywordCountingSet.contains(keyword)) {
      throw new DescriptorParseException("Duplicate '" + keyword
          + "' line in exit list entry.");
    }
    this.keywordCountingSet.remove(keyword);
  }

  private void checkAndClearKeywords() throws DescriptorParseException {
    for (String missingKeyword : this.keywordCountingSet) {
      throw new DescriptorParseException("Missing '" + missingKeyword
          + "' line in exit list entry.");
    }
    this.keywordCountingSet = null;
  }

  private void parseExitListEntry()
          throws DescriptorParseException {
    try (Scanner scanner = new Scanner(this.exitListEntryString)
            .useDelimiter(ExitList.EOL)) {
      while (scanner.hasNext()) {
        String line = scanner.next();
        String[] parts = line.split(" ");
        String keyword = parts[0];
        switch (keyword) {
          case "ExitNode":
            this.parseExitNodeLine(line, parts);
            break;
          case "Published":
            this.parsePublishedLine(line, parts);
            break;
          case "LastStatus":
            this.parseLastStatusLine(line, parts);
            break;
          case "ExitAddress":
            this.parseExitAddressLine(line, parts);
            break;
          default:
            if (this.unrecognizedLines == null) {
              this.unrecognizedLines = new ArrayList<>();
            }
            this.unrecognizedLines.add(line);
        }
      }
    }
  }

  private void parseExitNodeLine(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length != 2) {
      throw new DescriptorParseException("Invalid line '" + line + "' in "
          + "exit list entry.");
    }
    this.parsedExactlyOnceKeyword(parts[0]);
    this.fingerprint = ParseHelper.parseTwentyByteHexString(line,
        parts[1]);
  }

  private void parsePublishedLine(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length != 3) {
      throw new DescriptorParseException("Invalid line '" + line + "' in "
          + "exit list entry.");
    }
    this.parsedExactlyOnceKeyword(parts[0]);
    this.publishedMillis = ParseHelper.parseTimestampAtIndex(line, parts,
        1, 2);
  }

  private void parseLastStatusLine(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length != 3) {
      throw new DescriptorParseException("Invalid line '" + line + "' in "
          + "exit list entry.");
    }
    this.parsedExactlyOnceKeyword(parts[0]);
    this.lastStatusMillis = ParseHelper.parseTimestampAtIndex(line, parts,
        1, 2);
  }

  private void parseExitAddressLine(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length != 4) {
      throw new DescriptorParseException("Invalid line '" + line + "' in "
          + "exit list entry.");
    }
    this.keywordCountingSet.remove(parts[0]);
    this.exitAddresses.put(ParseHelper.parseIpv4Address(line, parts[1]),
        ParseHelper.parseTimestampAtIndex(line, parts, 2, 3));
  }

  private String fingerprint;

  @Override
  public String getFingerprint() {
    return this.fingerprint;
  }

  private long publishedMillis;

  @Override
  public long getPublishedMillis() {
    return this.publishedMillis;
  }

  private long lastStatusMillis;

  @Override
  public long getLastStatusMillis() {
    return this.lastStatusMillis;
  }

  private Map<String, Long> exitAddresses = new HashMap<>();

  @Override
  public Map<String, Long> getExitAddresses() {
    return new HashMap<>(this.exitAddresses);
  }
}

