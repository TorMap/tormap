package org.torproject.descriptor.index;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class DescriptorIndexCollectorTest {

  private static final String LAST_MODIFIED = "2026-05-09 00:00";

  @TempDir
  Path tempDir;

  @Test
  void fetchRemoteFilesDownloadsValidRemoteFile() throws IOException {
    Path localDir = tempDir.resolve("local");
    Path remoteDir = tempDir.resolve("remote");
    Files.createDirectories(localDir);
    Files.createDirectories(remoteDir.resolve("recent"));
    byte[] contents = "descriptor".getBytes(StandardCharsets.UTF_8);
    Files.write(remoteDir.resolve("recent").resolve("relay.txt"), contents);

    SortedMap<String, FileNode> remotes = new TreeMap<>();
    remotes.put("recent/relay.txt",
        new FileNode("relay.txt", contents.length, LAST_MODIFIED));

    boolean fetched = new DescriptorIndexCollector().fetchRemoteFiles(
        remoteDir.toUri().toString(),
        remotes,
        0L,
        localDir.toFile(),
        new TreeMap<>());

    assertTrue(fetched);
    Path destination = localDir.resolve("recent").resolve("relay.txt");
    assertTrue(Files.exists(destination));
    assertArrayEquals(contents, Files.readAllBytes(destination));
    assertEquals(contents.length, Files.size(destination));
  }

  @Test
  void fetchRemoteFilesDeletesTemporaryFileWhenSizeDiffersFromIndex()
      throws IOException {
    Path localDir = tempDir.resolve("local");
    Path remoteDir = tempDir.resolve("remote");
    Files.createDirectories(localDir);
    Files.createDirectories(remoteDir.resolve("archive"));
    byte[] contents = "descriptor".getBytes(StandardCharsets.UTF_8);
    Files.write(remoteDir.resolve("archive").resolve("relay.tar.xz"), contents);

    SortedMap<String, FileNode> remotes = new TreeMap<>();
    remotes.put("archive/relay.tar.xz",
        new FileNode("relay.tar.xz", contents.length + 1, LAST_MODIFIED));

    boolean fetched = new DescriptorIndexCollector().fetchRemoteFiles(
        remoteDir.toUri().toString(),
        remotes,
        0L,
        localDir.toFile(),
        new TreeMap<>());

    assertTrue(fetched);
    assertFalse(Files.exists(localDir.resolve("archive").resolve("relay.tar.xz")));
    assertFalse(Files.exists(localDir.resolve("archive").resolve(".relay.tar.xz")));
  }

  @Test
  void fetchRemoteFilesSkipsFilesResolvingOutsideLocalDirectoryViaSymlink()
      throws IOException {
    Path localDir = tempDir.resolve("local");
    Path outsideDir = tempDir.resolve("outside");
    Path remoteDir = tempDir.resolve("remote");
    Files.createDirectories(localDir);
    Files.createDirectories(outsideDir);
    Files.createDirectories(remoteDir.resolve("recent"));
    byte[] contents = "evil".getBytes(StandardCharsets.UTF_8);
    Files.write(remoteDir.resolve("recent").resolve("escape.txt"), contents);

    try {
      Files.createSymbolicLink(localDir.resolve("recent"), outsideDir);
    } catch (UnsupportedOperationException | IOException ex) {
      Assumptions.assumeTrue(false, "Symlinks are not supported in this environment");
    }

    SortedMap<String, FileNode> remotes = new TreeMap<>();
    remotes.put("recent/escape.txt",
        new FileNode("escape.txt", contents.length, LAST_MODIFIED));

    boolean fetched = new DescriptorIndexCollector().fetchRemoteFiles(
        remoteDir.toUri().toString(),
        remotes,
        0L,
        localDir.toFile(),
        new TreeMap<>());

    assertTrue(fetched);
    assertFalse(Files.exists(outsideDir.resolve("escape.txt")));
    assertFalse(Files.exists(localDir.resolve("recent").resolve("escape.txt")));
  }

  private Path writeRemoteFile(Path remoteDir, byte[] contents) throws IOException {
    Files.createDirectories(remoteDir.resolve("archive"));
    return Files.write(remoteDir.resolve("archive").resolve("big.tar.xz"), contents);
  }

  private boolean fetchBigFile(Path remoteDir, Path localDir, long indexSize,
      long warnBytes, long maxBytes) throws IOException {
    Files.createDirectories(localDir);
    SortedMap<String, FileNode> remotes = new TreeMap<>();
    remotes.put("archive/big.tar.xz", new FileNode("big.tar.xz", indexSize, LAST_MODIFIED));
    return new DescriptorIndexCollector(warnBytes, maxBytes).fetchRemoteFiles(
        remoteDir.toUri().toString(), remotes, 0L, localDir.toFile(), new TreeMap<>());
  }

  @Test
  void fetchRemoteFilesStoresFileAboveWarningSize() throws IOException {
    Path remoteDir = tempDir.resolve("remote");
    Path localDir = tempDir.resolve("local");
    byte[] contents = new byte[200];
    writeRemoteFile(remoteDir, contents);

    assertTrue(fetchBigFile(remoteDir, localDir, contents.length, 100, 1000));

    assertArrayEquals(contents, Files.readAllBytes(localDir.resolve("archive").resolve("big.tar.xz")));
  }

  @Test
  void fetchRemoteFilesSkipsFileWhoseIndexSizeExceedsLimit() throws IOException {
    Path remoteDir = tempDir.resolve("remote");
    Path localDir = tempDir.resolve("local");
    writeRemoteFile(remoteDir, new byte[2000]);

    assertTrue(fetchBigFile(remoteDir, localDir, 2000, 100, 1000));

    assertFalse(Files.exists(localDir.resolve("archive")));
  }

  @Test
  void fetchRemoteFilesStopsDownloadThatExceedsLimitDespiteSmallIndexSize() throws IOException {
    Path remoteDir = tempDir.resolve("remote");
    Path localDir = tempDir.resolve("local");
    writeRemoteFile(remoteDir, new byte[200_000]);

    assertTrue(fetchBigFile(remoteDir, localDir, 500, 100, 1000));

    Path archiveDir = localDir.resolve("archive");
    assertFalse(Files.exists(archiveDir.resolve("big.tar.xz")));
    assertFalse(Files.exists(archiveDir.resolve(".big.tar.xz")));
  }
}
