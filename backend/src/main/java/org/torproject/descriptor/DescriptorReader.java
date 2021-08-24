/* Copyright 2011--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor;

import java.io.File;
import java.util.SortedMap;

/**
 * Descriptor source that reads descriptors from local files and provides
 * an iterator over parsed descriptors.
 *
 * <p>This descriptor source is likely the most widely used one, possibly
 * in combination with {@link DescriptorCollector} to synchronize
 * descriptors from the CollecTor service.</p>
 *
 * <p>Reading descriptors is done in a batch which starts after setting
 * any configuration options and initiating the read process.</p>
 *
 * <p>Code sample:</p>
 * <pre>{@code
 * DescriptorReader descriptorReader =
 *     DescriptorSourceFactory.createDescriptorReader();
 * // Read descriptors from local directory called in/.
 * for (Descriptor descriptor :
 *     descriptorReader.readDescriptors(new File("in")) {
 *   // Only process network status consensuses, ignore the rest.
 *   if ((descriptor instanceof RelayNetworkStatusConsensus)) {
 *     RelayNetworkStatusConsensus consensus =
 *         (RelayNetworkStatusConsensus) descriptor;
 *     processConsensus(consensus);
 *   }
 * }}</pre>
 *
 * @since 1.0.0
 */
public interface DescriptorReader {

  /**
   * Set a history file to load before reading descriptors and exclude
   * descriptor files that haven't changed since they have last been read.
   *
   * <p>Lines in the history file contain the last modified time in
   * milliseconds since the epoch and the absolute path of a file, separated by
   * a space.</p>
   *
   * @since 1.6.0
   */
  void setHistoryFile(File historyFile);

  /**
   * Save a history file with file names and last modified timestamps of
   * descriptor files that exist in the input directory or directories and that
   * have either been parsed or excluded from parsing.
   *
   * <p>Lines in the history file contain the last modified time in
   * milliseconds since the epoch and the absolute path of a file, separated by
   * a space.</p>
   *
   * @since 1.6.0
   */
  void saveHistoryFile(File historyFile);

  /**
   * Exclude files if they haven't changed since the corresponding last
   * modified timestamps.
   *
   * <p>Can be used instead of (or in addition to) a history file.</p>
   *
   * @since 1.0.0
   */
  void setExcludedFiles(SortedMap<String, Long> excludedFiles);

  /**
   * Return files and last modified timestamps of files that exist in the
   * input directory or directories, but that have been excluded from
   * parsing, because they haven't changed since they were last read.
   *
   * <p>Can be used instead of (or in addition to) a history file when
   * combined with the set of parsed files.</p>
   *
   * @since 1.0.0
   */
  SortedMap<String, Long> getExcludedFiles();

  /**
   * Return files and last modified timestamps of files that exist in the
   * input directory or directories and that have been parsed.
   *
   * <p>Can be used instead of (or in addition to) a history file when
   * combined with the set of excluded files.</p>
   *
   * @since 1.0.0
   */
  SortedMap<String, Long> getParsedFiles();

  /**
   * Don't keep more than this number of descriptors in the queue (default:
   * 100).
   *
   * @param maxDescriptorsInQueue Maximum number of descriptors in the queue.
   *
   * @since 1.9.0
   */
  void setMaxDescriptorsInQueue(int maxDescriptorsInQueue);

  /**
   * Read descriptors from the given descriptor file(s) and return the parsed
   * descriptors.
   *
   * <p>Whenever the reader runs out of descriptors and expects to provide
   * more shortly after, it blocks the caller.  This method can only be
   * run once.</p>
   *
   * @param descriptorFiles One or more directories, tarballs, or files
   *     containing descriptors.
   *
   * @return Parsed descriptors.
   *
   * @since 1.9.0
   */
  Iterable<Descriptor> readDescriptors(File... descriptorFiles);
}

