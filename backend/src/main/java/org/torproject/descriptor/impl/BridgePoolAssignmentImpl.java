/* Copyright 2012--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.BridgePoolAssignment;
import org.torproject.descriptor.DescriptorParseException;

import java.io.File;
import java.util.EnumSet;
import java.util.Scanner;
import java.util.SortedMap;
import java.util.TreeMap;

public class BridgePoolAssignmentImpl extends DescriptorImpl
    implements BridgePoolAssignment {

  private static final long serialVersionUID = -8370471568586190472L;

  protected BridgePoolAssignmentImpl(byte[] rawDescriptorBytes,
      int[] offsetAndlength, File descriptorFile)
      throws DescriptorParseException {
    super(rawDescriptorBytes, offsetAndlength, descriptorFile, false);
    this.parseDescriptorBytes();
    this.checkExactlyOnceKeys(EnumSet.of(Key.BRIDGE_POOL_ASSIGNMENT));
    this.checkFirstKey(Key.BRIDGE_POOL_ASSIGNMENT);
    this.clearParsedKeys();
  }

  private void parseDescriptorBytes() throws DescriptorParseException {
    Scanner scanner = this.newScanner().useDelimiter(NL);
    while (scanner.hasNext()) {
      String line = scanner.next();
      if (line.startsWith(Key.BRIDGE_POOL_ASSIGNMENT.keyword + SP)) {
        this.parseBridgePoolAssignmentLine(line);
      } else {
        this.parseBridgeLine(line);
      }
    }
  }

  private void parseBridgePoolAssignmentLine(String line)
      throws DescriptorParseException {
    String[] parts = line.split("[ \t]+");
    if (parts.length != 3) {
      throw new DescriptorParseException("Illegal line '" + line
          + "' in bridge pool assignment.");
    }
    this.publishedMillis = ParseHelper.parseTimestampAtIndex(line,
        parts, 1, 2);
  }

  private void parseBridgeLine(String line)
      throws DescriptorParseException {
    String[] parts = line.split("[ \t]+");
    if (parts.length < 2) {
      throw new DescriptorParseException("Illegal line '" + line
          + "' in bridge pool assignment.");
    }
    String fingerprint = ParseHelper.parseTwentyByteHexString(line,
        parts[0]);
    String poolAndDetails = line.substring(line.indexOf(SP) + 1);
    this.entries.put(fingerprint, poolAndDetails);
  }

  private long publishedMillis;

  @Override
  public long getPublishedMillis() {
    return this.publishedMillis;
  }

  private SortedMap<String, String> entries = new TreeMap<>();

  @Override
  public SortedMap<String, String> getEntries() {
    return new TreeMap<>(this.entries);
  }
}

