/* Copyright 2011--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.DescriptorParseException;
import org.torproject.descriptor.DirSourceEntry;
import org.torproject.descriptor.DirectorySignature;
import org.torproject.descriptor.NetworkStatusEntry;

import java.io.File;
import java.util.*;

/* Parse the common parts of v3 consensuses, v3 votes, v3 microdesc
 * consensuses, v2 statuses, and sanitized bridge network statuses and
 * delegate the specific parts to the subclasses. */
public abstract class NetworkStatusImpl extends DescriptorImpl {

  private static final long serialVersionUID = -2208207369822099643L;

  protected Map<String, Integer> flagIndexes = new HashMap<>();

  protected Map<Integer, String> flagStrings = new HashMap<>();

  protected NetworkStatusImpl(byte[] rawDescriptorBytes, int[] offsetAndLength,
      File descriptorFile, boolean blankLinesAllowed)
      throws DescriptorParseException {
    super(rawDescriptorBytes, offsetAndLength, descriptorFile,
        blankLinesAllowed);
  }

  protected final void splitAndParseParts(boolean containsDirSourceEntries)
      throws DescriptorParseException {
    int firstRIndex = this.findFirstIndexOfKey(Key.R);
    int firstDirectorySignatureIndex = this.findFirstIndexOfKey(
        Key.DIRECTORY_SIGNATURE);
    int endIndex = this.offset + this.length;
    if (firstDirectorySignatureIndex < 0) {
      firstDirectorySignatureIndex = endIndex;
    }
    int directoryFooterIndex = this.findFirstIndexOfKey(Key.DIRECTORY_FOOTER);
    if (directoryFooterIndex < 0) {
      directoryFooterIndex = firstDirectorySignatureIndex;
    }
    if (firstRIndex < 0) {
      firstRIndex = directoryFooterIndex;
    }
    int firstDirSourceIndex = !containsDirSourceEntries ? -1
        : this.findFirstIndexOfKey(Key.DIR_SOURCE);
    if (firstDirSourceIndex < 0) {
      firstDirSourceIndex = firstRIndex;
    }
    if (firstDirSourceIndex > this.offset) {
      this.parseHeader(this.offset, firstDirSourceIndex - this.offset);
    }
    if (firstRIndex > firstDirSourceIndex) {
      this.parseDirSources(firstDirSourceIndex, firstRIndex
          - firstDirSourceIndex);
    }
    if (directoryFooterIndex > firstRIndex) {
      this.parseStatusEntries(firstRIndex, directoryFooterIndex - firstRIndex);
    }
    if (firstDirectorySignatureIndex > directoryFooterIndex) {
      this.parseFooter(directoryFooterIndex, firstDirectorySignatureIndex
          - directoryFooterIndex);
    }
    if (endIndex > firstDirectorySignatureIndex) {
      this.parseDirectorySignatures(firstDirectorySignatureIndex,
          endIndex - firstDirectorySignatureIndex);
    }
  }

  private void parseDirSources(int offset, int length)
      throws DescriptorParseException {
    List<int[]> offsetsAndLengths = this.splitByKey(Key.DIR_SOURCE, offset,
        length, false);
    for (int[] offsetAndLength : offsetsAndLengths) {
      this.parseDirSource(offsetAndLength[0], offsetAndLength[1]);
    }
  }

  private void parseStatusEntries(int offset, int length)
      throws DescriptorParseException {
    List<int[]> offsetsAndLengths = this.splitByKey(Key.R, offset, length,
        false);
    for (int[] offsetAndLength : offsetsAndLengths) {
      this.parseStatusEntry(offsetAndLength[0], offsetAndLength[1]);
    }
  }

  private void parseDirectorySignatures(int offset, int length)
      throws DescriptorParseException {
    List<int[]> offsetsAndLengths = this.splitByKey(Key.DIRECTORY_SIGNATURE,
        offset, length, false);
    for (int[] offsetAndLength : offsetsAndLengths) {
      this.parseDirectorySignature(offsetAndLength[0], offsetAndLength[1]);
    }
  }

  protected abstract void parseHeader(int offset, int length)
      throws DescriptorParseException;

  protected void parseDirSource(int offset, int length)
      throws DescriptorParseException {
    DirSourceEntryImpl dirSourceEntry = new DirSourceEntryImpl(
        this, offset, length);
    this.dirSourceEntries.put(dirSourceEntry.getIdentity(),
        dirSourceEntry);
    List<String> unrecognizedDirSourceLines = dirSourceEntry
        .getAndClearUnrecognizedLines();
    if (unrecognizedDirSourceLines != null) {
      if (this.unrecognizedLines == null) {
        this.unrecognizedLines = new ArrayList<>();
      }
      this.unrecognizedLines.addAll(unrecognizedDirSourceLines);
    }
  }

  protected String[] parseClientOrServerVersions(String line,
      String[] parts) throws DescriptorParseException {
    String[] result;
    switch (parts.length) {
      case 1:
        result = new String[0];
        break;
      case 2:
        result = parts[1].split(",", -1);
        for (String version : result) {
          if (version.length() < 1) {
            throw new DescriptorParseException("Illegal versions line '"
                + line + "'.");
          }
        }
        break;
      default:
        throw new DescriptorParseException("Illegal versions line '"
            + line + "'.");
    }
    return result;
  }

  protected void parseStatusEntry(int offset, int length)
      throws DescriptorParseException {
    NetworkStatusEntryImpl statusEntry = new NetworkStatusEntryImpl(
        this, offset, length, false, this.flagIndexes, this.flagStrings);
    this.statusEntries.put(statusEntry.getFingerprint(), statusEntry);
    List<String> unrecognizedStatusEntryLines = statusEntry
        .getAndClearUnrecognizedLines();
    if (unrecognizedStatusEntryLines != null) {
      if (this.unrecognizedLines == null) {
        this.unrecognizedLines = new ArrayList<>();
      }
      this.unrecognizedLines.addAll(unrecognizedStatusEntryLines);
    }
  }

  protected abstract void parseFooter(int offset, int length)
      throws DescriptorParseException;

  protected void parseDirectorySignature(int offset, int length)
      throws DescriptorParseException {
    if (this.signatures == null) {
      this.signatures = new ArrayList<>();
    }
    DirectorySignatureImpl signature = new DirectorySignatureImpl(
        this, offset, length);
    this.signatures.add(signature);
    List<String> unrecognizedStatusEntryLines = signature
        .getAndClearUnrecognizedLines();
    if (unrecognizedStatusEntryLines != null) {
      if (this.unrecognizedLines == null) {
        this.unrecognizedLines = new ArrayList<>();
      }
      this.unrecognizedLines.addAll(unrecognizedStatusEntryLines);
    }
  }

  protected SortedMap<String, DirSourceEntry> dirSourceEntries =
      new TreeMap<>();

  public SortedMap<String, DirSourceEntry> getDirSourceEntries() {
    return new TreeMap<>(this.dirSourceEntries);
  }

  protected SortedMap<String, NetworkStatusEntry> statusEntries =
      new TreeMap<>();

  public SortedMap<String, NetworkStatusEntry> getStatusEntries() {
    return new TreeMap<>(this.statusEntries);
  }

  public boolean containsStatusEntry(String fingerprint) {
    return this.statusEntries.containsKey(fingerprint);
  }

  public NetworkStatusEntry getStatusEntry(String fingerprint) {
    return this.statusEntries.get(fingerprint);
  }

  protected List<DirectorySignature> signatures;

  public List<DirectorySignature> getSignatures() {
    return this.signatures == null ? null
        : new ArrayList<>(this.signatures);
  }

  /**
   * Implements method defined in
   * {@link org.torproject.descriptor.RelayNetworkStatusConsensus}
   * and {@link org.torproject.descriptor.RelayNetworkStatusVote}.
   */
  public SortedMap<String, DirectorySignature> getDirectorySignatures() {
    SortedMap<String, DirectorySignature> directorySignatures = null;
    if (this.signatures != null) {
      directorySignatures = new TreeMap<>();
      for (DirectorySignature signature : this.signatures) {
        directorySignatures.put(signature.getIdentity(), signature);
      }
    }
    return directorySignatures;
  }
}

