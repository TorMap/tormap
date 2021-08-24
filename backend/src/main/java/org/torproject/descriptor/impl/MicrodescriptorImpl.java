/* Copyright 2014--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.torproject.descriptor.DescriptorParseException;
import org.torproject.descriptor.Microdescriptor;

import java.io.File;
import java.util.*;

/* Contains a microdescriptor. */
public class MicrodescriptorImpl extends DescriptorImpl
    implements Microdescriptor {

  private static final long serialVersionUID = 7792584185486747094L;

  protected MicrodescriptorImpl(byte[] descriptorBytes, int[] offsetAndLength,
      File descriptorFile)
      throws DescriptorParseException {
    super(descriptorBytes, offsetAndLength, descriptorFile, false);
    this.parseDescriptorBytes();
    this.calculateDigestSha256Base64(Key.ONION_KEY.keyword + NL);
    this.convertDigestSha256Base64ToHex();
    this.checkExactlyOnceKeys(EnumSet.of(Key.ONION_KEY));
    Set<Key> atMostOnceKeys = EnumSet.of(
        Key.NTOR_ONION_KEY, Key.FAMILY, Key.P, Key.P6, Key.ID);
    this.checkAtMostOnceKeys(atMostOnceKeys);
    this.checkFirstKey(Key.ONION_KEY);
    this.clearParsedKeys();
  }

  private void parseDescriptorBytes() throws DescriptorParseException {
    Scanner scanner = this.newScanner().useDelimiter(NL);
    Key nextCrypto = Key.EMPTY;
    StringBuilder crypto = null;
    while (scanner.hasNext()) {
      String line = scanner.next();
      if (line.startsWith("@")) {
        continue;
      }
      String[] parts = line.split("[ \t]+");
      Key key = Key.get(parts[0]);
      switch (key) {
        case ONION_KEY:
          this.parseOnionKeyLine(line, parts);
          nextCrypto = key;
          break;
        case NTOR_ONION_KEY:
          this.parseNtorOnionKeyLine(line, parts);
          break;
        case A:
          this.parseALine(line, parts);
          break;
        case FAMILY:
          this.parseFamilyLine(line, parts);
          break;
        case P:
          this.parsePLine(line, parts);
          break;
        case P6:
          this.parseP6Line(line, parts);
          break;
        case ID:
          this.parseIdLine(line, parts);
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
          if (nextCrypto.equals(Key.ONION_KEY)) {
            this.onionKey = cryptoString;
          } else {
            throw new DescriptorParseException("Unrecognized crypto "
                + "block in microdescriptor.");
          }
          nextCrypto = Key.EMPTY;
          break;
        default:
          if (crypto != null) {
            crypto.append(line).append(NL);
          } else {
            ParseHelper.parseKeyword(line, parts[0]);
            if (this.unrecognizedLines == null) {
              this.unrecognizedLines = new ArrayList<>();
            }
            this.unrecognizedLines.add(line);
          }
      }
    }
  }

  private void parseOnionKeyLine(String line, String[] parts)
      throws DescriptorParseException {
    if (!line.equals("onion-key")) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
  }

  private void parseNtorOnionKeyLine(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length != 2) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    this.ntorOnionKey = parts[1].replaceAll("=", "");
  }

  private void parseALine(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length != 2) {
      throw new DescriptorParseException("Wrong number of values in line "
          + "'" + line + "'.");
    }
    /* TODO Add more checks. */
    this.orAddresses.add(parts[1]);
  }

  private void parseFamilyLine(String line, String[] parts)
      throws DescriptorParseException {
    String[] familyEntries = new String[parts.length - 1];
    for (int i = 1; i < parts.length; i++) {
      if (parts[i].startsWith("$")) {
        if (parts[i].contains("=") ^ parts[i].contains("~")) {
          String separator = parts[i].contains("=") ? "=" : "~";
          String fingerprint = ParseHelper.parseTwentyByteHexString(line,
              parts[i].substring(1, parts[i].indexOf(separator)));
          String nickname = ParseHelper.parseNickname(line,
              parts[i].substring(parts[i].indexOf(separator) + 1));
          familyEntries[i - 1] = "$" + fingerprint + separator + nickname;
        } else {
          familyEntries[i - 1] = "$"
              + ParseHelper.parseTwentyByteHexString(line,
              parts[i].substring(1));
        }
      } else {
        familyEntries[i - 1] = ParseHelper.parseNickname(line, parts[i]);
      }
    }
    this.familyEntries = familyEntries;
  }

  private void parsePLine(String line, String[] parts)
      throws DescriptorParseException {
    this.validatePOrP6Line(line, parts);
    this.defaultPolicy = parts[1];
    this.portList = parts[2];
  }

  private void parseP6Line(String line, String[] parts)
      throws DescriptorParseException {
    this.validatePOrP6Line(line, parts);
    this.ipv6DefaultPolicy = parts[1];
    this.ipv6PortList = parts[2];
  }

  private void validatePOrP6Line(String line, String[] parts)
      throws DescriptorParseException {
    boolean isValid = true;
    if (parts.length != 3) {
      isValid = false;
    } else  {
      switch (parts[1]) {
        case "accept":
        case "reject":
          String[] ports = parts[2].split(",", -1);
          for (String port : ports) {
            if (port.length() < 1) {
              isValid = false;
              break;
            }
          }
          break;
        default:
          isValid = false;
      }
    }
    if (!isValid) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
  }

  private void parseIdLine(String line, String[] parts)
      throws DescriptorParseException {
    if (parts.length != 3) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    } else {
      switch (parts[1]) {
        case "ed25519":
          ParseHelper.verifyThirtyTwoByteBase64String(line, parts[2]);
          this.ed25519Identity = parts[2];
          break;
        case "rsa1024":
          ParseHelper.verifyTwentyByteBase64String(line, parts[2]);
          this.rsa1024Identity = parts[2];
          break;
        default:
          throw new DescriptorParseException("Illegal line '" + line
              + "'.");
      }
    }
  }

  private void convertDigestSha256Base64ToHex() {
    this.digestSha256Hex = Hex.encodeHexString(Base64.decodeBase64(
        this.getDigestSha256Base64()));
  }

  private String digestSha256Hex;

  @Override
  public String getDigestSha256Hex() {
    return this.digestSha256Hex;
  }

  private String onionKey;

  @Override
  public String getOnionKey() {
    return this.onionKey;
  }

  private String ntorOnionKey;

  @Override
  public String getNtorOnionKey() {
    return this.ntorOnionKey;
  }

  private List<String> orAddresses = new ArrayList<>();

  @Override
  public List<String> getOrAddresses() {
    return new ArrayList<>(this.orAddresses);
  }

  private String[] familyEntries;

  @Override
  public List<String> getFamilyEntries() {
    return this.familyEntries == null ? null
        : Arrays.asList(this.familyEntries);
  }

  private String defaultPolicy;

  @Override
  public String getDefaultPolicy() {
    return this.defaultPolicy;
  }

  private String portList;

  @Override
  public String getPortList() {
    return this.portList;
  }

  private String ipv6DefaultPolicy;

  @Override
  public String getIpv6DefaultPolicy() {
    return this.ipv6DefaultPolicy;
  }

  private String ipv6PortList;

  @Override
  public String getIpv6PortList() {
    return this.ipv6PortList;
  }

  private String rsa1024Identity;

  @Override
  public String getRsa1024Identity() {
    return this.rsa1024Identity;
  }

  private String ed25519Identity;

  @Override
  public String getEd25519Identity() {
    return this.ed25519Identity;
  }
}

