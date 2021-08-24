/* Copyright 2012--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.DescriptorParseException;
import org.torproject.descriptor.RelayDirectory;
import org.torproject.descriptor.RouterStatusEntry;
import org.torproject.descriptor.ServerDescriptor;

import java.io.File;
import java.util.*;

public class RelayDirectoryImpl extends DescriptorImpl
    implements RelayDirectory {

  private static final long serialVersionUID = -6770225160489757961L;

  protected RelayDirectoryImpl(byte[] directoryBytes, int[] offsetAndLength,
      File descriptorFile) throws DescriptorParseException {
    super(directoryBytes, offsetAndLength, descriptorFile, true);
    this.splitAndParseParts();
    this.calculateDigestSha1Hex(Key.SIGNED_DIRECTORY.keyword + NL,
        NL + Key.DIRECTORY_SIGNATURE.keyword + SP);
    Set<Key> exactlyOnceKeys = EnumSet.of(
        Key.SIGNED_DIRECTORY, Key.RECOMMENDED_SOFTWARE,
        Key.DIRECTORY_SIGNATURE);
    this.checkExactlyOnceKeys(exactlyOnceKeys);
    Set<Key> atMostOnceKeys = EnumSet.of(
        Key.DIR_SIGNING_KEY, Key.RUNNING_ROUTERS, Key.ROUTER_STATUS);
    this.checkAtMostOnceKeys(atMostOnceKeys);
    this.checkFirstKey(Key.SIGNED_DIRECTORY);
    this.clearParsedKeys();
  }

  private void splitAndParseParts() throws DescriptorParseException {
    int startIndex = 0;
    int firstRouterIndex = this.findFirstIndexOfKey(Key.ROUTER);
    int directorySignatureIndex = this.findFirstIndexOfKey(
        Key.DIRECTORY_SIGNATURE);
    int endIndex = this.offset + this.length;
    if (directorySignatureIndex < 0) {
      directorySignatureIndex = endIndex;
    }
    if (firstRouterIndex < 0) {
      firstRouterIndex = directorySignatureIndex;
    }
    if (firstRouterIndex > startIndex) {
      this.parseHeader(startIndex, firstRouterIndex - startIndex);
    }
    if (directorySignatureIndex > firstRouterIndex) {
      this.parseServerDescriptors(firstRouterIndex,
          directorySignatureIndex - firstRouterIndex);
    }
    if (endIndex > directorySignatureIndex) {
      this.parseDirectorySignatures(directorySignatureIndex,
          endIndex - directorySignatureIndex);
    }
  }

  private void parseServerDescriptors(int offset, int length) {
    List<int[]> offsetsAndLengths = this.splitByKey(Key.ROUTER, offset, length,
        true);
    for (int[] offsetAndLength : offsetsAndLengths) {
      this.parseServerDescriptor(offsetAndLength[0], offsetAndLength[1]);
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

  private void parseHeader(int offset, int length)
      throws DescriptorParseException {
    Scanner scanner = this.newScanner(offset, length).useDelimiter(NL);
    String publishedLine = null;
    Key nextCrypto = Key.EMPTY;
    String runningRoutersLine = null;
    String routerStatusLine = null;
    StringBuilder crypto = null;
    while (scanner.hasNext()) {
      String line = scanner.next();
      if (line.isEmpty() || line.startsWith("@")) {
        continue;
      }
      String lineNoOpt = line.startsWith(Key.OPT.keyword + SP)
          ? line.substring(Key.OPT.keyword.length() + 1) : line;
      String[] partsNoOpt = lineNoOpt.split("[ \t]+");
      Key key = Key.get(partsNoOpt[0]);
      switch (key) {
        case SIGNED_DIRECTORY:
          this.parseSignedDirectoryLine(line, lineNoOpt);
          break;
        case PUBLISHED:
          if (publishedLine != null) {
            throw new DescriptorParseException("Keyword 'published' is "
                + "contained more than once, but must be contained "
                + "exactly once.");
          } else {
            publishedLine = line;
          }
          break;
        case DIR_SIGNING_KEY:
          this.parseDirSigningKeyLine(line, partsNoOpt);
          nextCrypto = key;
          break;
        case RECOMMENDED_SOFTWARE:
          this.parseRecommendedSoftwareLine(line, partsNoOpt);
          break;
        case RUNNING_ROUTERS:
          runningRoutersLine = line;
          break;
        case ROUTER_STATUS:
          routerStatusLine = line;
          break;
        case CRYPTO_BEGIN:
          crypto = new StringBuilder();
          crypto.append(line).append(NL);
          break;
        case CRYPTO_END:
          if (null == crypto) {
            throw new DescriptorParseException(Key.CRYPTO_END + " before "
                + Key.CRYPTO_BEGIN);
          }
          crypto.append(line).append(NL);
          String cryptoString = crypto.toString();
          crypto = null;
          if (nextCrypto.equals(Key.DIR_SIGNING_KEY)
              && this.dirSigningKey == null) {
            this.dirSigningKey = cryptoString;
          } else {
            throw new DescriptorParseException("Unrecognized crypto "
                + "block in v1 directory.");
          }
          nextCrypto = Key.EMPTY;
          break;
        default:
          if (crypto != null) {
            crypto.append(line).append(NL);
          } else {
            if (this.unrecognizedLines == null) {
              this.unrecognizedLines = new ArrayList<>();
            }
            this.unrecognizedLines.add(line);
          }
      }
    }
    if (publishedLine == null) {
      throw new DescriptorParseException("Keyword 'published' is "
          + "contained 0 times, but must be contained exactly once.");
    } else {
      String publishedLineNoOpt = publishedLine.startsWith(Key.OPT.keyword + SP)
          ? publishedLine.substring(Key.OPT.keyword.length() + 1)
          : publishedLine;
      String[] publishedPartsNoOpt = publishedLineNoOpt.split("[ \t]+");
      this.parsePublishedLine(publishedLine,
          publishedPartsNoOpt);
    }
    if (routerStatusLine != null) {
      String routerStatusLineNoOpt =
          routerStatusLine.startsWith(Key.OPT.keyword + SP)
          ? routerStatusLine.substring(Key.OPT.keyword.length() + 1)
          : routerStatusLine;
      String[] routerStatusPartsNoOpt =
          routerStatusLineNoOpt.split("[ \t]+");
      this.parseRouterStatusLine(
          routerStatusPartsNoOpt);
    } else if (runningRoutersLine != null) {
      String runningRoutersLineNoOpt =
          runningRoutersLine.startsWith(Key.OPT.keyword + SP)
          ? runningRoutersLine.substring(Key.OPT.keyword.length() + 1)
          : runningRoutersLine;
      String[] runningRoutersPartsNoOpt =
          runningRoutersLineNoOpt.split("[ \t]+");
      this.parseRunningRoutersLine(
          runningRoutersPartsNoOpt);
    } else {
      throw new DescriptorParseException("Either running-routers or "
          + "router-status line must be given.");
    }
  }

  protected void parseServerDescriptor(int offset, int length) {
    try {
      ServerDescriptorImpl serverDescriptor =
          new RelayServerDescriptorImpl(this.rawDescriptorBytes,
          new int[] { offset, length }, this.getDescriptorFile());
      this.serverDescriptors.add(serverDescriptor);
    } catch (DescriptorParseException e) {
      this.serverDescriptorParseExceptions.add(e);
    }
  }

  private void parseDirectorySignature(int offset, int length)
      throws DescriptorParseException {
    Scanner scanner = this.newScanner(offset, length).useDelimiter(NL);
    Key nextCrypto = Key.EMPTY;
    StringBuilder crypto = null;
    while (scanner.hasNext()) {
      String line = scanner.next();
      String lineNoOpt = line.startsWith(Key.OPT.keyword + SP)
          ? line.substring(Key.OPT.keyword.length() + 1) : line;
      String[] partsNoOpt = lineNoOpt.split("[ \t]+");
      Key key = Key.get(partsNoOpt[0]);
      switch (key) {
        case DIRECTORY_SIGNATURE:
          this.parseDirectorySignatureLine(line, partsNoOpt);
          nextCrypto = key;
          break;
        case CRYPTO_BEGIN:
          crypto = new StringBuilder();
          crypto.append(line).append(NL);
          break;
        case CRYPTO_END:
          if (null == crypto) {
            throw new DescriptorParseException(Key.CRYPTO_END + " before "
                + Key.CRYPTO_BEGIN);
          }
          crypto.append(line).append(NL);
          String cryptoString = crypto.toString();
          crypto = null;
          if (nextCrypto.equals(Key.DIRECTORY_SIGNATURE)) {
            this.directorySignature = cryptoString;
          } else {
            throw new DescriptorParseException("Unrecognized crypto "
                + "block in v2 network status.");
          }
          nextCrypto = Key.EMPTY;
          break;
        default:
          if (crypto != null) {
            crypto.append(line).append(NL);
          } else {
            if (this.unrecognizedLines == null) {
              this.unrecognizedLines = new ArrayList<>();
            }
            this.unrecognizedLines.add(line);
          }
      }
    }
  }

  private void parseSignedDirectoryLine(String line, String lineNoOpt)
      throws DescriptorParseException {
    if (!lineNoOpt.equals(Key.SIGNED_DIRECTORY.keyword)) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
  }

  private void parsePublishedLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.publishedMillis = ParseHelper.parseTimestampAtIndex(line,
        partsNoOpt, 1, 2);
  }

  private void parseDirSigningKeyLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    if (partsNoOpt.length > 2) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    } else if (partsNoOpt.length == 2) {
      /* Early directories didn't have a crypto object following the
       * "dir-signing-key" line, but had the key base64-encoded in the
       * same line. */
      StringBuilder sb = new StringBuilder();
      sb.append("-----BEGIN RSA PUBLIC KEY-----\n");
      String keyString = partsNoOpt[1];
      while (keyString.length() > 64) {
        sb.append(keyString, 0, 64).append(NL);
        keyString = keyString.substring(64);
      }
      if (keyString.length() > 0) {
        sb.append(keyString).append(NL);
      }
      sb.append("-----END RSA PUBLIC KEY-----\n");
      this.dirSigningKey = sb.toString();
    }
  }

  private void parseRecommendedSoftwareLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    List<String> result = new ArrayList<>();
    if (partsNoOpt.length > 2) {
      throw new DescriptorParseException("Illegal versions line '" + line
          + "'.");
    } else if (partsNoOpt.length == 2) {
      String[] versions = partsNoOpt[1].split(",", -1);
      for (String version : versions) {
        if (version.length() < 1) {
          throw new DescriptorParseException("Illegal versions line '"
              + line + "'.");
        }
        result.add(version);
      }
    }
    this.recommendedSoftware = result;
  }

  private void parseRunningRoutersLine(String[] partsNoOpt)
      throws DescriptorParseException {
    for (int i = 1; i < partsNoOpt.length; i++) {
      String part = partsNoOpt[i];
      String debugLine = "running-routers [...] " + part + " [...]";
      boolean isLive = true;
      if (part.startsWith("!")) {
        isLive = false;
        part = part.substring(1);
      }
      boolean isVerified;
      String fingerprint = null;
      String nickname = null;
      if (part.startsWith("$")) {
        isVerified = false;
        fingerprint = ParseHelper.parseTwentyByteHexString(debugLine,
            part.substring(1));
      } else {
        isVerified = true;
        nickname = ParseHelper.parseNickname(debugLine, part);
      }
      this.statusEntries.add(new RouterStatusEntryImpl(fingerprint,
          nickname, isLive, isVerified));
    }
  }

  private void parseRouterStatusLine(String[] partsNoOpt)
      throws DescriptorParseException {
    for (int i = 1; i < partsNoOpt.length; i++) {
      String part = partsNoOpt[i];
      String debugLine = "router-status [...] " + part + " [...]";
      RouterStatusEntry entry = null;
      if (part.contains("=")) {
        String[] partParts = part.split("=");
        if (partParts.length == 2) {
          boolean isVerified = true;
          boolean isLive;
          String nickname;
          if (partParts[0].startsWith("!")) {
            isLive = false;
            nickname = ParseHelper.parseNickname(debugLine,
                partParts[0].substring(1));
          } else {
            isLive = true;
            nickname = ParseHelper.parseNickname(debugLine, partParts[0]);
          }
          String fingerprint = ParseHelper.parseTwentyByteHexString(
              debugLine, partParts[1].substring(1));
          entry = new RouterStatusEntryImpl(fingerprint, nickname, isLive,
              isVerified);
        }
      } else {
        boolean isVerified = false;
        boolean isLive;
        String nickname = null;
        String fingerprint;
        if (part.startsWith("!")) {
          isLive = false;
          fingerprint = ParseHelper.parseTwentyByteHexString(
              debugLine, part.substring(2));
        } else {
          isLive = true;
          fingerprint = ParseHelper.parseTwentyByteHexString(
              debugLine, part.substring(1));
        }
        entry = new RouterStatusEntryImpl(fingerprint, nickname, isLive,
            isVerified);
      }
      if (entry == null) {
        throw new DescriptorParseException("Illegal router-status entry '"
            + part + "' in v1 directory.");
      }
      this.statusEntries.add(entry);
    }
  }

  private void parseDirectorySignatureLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    if (partsNoOpt.length < 2) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    this.nickname = ParseHelper.parseNickname(line, partsNoOpt[1]);
  }

  private long publishedMillis;

  @Override
  public long getPublishedMillis() {
    return this.publishedMillis;
  }

  private String dirSigningKey;

  @Override
  public String getDirSigningKey() {
    return this.dirSigningKey;
  }

  private List<String> recommendedSoftware;

  @Override
  public List<String> getRecommendedSoftware() {
    return this.recommendedSoftware == null ? null
        : new ArrayList<>(this.recommendedSoftware);
  }

  private String directorySignature;

  @Override
  public String getDirectorySignature() {
    return this.directorySignature;
  }

  private List<RouterStatusEntry> statusEntries = new ArrayList<>();

  @Override
  public List<RouterStatusEntry> getRouterStatusEntries() {
    return new ArrayList<>(this.statusEntries);
  }

  private List<ServerDescriptor> serverDescriptors = new ArrayList<>();

  @Override
  public List<ServerDescriptor> getServerDescriptors() {
    return new ArrayList<>(this.serverDescriptors);
  }

  private List<Exception> serverDescriptorParseExceptions =
      new ArrayList<>();

  @Override
  public List<Exception> getServerDescriptorParseExceptions() {
    return new ArrayList<>(this.serverDescriptorParseExceptions);
  }

  private String nickname;

  @Override
  public String getNickname() {
    return this.nickname;
  }

}

