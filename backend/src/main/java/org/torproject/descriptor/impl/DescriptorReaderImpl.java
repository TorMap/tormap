/* Copyright 2011--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.torproject.descriptor.Descriptor;
import org.torproject.descriptor.DescriptorParser;
import org.torproject.descriptor.DescriptorReader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class DescriptorReaderImpl implements DescriptorReader {

  private static final Logger logger = LoggerFactory.getLogger(
      DescriptorReaderImpl.class);

  private boolean hasStartedReading = false;

  private File manualSaveHistoryFile;

  @Override
  public void setHistoryFile(File historyFile) {
    if (this.hasStartedReading) {
      throw new IllegalStateException("Reconfiguration is not permitted "
          + "after starting to read.");
    }
    this.manualSaveHistoryFile = historyFile;
  }

  private SortedMap<String, Long> excludedFiles;

  @Override
  public void setExcludedFiles(SortedMap<String, Long> excludedFiles) {
    if (this.hasStartedReading) {
      throw new IllegalStateException("Reconfiguration is not permitted "
          + "after starting to read.");
    }
    this.excludedFiles = excludedFiles;
  }

  @Override
  public SortedMap<String, Long> getExcludedFiles() {
    if (this.reader == null || !this.reader.hasFinishedReading) {
      throw new IllegalStateException("Operation is not permitted before "
          + "finishing to read.");
    }
    return new TreeMap<>(this.reader.excludedFilesAfter);
  }

  @Override
  public SortedMap<String, Long> getParsedFiles() {
    if (this.reader == null || !this.reader.hasFinishedReading) {
      throw new IllegalStateException("Operation is not permitted before "
          + "finishing to read.");
    }
    return new TreeMap<>(this.reader.parsedFilesAfter);
  }

  private int maxDescriptorsInQueue = 100;

  @Override
  public void setMaxDescriptorsInQueue(int maxDescriptorsInQueue) {
    if (this.hasStartedReading) {
      throw new IllegalStateException("Reconfiguration is not permitted "
          + "after starting to read.");
    }
    this.maxDescriptorsInQueue = maxDescriptorsInQueue;
  }

  private DescriptorReaderRunnable reader;

  @Override
  public Iterable<Descriptor> readDescriptors(File... descriptorFiles) {
    if (this.hasStartedReading) {
      throw new IllegalStateException("Initiating reading is only "
          + "permitted once.");
    }
    this.hasStartedReading = true;
    BlockingIteratorImpl<Descriptor> descriptorQueue =
        new BlockingIteratorImpl<>(this.maxDescriptorsInQueue);
    this.reader = new DescriptorReaderRunnable(descriptorFiles, descriptorQueue,
        this.manualSaveHistoryFile, this.excludedFiles);
    Thread readerThread = new Thread(this.reader);
    readerThread.setDaemon(true);
    readerThread.start();
    return descriptorQueue;
  }

  @Override
  public void saveHistoryFile(File historyFile) {
    if (!this.reader.hasFinishedReading) {
      throw new IllegalStateException("Saving history is only permitted after "
          + "reading descriptors.");
    }
    this.reader.writeNewHistory(historyFile);
  }

  private static class DescriptorReaderRunnable implements Runnable {

    private File[] descriptorFiles;

    private BlockingIteratorImpl<Descriptor> descriptorQueue;

    private File manualSaveHistoryFile;

    private List<File> tarballs = new ArrayList<>();

    private SortedMap<String, Long> excludedFilesBefore = new TreeMap<>();

    private SortedMap<String, Long> excludedFilesAfter = new TreeMap<>();

    private SortedMap<String, Long> parsedFilesAfter = new TreeMap<>();

    private DescriptorParser descriptorParser;

    private boolean hasFinishedReading = false;

    private DescriptorReaderRunnable(File[] descriptorFiles,
        BlockingIteratorImpl<Descriptor> descriptorQueue,
        File manualSaveHistoryFile, SortedMap<String, Long> excludedFiles) {
      this.descriptorFiles = descriptorFiles;
      this.descriptorQueue = descriptorQueue;
      this.manualSaveHistoryFile = manualSaveHistoryFile;
      if (excludedFiles != null) {
        this.excludedFilesBefore = excludedFiles;
      }
      this.descriptorParser = new DescriptorParserImpl();
    }

    public void run() {
      try {
        this.readOldHistory(this.manualSaveHistoryFile);
        this.readDescriptorFiles();
        this.readTarballs();
        this.hasFinishedReading = true;
      } catch (Throwable t) {
        logger.error("Bug: uncaught exception or error while reading "
            + "descriptors.", t);
      } finally {
        if (null != this.descriptorQueue) {
          this.descriptorQueue.setOutOfDescriptors();
        }
      }
    }

    private void readOldHistory(File historyFile) {
      if (historyFile == null || !historyFile.exists()) {
        return;
      }
      List<String> lines;
      try {
        lines = Files.readAllLines(historyFile.toPath(),
            StandardCharsets.UTF_8);
        for (String line : lines) {
          if (!line.contains(" ")) {
            logger.warn("Unexpected line structure in old history: {}", line);
            continue;
          }
          long lastModifiedMillis = Long.parseLong(line.substring(0,
              line.indexOf(" ")));
          String absolutePath = line.substring(line.indexOf(" ") + 1);
          this.excludedFilesBefore.put(absolutePath, lastModifiedMillis);
        }
      } catch (IOException | NumberFormatException e) {
        logger.warn("Trouble reading given history file {}.", historyFile, e);
      }
    }

    private void writeNewHistory(File historyFile) {
      if (historyFile == null) {
        return;
      }
      if (historyFile.getParentFile() != null) {
        historyFile.getParentFile().mkdirs();
      }
      try (BufferedWriter bw = Files.newBufferedWriter(historyFile.toPath(),
          StandardCharsets.UTF_8)) {
        SortedMap<String, Long> newHistory = new TreeMap<>();
        newHistory.putAll(this.excludedFilesAfter);
        newHistory.putAll(this.parsedFilesAfter);
        for (Map.Entry<String, Long> e : newHistory.entrySet()) {
          String absolutePath = e.getKey();
          String lastModifiedMillis = String.valueOf(e.getValue());
          bw.write(lastModifiedMillis + " " + absolutePath);
          bw.newLine();
        }
      } catch (IOException e) {
        logger.warn("Trouble writing new history file '{}'.",
            historyFile, e);
      }
    }

    private void readDescriptorFiles() {
      if (null == this.descriptorFiles) {
        return;
      }
      Stack<File> files = new Stack<>();
      for (File descriptorFile : this.descriptorFiles) {
        if (!descriptorFile.exists()) {
          continue;
        }
        files.add(descriptorFile);
        while (!files.isEmpty()) {
          File file = files.pop();
          try {
            String absolutePath = file.getAbsolutePath();
            long lastModifiedMillis = file.lastModified();
            if (this.excludedFilesBefore.getOrDefault(absolutePath, 0L)
                == lastModifiedMillis) {
              this.excludedFilesAfter.put(absolutePath, lastModifiedMillis);
              continue;
            }
            if (file.isDirectory()) {
              files.addAll(Arrays.asList(file.listFiles()));
              continue;
            } else if (file.getName().endsWith(".tar")
                || file.getName().endsWith(".tar.bz2")
                || file.getName().endsWith(".tar.xz")) {
              tarballs.add(file);
              continue;
            } else {
              this.readDescriptorFile(file);
            }
            this.parsedFilesAfter.put(absolutePath, lastModifiedMillis);
          } catch (IOException e) {
            logger.warn("Unable to read descriptor file {}.", file, e);
          }
        }
      }
    }

    private void readTarballs() {
      if (this.tarballs.isEmpty()) {
        return;
      }
      long total = 0L;
      for (File tarball : this.tarballs) {
        total += tarball.length();
      }
      long progress = 0L;
      for (File tarball : this.tarballs) {
        try {
          this.readTarball(tarball);
          this.parsedFilesAfter.put(tarball.getAbsolutePath(),
              tarball.lastModified());
        } catch (IOException e) {
          logger.warn("Unable to read tarball {}.", tarball, e);
        }
        long previousPercentDone = 100L * progress / total;
        progress += tarball.length();
        long percentDone = 100L * progress / total;
        if (percentDone > previousPercentDone) {
          logger.info("Finished reading {}% of tarball bytes.",
              percentDone);
        }
      }
    }

    private void readTarball(File file) throws IOException {
      try (FileInputStream in = new FileInputStream(file)) {
        if (in.available() <= 0) {
          return;
        }
        TarArchiveInputStream tais;
        if (file.getName().endsWith(".tar.bz2")) {
          tais = new TarArchiveInputStream(new BZip2CompressorInputStream(in));
        } else if (file.getName().endsWith(".tar.xz")) {
          tais = new TarArchiveInputStream(new XZCompressorInputStream(in));
        } else if (file.getName().endsWith(".tar")) {
          tais = new TarArchiveInputStream(in);
        } else {
          return;
        }
        try (BufferedInputStream bis = new BufferedInputStream(tais)) {
          TarArchiveEntry tae;
          while ((tae = tais.getNextTarEntry()) != null) {
            if (tae.isDirectory()) {
              continue;
            }
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
              int len;
              byte[] data = new byte[1024];
              while ((len = bis.read(data, 0, 1024)) >= 0) {
                baos.write(data, 0, len);
              }
              byte[] rawDescriptorBytes = baos.toByteArray();
              if (rawDescriptorBytes.length < 1) {
                continue;
              }
              String fileName = tae.getName().substring(
                      tae.getName().lastIndexOf("/") + 1);
              for (Descriptor descriptor :
                      this.descriptorParser.parseDescriptors(
                      rawDescriptorBytes, file, fileName)) {
                this.descriptorQueue.add(descriptor);
              }
            }
          }
        }
      }
    }

    private void readDescriptorFile(File file) throws IOException {
      try (FileInputStream fis = new FileInputStream(file)) {
        InputStream is = fis;
        if (file.getName().endsWith(".gz")) {
          is = new GzipCompressorInputStream(fis);
        }
        byte[] rawDescriptorBytes = IOUtils.toByteArray(is);
        if (rawDescriptorBytes.length > 0) {
          for (Descriptor descriptor : this.descriptorParser.parseDescriptors(
              rawDescriptorBytes, file, file.getName())) {
            this.descriptorQueue.add(descriptor);
          }
        }
      }
    }
  }
}

