package org.torproject.descriptor.impl;

import org.torproject.descriptor.GeoipFile;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

public class GeoipEntryImpl implements GeoipFile.GeoipEntry {
  private final InetAddress start;
  private final InetAddress end;
  private final String countryCode;
  private final String autonomousSystemNumber;

  /**
   * An entry in a GeoIP file.
   *
   * <p>The start and end are expecting to be a 32-bit integer represented as an
   * ASCII string for IPv4 addresses, and a colon-seperated address as an
   * ASCII string for IPv6 addresses. If you provide a dotted-quad IPv4 address
   * string then this class will also handle that, but you won't see that in
   * the files that exist at the time of writing this comment.
   *
   * @param start the start string found in the file
   * @param end the end string found in the file
   * @param countryCode the country code found in the file
   * @param autonomousSystemNumber the autonomous system number found in the
   *                               file, or null if not present
   * @throws UnknownHostException on failing to parse an IP address string
   */
  public GeoipEntryImpl(String start, String end, String countryCode,
                        String autonomousSystemNumber)
          throws UnknownHostException {
    InetAddress parsedStart;
    InetAddress parsedEnd;
    try {
      int addr = Integer.parseInt(start);
      parsedStart = InetAddress.getByAddress(new byte[]{
          (byte) (addr >>> 24), (byte) (addr >>> 16),
          (byte) (addr >>> 8), (byte) addr});
    } catch (NumberFormatException nfe) {
      parsedStart = InetAddress.getByName(start);
    }
    try {
      int addr = Integer.parseInt(end);
      parsedEnd = InetAddress.getByAddress(new byte[]{
          (byte) (addr >>> 24), (byte) (addr >>> 16),
          (byte) (addr >>> 8), (byte) addr});
    } catch (NumberFormatException nfe) {
      parsedEnd = InetAddress.getByName(end);
    }
    this.start = parsedStart;
    this.end = parsedEnd;
    this.countryCode = countryCode;
    this.autonomousSystemNumber = autonomousSystemNumber;
  }


  @Override
  public InetAddress getStart() {
    return this.start;
  }

  @Override
  public InetAddress getEnd() {
    return this.end;
  }

  @Override
  public String getCountryCode() {
    return this.countryCode;
  }

  @Override
  public String getAutonomousSystemNumber() {
    return this.autonomousSystemNumber;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    GeoipFile.GeoipEntry that = (GeoipFile.GeoipEntry) obj;
    return getStart().equals(that.getStart()) && getEnd().equals(that.getEnd())
            && getCountryCode().equals(that.getCountryCode())
            && Objects.equals(autonomousSystemNumber,
                              that.getAutonomousSystemNumber());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getStart(), getEnd(), getCountryCode(),
                        this.autonomousSystemNumber);
  }
}
