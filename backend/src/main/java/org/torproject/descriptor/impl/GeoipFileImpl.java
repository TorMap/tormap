package org.torproject.descriptor.impl;

import org.torproject.descriptor.Descriptor;
import org.torproject.descriptor.DescriptorParseException;
import org.torproject.descriptor.GeoipFile;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class GeoipFileImpl extends DescriptorImpl implements GeoipFile {

  private final List<GeoipEntry> entries;

  protected GeoipFileImpl(byte[] rawDescriptorBytes,
                          int[] offsetAndLength,File descriptorFile)
          throws DescriptorParseException {
    super(rawDescriptorBytes, offsetAndLength, descriptorFile);
    entries = new ArrayList<>();
    this.splitAndParseEntries();
  }

  private void splitAndParseEntries() throws DescriptorParseException {
    Scanner scanner = this.newScanner().useDelimiter(EOL);
    while (scanner.hasNext()) {
      String line = scanner.next();
      if (line.startsWith("@") || line.startsWith("#")) {
        /* Skip annotation and comments. */
        if (!scanner.hasNext()) {
          throw new DescriptorParseException("Descriptor is empty.");
        }
        continue;
      }
      String[] parts = line.split(",");
      if (parts.length < 3) {
        if (this.unrecognizedLines == null) {
          this.unrecognizedLines = new ArrayList<>();
        }
        this.unrecognizedLines.add(line);
      }
      String start = parts[0];
      String end = parts[1];
      String countryCode = parts[2];
      String autonomousSystemNumber;
      if (parts.length >= 4) {
        autonomousSystemNumber = parts[3];
      } else {
        autonomousSystemNumber = null;
      }
      try {
        entries.add(new GeoipEntryImpl(start, end, countryCode,
                                       autonomousSystemNumber));
      } catch (UnknownHostException e) {
        if (this.unrecognizedLines == null) {
          this.unrecognizedLines = new ArrayList<>();
        }
        this.unrecognizedLines.add(line);
      }
    }
  }

  @Override
  public List<GeoipEntry> getEntries() {
    return this.entries;
  }

  /**
   * Compares two InetAddresses.
   *
   * @param a1 first address
   * @param a2 second address
   * @return -1 if a1 < a2, 0 if a1 == a2, and 1 if a1 > a2
   * @throws RuntimeException when addresses have differing lengths
   */
  private int compareAddresses(InetAddress a1, InetAddress a2) {
    byte[] b1 = a1.getAddress();
    byte[] b2 = a2.getAddress();
    if (b1.length != b2.length) {
      throw new RuntimeException(
              "Comparing two addresses of different lengths.");
    }
    for (int i = 0; i < b1.length; i++) {
      int i1 = ((int) b1[i] & 0xFF);
      int i2 = ((int) b2[i] & 0xFF);
      if (i1 < i2) {
        return -1;
      } else if (i1 > i2) {
        return 1;
      }
    }
    return 0;
  }

  @Override
  public Optional<GeoipEntry> getEntry(InetAddress forAddress) {
    int low = 0;
    int mid;
    int high = entries.size();
    while (low <= high) {
      mid = (low + high) / 2;
      GeoipEntry entry = entries.get(mid);
      int startComparisonResult = compareAddresses(
              entry.getStart(), forAddress);
      if (startComparisonResult == -1) {
        /* entry start is less than desired address */
        int endComparisonResult = compareAddresses(
                entry.getEnd(), forAddress);
        if (endComparisonResult >= 0) {
          /* entry end is equal to or greater than desired address */
          return Optional.of(entry);
        }
        low = mid + 1;
      } else if (startComparisonResult == 1) {
        /* entry start is greater than desired address */
        high = mid - 1;
      } else if (startComparisonResult == 0) {
        /* entry start is the same as desired address */
        return Optional.of(entry);
      }
    }
    return Optional.empty();
  }

  /**
   * Parse a GeoIP file.
   */
  public static List<Descriptor> parse(byte[] rawDescriptorBytes,
       File sourceFile) throws DescriptorParseException {
    List<Descriptor> result = new ArrayList<>();
    result.add(new GeoipFileImpl(rawDescriptorBytes,
            new int[]{0, rawDescriptorBytes.length}, sourceFile));
    return result;
  }
}
