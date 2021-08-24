package org.torproject.descriptor;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.List;
import java.util.Optional;

/**
 * A GeoIP file contains information about the geographical (country
 * code) and topological (autonomous system) location of an IP address.
 *
 * @since 2.16.0
 */
public interface GeoipFile extends Descriptor {

  String EOL = "\n";

  public interface GeoipEntry extends Serializable {
    InetAddress getStart();

    InetAddress getEnd();

    String getCountryCode();

    String getAutonomousSystemNumber();
  }

  List<GeoipEntry> getEntries();

  Optional<GeoipEntry> getEntry(InetAddress forAddress);
}