/* Copyright 2015--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor;

import java.io.File;

/**
 * Descriptor source that synchronizes descriptors from the CollecTor
 * service to a given local directory.
 *
 * <p>This type is not a descriptor source in the proper sense, because it
 * does not produce descriptors by itself.  But it often creates the
 * prerequisites for reading descriptors from disk using
 * {@link DescriptorReader}.</p>
 *
 * <p>Code sample:</p>
 * <pre>{@code
 * DescriptorCollector descriptorCollector =
 *     DescriptorSourceFactory.createDescriptorCollector();
 * descriptorCollector.collectDescriptors(
 *     // Download from Tor's main CollecTor instance,
 *     "https://collector.torproject.org",
 *     // include network status consensuses and relay server descriptors
 *     new String[] { "/recent/relay-descriptors/consensuses/",
 *     "/recent/relay-descriptors/server-descriptors/" },
 *     // regardless of last-modified time,
 *     0L,
 *     // write to the local directory called in/,
 *     new File("in"),
 *     // and delete extraneous files that do not exist remotely anymore.
 *     true);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface DescriptorCollector {

  /**
   * Fetch remote files from a CollecTor instance that do not yet exist
   * locally and possibly delete local files that do not exist remotely
   * anymore.
   *
   * @param collecTorBaseUrl CollecTor base URL without trailing slash,
   *     e.g., {@code "https://collector.torproject.org"}
   * @param remoteDirectories Remote directories to collect descriptors
   *     from, e.g.,
   *     {@code "recent/relay-descriptors/server-descriptors"}, without
   *     processing subdirectories unless they are explicitly listed.
   *     Leading and trailing slashes will be ignored, i.e., {@code "/abc/xyz/"}
   *     results in the same downloads as  {@code "abc/xyz"}.
   * @param minLastModified Minimum last-modified time in milliseconds of
   *     files to be collected, or 0 for collecting all files
   * @param localDirectory Directory where collected files will be written
   * @param deleteExtraneousLocalFiles Whether to delete all local files
   *     that do not exist remotely anymore
   *
   * @since 1.0.0
   */
  void collectDescriptors(String collecTorBaseUrl,
      String[] remoteDirectories, long minLastModified,
      File localDirectory, boolean deleteExtraneousLocalFiles);
}

