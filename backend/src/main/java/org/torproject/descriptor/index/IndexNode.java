/* Copyright 2016--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.index;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.torproject.descriptor.internal.FileType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * An index node is the top-level node in the JSON structure.
 * It provides some utility methods for reading
 * and searching (in a limited way) it's sub-structure.
 *
 * @since 1.4.0
 */
@JsonPropertyOrder({ "created", "revision", "path", "directories", "files" })
public class IndexNode {

  private static final int READ_TIMEOUT = Integer.parseInt(System
      .getProperty("sun.net.client.defaultReadTimeout", "60000"));

  private static final int CONNECT_TIMEOUT = Integer.parseInt(System
      .getProperty("sun.net.client.defaultConnectTimeout", "60000"));

  /** An empty node, which is not added to JSON output. */
  public static final IndexNode emptyNode = new IndexNode("", "",
      new TreeSet<>(), new TreeSet<>());

  private static ObjectMapper objectMapper = new ObjectMapper()
      .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
      .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
      .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
      .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

  /** The created date-time is exposed in JSON as 'index_created' field. */
  @JsonProperty("index_created")
  public final String created;

  /** The software's build revision JSON as 'build_revision' field. */
  @JsonProperty("build_revision")
  public final String revision;

  /** Path (i.e. base url) is exposed in JSON. */
  public final String path;

  /** The directory list is exposed in JSON. Sorted according to path. */
  public final SortedSet<DirectoryNode> directories;

  /** The file list is exposed in JSON. Sorted according to path. */
  public final SortedSet<FileNode> files;

  /* Added to satisfy Jackson. */
  private IndexNode() {
    this.created = null;
    this.revision = null;
    this.path = null;
    this.files = null;
    this.directories = null;
  }

  /** For backwards compatibility and testing. */
  public IndexNode(String created, String path,
            SortedSet<FileNode> files,
            SortedSet<DirectoryNode> directories) {
    this(created, null, path, files, directories);
  }

  /** An index node is the top-level node in the JSON structure. */
  public IndexNode(String created, String revision, String path,
            SortedSet<FileNode> files,
            SortedSet<DirectoryNode> directories) {
    this.created = created;
    this.revision = revision;
    this.path = path;
    this.files = files;
    this.directories = directories;
  }

  /**
   * Reads JSON from given URL String.
   * Returns an empty IndexNode in case of an error.
   */
  public static IndexNode fetchIndex(String urlString) throws Exception {
    String ending
        = urlString.substring(urlString.lastIndexOf(".") + 1).toUpperCase();
    URLConnection connection = (new URL(urlString)).openConnection();
    connection.setReadTimeout(READ_TIMEOUT);
    connection.setConnectTimeout(CONNECT_TIMEOUT);
    connection.connect();
    try (InputStream is = FileType.valueOf(ending)
        .inputStream(connection.getInputStream())) {
      return fetchIndex(is);
    }
  }

  /**
   * Reads JSON from given InputStream.
   * Returns an empty IndexNode in case of an error.
   */
  public static IndexNode fetchIndex(InputStream is) throws IOException {
    return objectMapper.readValue(is, IndexNode.class);
  }

  /** Return a map of file paths for the given directories. */
  public SortedMap<String, FileNode> retrieveFilesIn(String ... remoteDirs) {
    SortedMap<String, FileNode> map = new TreeMap<>();
    for (String remote : remoteDirs) {
      if (null == remote || remote.isEmpty()) {
        continue;
      }
      String[] dirs = remote.replaceAll("/", " ").trim().split(" ");
      DirectoryNode currentDir = findPathIn(dirs[0], this.directories);
      if (null == currentDir) {
        continue;
      }
      StringBuilder currentPath = new StringBuilder(dirs[0] + "/");
      for (int k = 1; k < dirs.length; k++) {
        DirectoryNode dn = findPathIn(dirs[k], currentDir.directories);
        if (null == dn) {
          break;
        } else {
          currentPath.append(dirs[k]).append("/");
          currentDir = dn;
        }
      }
      if (null == currentDir.files) {
        continue;
      }
      for (FileNode file : currentDir.files) {
        if (file.lastModifiedMillis() > 0) { // only add valid files
          map.put(currentPath.toString() + file.path, file);
        }
      }
    }
    return map;
  }

  /** Returns the directory nodes with the given path, but no file nodes. */
  public static DirectoryNode findPathIn(String path,
      SortedSet<DirectoryNode> dirs) {
    if (null != dirs) {
      for (DirectoryNode dn : dirs) {
        if (dn.path.equals(path)) {
          return dn;
        }
      }
    }
    return null;
  }

  /** Write JSON representation of the given index node to the given path. */
  public static void writeIndex(Path outPath, IndexNode indexNode)
      throws Exception {
    String ending = outPath.toString()
        .substring(outPath.toString().lastIndexOf(".") + 1).toUpperCase();
    try (OutputStream os = FileType.valueOf(ending)
         .outputStream(Files.newOutputStream(outPath))) {
      os.write(makeJsonString(indexNode).getBytes());
    }
  }

  /** Write JSON representation of the given index node to a string. */
  public static String makeJsonString(IndexNode indexNode) throws IOException {
    return objectMapper.writeValueAsString(indexNode);
  }

  /** For debugging purposes. */
  @Override
  public String toString() {
    return "index: " + path + ", created " + created
        + ",\nfns: " + files + ",\ndirs: " + directories;
  }
}

