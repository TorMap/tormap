/* Copyright 2016--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.index;

import java.util.SortedSet;

/**
 * A directory node has a file set and a set of subdirectories.
 *
 * @since 1.4.0
 */
public class DirectoryNode implements Comparable<DirectoryNode> {

  /** Path (i.e. directory name) is exposed in JSON. */
  public final String path;

  /** The file list is exposed in JSON. Sorted according to path. */
  public final SortedSet<FileNode> files;

  /** The directory list is exposed in JSON. Sorted according to path. */
  public final SortedSet<DirectoryNode> directories;

  /* Added to satisfy Jackson. */
  private DirectoryNode() {
    this.path = null;
    this.files = null;
    this.directories = null;
  }

  /** A directory for the JSON structure. */
  public DirectoryNode(String path, SortedSet<FileNode> files,
                SortedSet<DirectoryNode> directories) {
    this.path = path;
    this.files = files;
    this.directories = directories;
  }

  /**
   * This compareTo is not compatible with equals or hash!
   * It simply ensures a path-sorted JSON output.
   */
  @Override
  public int compareTo(DirectoryNode other) {
    return this.path.compareTo(other.path);
  }

  /** For debugging purposes. */
  @Override
  public String toString() {
    return "Dn: " + path + " fns: " + files + " dirs: " + directories;
  }
}

