/* Copyright 2016--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.index;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

/**
 * A FileNode provides the file's name, size, and modified time.
 *
 * @since 1.4.0
 */
public class FileNode implements Comparable<FileNode> {

  private static final Logger logger = LoggerFactory.getLogger(FileNode.class);

  /** Path (i.e. file name) is exposed in JSON. */
  public final String path;

  /** The file size is exposed in JSON. */
  public final long size;

  /** The last modified date-time string is exposed in JSON. */
  public final String lastModified;

  @JsonIgnore
  private long lastModifiedMillis;

  /* Added to satisfy Gson. */
  private FileNode() {
    path = null;
    size = 0;
    lastModified = null;
  }

  /**
   * A FileNode needs a path, i.e. the file name, the file size, and
   * the last modified date-time string.
   */
  public FileNode(String path, long size, String lastModified) {
    this.path = path;
    this.size = size;
    this.lastModified = lastModified;
  }

  /**
   * This compareTo is not compatible with equals or hash!
   * It simply ensures a path-sorted Gson output.
   */
  @Override
  public int compareTo(FileNode other) {
    return this.path.compareTo(other.path);
  }

  /** Lazily returns the last modified time in millis. */
  public long lastModifiedMillis() {
    if (this.lastModifiedMillis == 0) {
      DateFormat dateTimeFormat =
          new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
      dateTimeFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      try {
        lastModifiedMillis = dateTimeFormat.parse(this.lastModified).getTime();
      } catch (ParseException ex) {
        logger.warn("Cannot parse last-modified time {} of remote file entry "
            + "{}. Fetching remote file regardless of configured last-modified "
            + "time. The following error message provides more details.",
            this.lastModified, this.path, ex);
        this.lastModifiedMillis = -1L;
      }
    }
    return this.lastModifiedMillis;
  }

  /** For debugging purposes. */
  @Override
  public String toString() {
    return "Fn: " + path;
  }
}

