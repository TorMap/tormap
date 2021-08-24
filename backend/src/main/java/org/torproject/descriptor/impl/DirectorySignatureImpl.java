/* Copyright 2012--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.DescriptorParseException;
import org.torproject.descriptor.DirectorySignature;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.torproject.descriptor.impl.DescriptorImpl.NL;
import static org.torproject.descriptor.impl.DescriptorImpl.SP;

public class DirectorySignatureImpl implements DirectorySignature {

  private static final long serialVersionUID = -1084841439595622290L;

  private DescriptorImpl parent;

  private int offset;

  private int length;

  private List<String> unrecognizedLines;

  protected List<String> getAndClearUnrecognizedLines() {
    List<String> lines = this.unrecognizedLines;
    this.unrecognizedLines = null;
    return lines;
  }

  protected DirectorySignatureImpl(DescriptorImpl parent, int offset,
      int length) throws DescriptorParseException {
    this.parent = parent;
    this.offset = offset;
    this.length = length;
    this.parseDirectorySignatureBytes();
  }

  private void parseDirectorySignatureBytes()
      throws DescriptorParseException {
    Scanner scanner = this.parent.newScanner(this.offset, this.length)
        .useDelimiter(NL);
    StringBuilder crypto = null;
    while (scanner.hasNext()) {
      String line = scanner.next();
      String[] parts = line.split(SP, -1);
      Key key = Key.get(parts[0]);
      switch (key) {
        case DIRECTORY_SIGNATURE:
          int algorithmOffset = 0;
          switch (parts.length) {
            case 4:
              this.algorithm = parts[1];
              algorithmOffset = 1;
              break;
            case 3:
              break;
            default:
              throw new DescriptorParseException("Illegal line '" + line
                  + "'.");
          }
          this.identity = ParseHelper.parseHexString(line,
              parts[1 + algorithmOffset]);
          this.signingKeyDigest = ParseHelper.parseHexString(
              line, parts[2 + algorithmOffset]);
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
          this.signature = cryptoString;
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

  static final String DEFAULT_ALGORITHM = "sha1";

  private String algorithm;

  @Override
  public String getAlgorithm() {
    return this.algorithm == null ? DEFAULT_ALGORITHM : this.algorithm;
  }

  private String identity;

  @Override
  public String getIdentity() {
    return this.identity;
  }

  private String signingKeyDigest;

  @Override
  public String getSigningKeyDigestSha1Hex() {
    return this.signingKeyDigest;
  }

  private String signature;

  @Override
  public String getSignature() {
    return this.signature;
  }
}

