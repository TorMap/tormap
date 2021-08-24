/* Copyright 2012--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.DescriptorParseException;
import org.torproject.descriptor.ExitList;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ExitListImpl extends DescriptorImpl implements ExitList {

  private static final long serialVersionUID = 4227448935362502463L;

  protected ExitListImpl(byte[] rawDescriptorBytes, File descriptorfile,
      String fileName) throws DescriptorParseException {
    super(rawDescriptorBytes, new int[] { 0, rawDescriptorBytes.length },
        descriptorfile, false);
    this.splitAndParseExitListEntries();
    this.setPublishedMillisFromFileName(fileName);
  }

  private void setPublishedMillisFromFileName(String fileName)
      throws DescriptorParseException {
    if (this.downloadedMillis == 0L
        && fileName.length() == "2012-02-01-04-06-24".length()) {
      try {
        SimpleDateFormat fileNameFormat = new SimpleDateFormat(
            "yyyy-MM-dd-HH-mm-ss");
        fileNameFormat.setLenient(false);
        fileNameFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        this.downloadedMillis = fileNameFormat.parse(fileName).getTime();
      } catch (ParseException e) {
        /* Handle below. */
      }
    }
    if (this.downloadedMillis == 0L) {
      throw new DescriptorParseException("Unrecognized exit list file "
          + "name '" + fileName + "'.");
    }
  }

  private void splitAndParseExitListEntries()
      throws DescriptorParseException {
    Scanner scanner = this.newScanner().useDelimiter(EOL);
    StringBuilder sb = new StringBuilder();
    boolean firstEntry = true;
    while (scanner.hasNext()) {
      String line = scanner.next();
      if (line.startsWith("@")) { /* Skip annotation. */
        if (!scanner.hasNext()) {
          throw new DescriptorParseException("Descriptor is empty.");
        } else {
          line = scanner.next();
        }
      }
      String[] parts = line.split(" ");
      String keyword = parts[0];
      switch (keyword) {
        case "Downloaded":
          this.downloadedMillis = ParseHelper.parseTimestampAtIndex(line,
              parts, 1, 2);
          break;
        case "ExitNode":
          if (!firstEntry) {
            this.parseExitListEntry(sb.toString());
          } else {
            firstEntry = false;
          }
          sb = new StringBuilder();
          sb.append(line).append(ExitList.EOL);
          break;
        case "Published":
        case "LastStatus":
        case "ExitAddress":
          sb.append(line).append(ExitList.EOL);
          break;
        default:
          if (this.unrecognizedLines == null) {
            this.unrecognizedLines = new ArrayList<>();
          }
          this.unrecognizedLines.add(line);
      }
    }
    /* Parse the last entry. */
    this.parseExitListEntry(sb.toString());
  }

  protected void parseExitListEntry(String exitListEntryString)
      throws DescriptorParseException {
    ExitListEntryImpl exitListEntry = new ExitListEntryImpl(
        exitListEntryString);
    this.exitListEntries.add(exitListEntry);
    List<String> unrecognizedExitListEntryLines = exitListEntry
        .getAndClearUnrecognizedLines();
    if (unrecognizedExitListEntryLines != null) {
      if (this.unrecognizedLines == null) {
        this.unrecognizedLines = new ArrayList<>();
      }
      this.unrecognizedLines.addAll(unrecognizedExitListEntryLines);
    }
  }

  private long downloadedMillis;

  @Override
  public long getDownloadedMillis() {
    return this.downloadedMillis;
  }

  private Set<Entry> exitListEntries = new HashSet<>();

  @Override
  public Set<Entry> getEntries() {
    return new HashSet<>(this.exitListEntries);
  }
}

