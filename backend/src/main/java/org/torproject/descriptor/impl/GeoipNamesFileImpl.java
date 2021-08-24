package org.torproject.descriptor.impl;

import org.torproject.descriptor.Descriptor;
import org.torproject.descriptor.DescriptorParseException;
import org.torproject.descriptor.GeoipNamesFile;

import java.io.File;
import java.util.*;

public class GeoipNamesFileImpl extends DescriptorImpl
        implements GeoipNamesFile {

  private final Map<String, String> names;

  protected GeoipNamesFileImpl(byte[] rawDescriptorBytes,
                          int[] offsetAndLength,File descriptorFile)
          throws DescriptorParseException {
    super(rawDescriptorBytes, offsetAndLength, descriptorFile);
    names = new HashMap<>();
    this.splitAndParseEntries();
  }

  /**
   * Parse a GeoIP file.
   */
  public static List<Descriptor> parse(byte[] rawDescriptorBytes,
      File sourceFile) throws DescriptorParseException {
    List<Descriptor> result = new ArrayList<>();
    result.add(new GeoipNamesFileImpl(rawDescriptorBytes,
            new int[]{0, rawDescriptorBytes.length}, sourceFile));
    return result;
  }

  private void splitAndParseEntries() throws DescriptorParseException {
    Scanner scanner = this.newScanner().useDelimiter(EOL);
    while (scanner.hasNext()) {
      String line = scanner.next();
      if (line.startsWith("@")) {
        /* Skip annotation. */
        if (!scanner.hasNext()) {
          throw new DescriptorParseException("Descriptor is empty.");
        }
        continue;
      }
      String[] parts = line.split(",", 2);
      if (parts.length < 2) {
        if (this.unrecognizedLines == null) {
          this.unrecognizedLines = new ArrayList<>();
        }
        this.unrecognizedLines.add(line);
      }
      names.put(parts[0], parts[1]);
      String name = parts[1];
    }
  }

  @Override
  public int size() {
    return names.size();
  }

  @Override
  public boolean isEmpty() {
    return names.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return names.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return names.containsValue(value);
  }

  @Override
  public String get(Object key) {
    return names.get(key);
  }

  @Override
  public String put(String key, String value) {
    throw new UnsupportedOperationException("Names map is read-only.");
  }

  @Override
  public String remove(Object key) {
    throw new UnsupportedOperationException("Names map is read-only.");
  }

  @Override
  public void putAll(Map<? extends String, ? extends String> map) {
    throw new UnsupportedOperationException("Names map is read-only.");
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException("Names map is read-only.");
  }

  @Override
  public Set<String> keySet() {
    return names.keySet();
  }

  @Override
  public Collection<String> values() {
    return names.values();
  }

  @Override
  public Set<Entry<String, String>> entrySet() {
    return names.entrySet();
  }
}
