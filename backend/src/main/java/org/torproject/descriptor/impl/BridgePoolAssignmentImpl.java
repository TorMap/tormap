/* Copyright 2012--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.BridgePoolAssignment;
import org.torproject.descriptor.DescriptorParseException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
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

  private long publishedMillis;

  @Override
  public long getPublishedMillis() {
    return this.publishedMillis;
  }

  // Multiple details per fingerprint; deterministic ordering handled on
  // readback.
  private final Map<String, List<String>> entries = new TreeMap<>();

  private void parseBridgeLine(String line) throws DescriptorParseException {
    String[] parts = line.split("[ \t]+");
    if (parts.length < 2) {
      throw new DescriptorParseException("Illegal line '" + line
          + "' in bridge pool assignment.");
    }
    String fingerprint = ParseHelper.parseTwentyByteHexString(line, parts[0]);
    String poolAndDetails = line.substring(line.indexOf(SP) + 1);

    // Append this detail under the fingerprint (preserves insertion order)
    entries.computeIfAbsent(fingerprint, k -> new ArrayList<>())
        .add(poolAndDetails);
  }

  /**
   * Returns a defensive, unmodifiable copy:
   * - keys are sorted to keep descriptor output deterministic
   * - each list is unmodifiable
   * - the outer map is unmodifiable.
   */
  @Override
  public Map<String, List<String>> getEntries() {
    Map<String, List<String>> copy = new TreeMap<>();
    for (Map.Entry<String, List<String>> e : entries.entrySet()) {
      copy.put(e.getKey(), List.copyOf(e.getValue()));
    }
    return Collections.unmodifiableMap(copy);
  }

}
