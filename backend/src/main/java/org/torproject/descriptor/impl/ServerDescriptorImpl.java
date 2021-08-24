/* Copyright 2012--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.BandwidthHistory;
import org.torproject.descriptor.DescriptorParseException;
import org.torproject.descriptor.ServerDescriptor;

import java.io.File;
import java.util.*;

/* Contains a server descriptor. */
public abstract class ServerDescriptorImpl extends DescriptorImpl
    implements ServerDescriptor {

  private static final long serialVersionUID = 5240701284736998121L;

  private static final Set<Key> atMostOnce = EnumSet.of(
      Key.IDENTITY_ED25519, Key.MASTER_KEY_ED25519, Key.PLATFORM, Key.PROTO,
      Key.FINGERPRINT, Key.HIBERNATING, Key.UPTIME, Key.CONTACT, Key.FAMILY,
      Key.READ_HISTORY, Key.WRITE_HISTORY, Key.EVENTDNS, Key.CACHES_EXTRA_INFO,
      Key.EXTRA_INFO_DIGEST, Key.HIDDEN_SERVICE_DIR, Key.OVERLOAD_GENERAL,
      Key.PROTOCOLS, Key.ALLOW_SINGLE_HOP_EXITS, Key.ONION_KEY, Key.SIGNING_KEY,
      Key.IPV6_POLICY, Key.NTOR_ONION_KEY, Key.ONION_KEY_CROSSCERT,
      Key.NTOR_ONION_KEY_CROSSCERT, Key.TUNNELLED_DIR_SERVER,
      Key.ROUTER_SIG_ED25519, Key.ROUTER_SIGNATURE, Key.ROUTER_DIGEST_SHA256,
      Key.ROUTER_DIGEST, Key.BRIDGE_DISTRIBUTION_REQUEST);

  private static final Set<Key> exactlyOnce = EnumSet.of(
      Key.ROUTER, Key.BANDWIDTH, Key.PUBLISHED);

  protected ServerDescriptorImpl(byte[] descriptorBytes, int[] offsetAndLength,
      File descriptorFile) throws DescriptorParseException {
    super(descriptorBytes, offsetAndLength, descriptorFile, false);
    this.parseDescriptorBytes();
    this.checkExactlyOnceKeys(exactlyOnce);
    this.checkAtMostOnceKeys(atMostOnce);
    this.checkFirstKey(Key.ROUTER);
    if (this.getKeyCount(Key.ACCEPT) == 0
        && this.getKeyCount(Key.REJECT) == 0) {
      throw new DescriptorParseException("Either keyword 'accept' or "
          + "'reject' must be contained at least once.");
    }
    this.clearParsedKeys();
  }

  private void parseDescriptorBytes() throws DescriptorParseException {
    Scanner scanner = this.newScanner().useDelimiter(NL);
    Key nextCrypto = Key.EMPTY;
    List<String> cryptoLines = null;
    while (scanner.hasNext()) {
      String line = scanner.next();
      if (line.startsWith("@")) {
        continue;
      }
      String lineNoOpt = line.startsWith(Key.OPT.keyword + SP)
          ? line.substring(Key.OPT.keyword.length() + 1) : line;
      String[] partsNoOpt = lineNoOpt.split("[ \t]+");
      Key key = Key.get(partsNoOpt[0]);
      switch (key) {
        case ROUTER:
          this.parseRouterLine(line, partsNoOpt);
          break;
        case OR_ADDRESS:
          this.parseOrAddressLine(line, partsNoOpt);
          break;
        case BANDWIDTH:
          this.parseBandwidthLine(line, partsNoOpt);
          break;
        case OVERLOAD_GENERAL:
          this.parseOverloadGeneralLine(line, partsNoOpt);
          break;
        case PLATFORM:
          this.parsePlatformLine(lineNoOpt);
          break;
        case PROTO:
          this.parseProtoLine(line, lineNoOpt, partsNoOpt);
          break;
        case PUBLISHED:
          this.parsePublishedLine(line, partsNoOpt);
          break;
        case FINGERPRINT:
          this.parseFingerprintLine(line, lineNoOpt);
          break;
        case HIBERNATING:
          this.parseHibernatingLine(line, partsNoOpt);
          break;
        case UPTIME:
          this.parseUptimeLine(line, partsNoOpt);
          break;
        case ONION_KEY:
          this.parseOnionKeyLine(line, lineNoOpt);
          nextCrypto = key;
          break;
        case SIGNING_KEY:
          this.parseSigningKeyLine(line, lineNoOpt);
          nextCrypto = key;
          break;
        case ACCEPT:
          this.parseAcceptLine(line, lineNoOpt, partsNoOpt);
          break;
        case REJECT:
          this.parseRejectLine(line, lineNoOpt, partsNoOpt);
          break;
        case ROUTER_SIGNATURE:
          this.parseRouterSignatureLine(line, lineNoOpt);
          nextCrypto = key;
          break;
        case CONTACT:
          this.parseContactLine(lineNoOpt);
          break;
        case BRIDGE_DISTRIBUTION_REQUEST:
          this.parseBridgeDistributionRequestLine(line, partsNoOpt);
          break;
        case FAMILY:
          this.parseFamilyLine(line, partsNoOpt);
          break;
        case READ_HISTORY:
          this.parseReadHistoryLine(line, partsNoOpt);
          break;
        case WRITE_HISTORY:
          this.parseWriteHistoryLine(line, partsNoOpt);
          break;
        case EVENTDNS:
          this.parseEventdnsLine(line, partsNoOpt);
          break;
        case CACHES_EXTRA_INFO:
          this.parseCachesExtraInfoLine(line, lineNoOpt);
          break;
        case EXTRA_INFO_DIGEST:
          this.parseExtraInfoDigestLine(line, partsNoOpt);
          break;
        case HIDDEN_SERVICE_DIR:
          this.parseHiddenServiceDirLine();
          break;
        case PROTOCOLS:
          this.parseProtocolsLine(line, partsNoOpt);
          break;
        case ALLOW_SINGLE_HOP_EXITS:
          this.parseAllowSingleHopExitsLine(line, lineNoOpt);
          break;
        case DIRCACHEPORT:
          this.parseDircacheportLine(line, partsNoOpt);
          break;
        case ROUTER_DIGEST:
          this.parseRouterDigestLine(line, partsNoOpt);
          break;
        case ROUTER_DIGEST_SHA256:
          this.parseRouterDigestSha256Line(line, partsNoOpt);
          break;
        case IPV6_POLICY:
          this.parseIpv6PolicyLine(line, partsNoOpt);
          break;
        case NTOR_ONION_KEY:
          this.parseNtorOnionKeyLine(line, partsNoOpt);
          break;
        case IDENTITY_ED25519:
          this.parseIdentityEd25519Line(line, partsNoOpt);
          nextCrypto = key;
          break;
        case MASTER_KEY_ED25519:
          this.parseMasterKeyEd25519Line(line, partsNoOpt);
          break;
        case ROUTER_SIG_ED25519:
          this.parseRouterSigEd25519Line(line, partsNoOpt);
          break;
        case ONION_KEY_CROSSCERT:
          this.parseOnionKeyCrosscert(line, partsNoOpt);
          nextCrypto = key;
          break;
        case NTOR_ONION_KEY_CROSSCERT:
          this.parseNtorOnionKeyCrosscert(line, partsNoOpt);
          nextCrypto = key;
          break;
        case TUNNELLED_DIR_SERVER:
          this.parseTunnelledDirServerLine(line, lineNoOpt);
          break;
        case CRYPTO_BEGIN:
          cryptoLines = new ArrayList<>();
          cryptoLines.add(line);
          break;
        case CRYPTO_END:
          if (null == cryptoLines) {
            throw new DescriptorParseException(Key.CRYPTO_END + " before "
                + Key.CRYPTO_BEGIN);
          }
          cryptoLines.add(line);
          StringBuilder sb = new StringBuilder();
          for (String cryptoLine : cryptoLines) {
            sb.append(NL).append(cryptoLine);
          }
          String cryptoString = sb.toString().substring(1);
          switch (nextCrypto) {
            case ONION_KEY:
              this.onionKey = cryptoString;
              break;
            case SIGNING_KEY:
              this.signingKey = cryptoString;
              break;
            case ROUTER_SIGNATURE:
              this.routerSignature = cryptoString;
              break;
            case IDENTITY_ED25519:
              this.identityEd25519 = cryptoString;
              this.parseIdentityEd25519CryptoBlock(cryptoString);
              break;
            case ONION_KEY_CROSSCERT:
              this.onionKeyCrosscert = cryptoString;
              break;
            case NTOR_ONION_KEY_CROSSCERT:
              this.ntorOnionKeyCrosscert = cryptoString;
              break;
            default:
              if (this.unrecognizedLines == null) {
                this.unrecognizedLines = new ArrayList<>();
              }
              this.unrecognizedLines.addAll(cryptoLines);
          }
          cryptoLines = null;
          nextCrypto = Key.EMPTY;
          break;
        case INVALID:
        default:
          if (cryptoLines != null) {
            cryptoLines.add(line);
          } else {
            ParseHelper.parseKeyword(line, partsNoOpt[0]);
            if (this.unrecognizedLines == null) {
              this.unrecognizedLines = new ArrayList<>();
            }
            this.unrecognizedLines.add(line);
          }
      }
    }
  }

  private void parseRouterLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    if (partsNoOpt.length != 6) {
      throw new DescriptorParseException("Illegal line '" + line
          + "' in server descriptor.");
    }
    this.nickname = ParseHelper.parseNickname(line, partsNoOpt[1]);
    this.address = ParseHelper.parseIpv4Address(line, partsNoOpt[2]);
    this.orPort = ParseHelper.parsePort(line, partsNoOpt[3]);
    this.socksPort = ParseHelper.parsePort(line, partsNoOpt[4]);
    this.dirPort = ParseHelper.parsePort(line, partsNoOpt[5]);
  }

  private void parseOrAddressLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    if (partsNoOpt.length != 2) {
      throw new DescriptorParseException("Wrong number of values in line "
          + "'" + line + "'.");
    }
    /* TODO Add more checks. */
    this.orAddresses.add(partsNoOpt[1]);
  }

  private void parseBandwidthLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    if (partsNoOpt.length < 3 || partsNoOpt.length > 4) {
      throw new DescriptorParseException("Wrong number of values in line "
          + "'" + line + "'.");
    }
    boolean isValid = false;
    try {
      this.bandwidthRate = Integer.parseInt(partsNoOpt[1]);
      this.bandwidthBurst = Integer.parseInt(partsNoOpt[2]);
      if (partsNoOpt.length == 4) {
        this.bandwidthObserved = Integer.parseInt(partsNoOpt[3]);
      }
      if (this.bandwidthRate >= 0 && this.bandwidthBurst >= 0
          && this.bandwidthObserved >= 0) {
        isValid = true;
      }
      if (partsNoOpt.length < 4) {
        /* Tor versions 0.0.8 and older only wrote bandwidth lines with
         * rate and burst values, but no observed value. */
        this.bandwidthObserved = -1;
      }
    } catch (NumberFormatException e) {
      /* Handle below. */
    }
    if (!isValid) {
      throw new DescriptorParseException("Illegal values in line '" + line
          + "'.");
    }
  }

  private void parseOverloadGeneralLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    int overloadGeneralVersion = Integer.parseInt(partsNoOpt[1]);
    if (overloadGeneralVersion != 1) {
      throw new DescriptorParseException("Unknown version number for line '"
              + line + "' in extra-info descriptor.");
    } else {
      this.overloadGeneralVersion = overloadGeneralVersion;
    }
    if (partsNoOpt.length < 4) {
      throw new DescriptorParseException("Missing fields for line '"
              + line + "' in extra-info descriptor.");
    }
    this.overloadGeneralTimestamp = ParseHelper.parseTimestampAtIndex(line,
            partsNoOpt, 2, 3);

  }

  private void parsePlatformLine(String lineNoOpt) {
    if (lineNoOpt.length() > Key.PLATFORM.keyword.length() + 1) {
      this.platform = lineNoOpt.substring(Key.PLATFORM.keyword.length() + 1);
    } else {
      this.platform = "";
    }
  }

  private void parseProtoLine(String line, String lineNoOpt,
      String[] partsNoOpt) throws DescriptorParseException {
    this.protocols = ParseHelper.parseProtocolVersions(line, lineNoOpt,
        partsNoOpt);
  }

  private void parsePublishedLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.publishedMillis = ParseHelper.parseTimestampAtIndex(line,
        partsNoOpt, 1, 2);
  }

  private void parseFingerprintLine(String line, String lineNoOpt)
      throws DescriptorParseException {
    if (lineNoOpt.length() != Key.FINGERPRINT.keyword.length() + 5 * 10) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    this.fingerprint = ParseHelper.parseTwentyByteHexString(line,
        lineNoOpt.substring(Key.FINGERPRINT.keyword.length() + 1)
            .replaceAll(SP, ""));
  }

  private void parseHibernatingLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    if (partsNoOpt.length != 2) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    this.hibernating = ParseHelper.parseBoolean(partsNoOpt[1], line);
  }

  private void parseUptimeLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    if (partsNoOpt.length != 2) {
      throw new DescriptorParseException("Wrong number of values in line "
          + "'" + line + "'.");
    }
    boolean isValid = false;
    try {
      this.uptime = Long.parseLong(partsNoOpt[1]);
      isValid = true;
    } catch (NumberFormatException e) {
      /* Handle below. */
    }
    if (!isValid) {
      throw new DescriptorParseException("Illegal value in line '" + line
          + "'.");
    }
  }

  private void parseOnionKeyLine(String line, String lineNoOpt)
      throws DescriptorParseException {
    if (!lineNoOpt.equals(Key.ONION_KEY.keyword)) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
  }

  private void parseSigningKeyLine(String line, String lineNoOpt)
      throws DescriptorParseException {
    if (!lineNoOpt.equals(Key.SIGNING_KEY.keyword)) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
  }

  private void parseAcceptLine(String line, String lineNoOpt,
      String[] partsNoOpt) throws DescriptorParseException {
    this.parseExitPolicyLine(line, lineNoOpt, partsNoOpt);
  }

  private void parseRejectLine(String line, String lineNoOpt,
      String[] partsNoOpt) throws DescriptorParseException {
    this.parseExitPolicyLine(line, lineNoOpt, partsNoOpt);
  }

  private void parseExitPolicyLine(String line, String lineNoOpt,
      String[] partsNoOpt) throws DescriptorParseException {
    if (partsNoOpt.length != 2) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    ParseHelper.parseExitPattern(line, partsNoOpt[1]);
    this.exitPolicyLines.add(lineNoOpt);
  }

  private void parseRouterSignatureLine(String line, String lineNoOpt)
      throws DescriptorParseException {
    if (!lineNoOpt.equals(Key.ROUTER_SIGNATURE.keyword)) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
  }

  private void parseContactLine(String lineNoOpt) {
    if (lineNoOpt.length() > Key.CONTACT.keyword.length() + 1) {
      this.contact = lineNoOpt.substring(Key.CONTACT.keyword.length() + 1);
    } else {
      this.contact = "";
    }
  }

  private void parseBridgeDistributionRequestLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    if (partsNoOpt.length < 2) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    this.bridgeDistributionRequest = partsNoOpt[1];
  }

  private void parseFamilyLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    String[] familyEntries = new String[partsNoOpt.length - 1];
    for (int i = 1; i < partsNoOpt.length; i++) {
      if (partsNoOpt[i].startsWith("$")) {
        if (partsNoOpt[i].contains("=") ^ partsNoOpt[i].contains("~")) {
          String separator = partsNoOpt[i].contains("=") ? "=" : "~";
          String fingerprint = ParseHelper.parseTwentyByteHexString(line,
              partsNoOpt[i].substring(1, partsNoOpt[i].indexOf(
              separator)));
          String nickname = ParseHelper.parseNickname(line,
              partsNoOpt[i].substring(partsNoOpt[i].indexOf(
              separator) + 1));
          familyEntries[i - 1] = "$" + fingerprint + separator + nickname;
        } else {
          familyEntries[i - 1] = "$"
              + ParseHelper.parseTwentyByteHexString(line,
              partsNoOpt[i].substring(1));
        }
      } else {
        familyEntries[i - 1] = ParseHelper.parseNickname(line,
            partsNoOpt[i]);
      }
    }
    this.familyEntries = familyEntries;
  }

  private void parseReadHistoryLine(String line, String[] partsNoOpt)
      throws DescriptorParseException {
    this.readHistory = new BandwidthHistoryImpl(line, partsNoOpt);
  }

  private void parseWriteHistoryLine(String line, String[] partsNoOpt)
      throws DescriptorParseException {
    this.writeHistory = new BandwidthHistoryImpl(line, partsNoOpt);
  }

  private void parseEventdnsLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    if (partsNoOpt.length != 2) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    this.usesEnhancedDnsLogic = ParseHelper.parseBoolean(partsNoOpt[1],
        line);
  }

  private void parseCachesExtraInfoLine(String line, String lineNoOpt)
      throws DescriptorParseException {
    if (!lineNoOpt.equals(Key.CACHES_EXTRA_INFO.keyword)) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    this.cachesExtraInfo = true;
  }

  private void parseExtraInfoDigestLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    if (partsNoOpt.length < 2) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    this.extraInfoDigest = ParseHelper.parseTwentyByteHexString(line,
        partsNoOpt[1]);
    if (partsNoOpt.length >= 3) {
      ParseHelper.verifyThirtyTwoByteBase64String(line, partsNoOpt[2]);
      this.extraInfoDigestSha256 = partsNoOpt[2];
    }
  }

  private void parseHiddenServiceDirLine() {
    this.hiddenServiceDir = true;
  }

  private void parseProtocolsLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    int linkIndex = -1;
    int circuitIndex = -1;
    for (int i = 1; i < partsNoOpt.length; i++) {
      switch (partsNoOpt[i]) {
        case "Link":
          linkIndex = i;
          break;
        case "Circuit":
          circuitIndex = i;
          break;
        default:
          // empty
      }
    }
    if (linkIndex < 0 || circuitIndex < 0 || circuitIndex < linkIndex) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    try {
      Integer[] linkProtocolVersions =
          new Integer[circuitIndex - linkIndex - 1];
      for (int i = linkIndex + 1, j = 0; i < circuitIndex; i++, j++) {
        linkProtocolVersions[j] = Integer.parseInt(partsNoOpt[i]);
      }
      Integer[] circuitProtocolVersions =
          new Integer[partsNoOpt.length - circuitIndex - 1];
      for (int i = circuitIndex + 1, j = 0; i < partsNoOpt.length;
          i++, j++) {
        circuitProtocolVersions[j] = Integer.parseInt(partsNoOpt[i]);
      }
      this.linkProtocolVersions = linkProtocolVersions;
      this.circuitProtocolVersions = circuitProtocolVersions;
    } catch (NumberFormatException e) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
  }

  private void parseAllowSingleHopExitsLine(String line, String lineNoOpt)
      throws DescriptorParseException {
    if (!lineNoOpt.equals(Key.ALLOW_SINGLE_HOP_EXITS.keyword)) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    this.allowSingleHopExits = true;
  }

  private void parseDircacheportLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    /* The dircacheport line was only contained in server descriptors
     * published by Tor 0.0.8 and before.  It's only specified in old
     * tor-spec.txt versions. */
    if (partsNoOpt.length != 2) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    if (this.dirPort != 0) {
      throw new DescriptorParseException("At most one of dircacheport "
          + "and the directory port in the router line may be non-zero.");
    }
    this.dirPort = ParseHelper.parsePort(line, partsNoOpt[1]);
  }

  private void parseRouterDigestLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    if (partsNoOpt.length != 2) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    this.setDigestSha1Hex(ParseHelper.parseTwentyByteHexString(
        line, partsNoOpt[1]));
  }

  private void parseIpv6PolicyLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    boolean isValid = true;
    if (partsNoOpt.length != 3) {
      isValid = false;
    } else {
      switch (Key.get(partsNoOpt[1])) {
        case ACCEPT:
        case REJECT:
          this.ipv6DefaultPolicy = partsNoOpt[1];
          this.ipv6PortList = partsNoOpt[2];
          String[] ports = partsNoOpt[2].split(",", -1);
          for (String port : ports) {
            if (port.length() < 1) {
              isValid = false;
              break;
            }
          }
          break;
        case INVALID:
        default:
          isValid = false;
      }
    }
    if (!isValid) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
  }

  private void parseNtorOnionKeyLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    if (partsNoOpt.length != 2) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    this.ntorOnionKey = partsNoOpt[1].replaceAll("=", "");
  }

  private void parseIdentityEd25519Line(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    if (partsNoOpt.length != 1) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
  }

  private void parseOnionKeyCrosscert(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    if (partsNoOpt.length != 1) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
  }

  private void parseNtorOnionKeyCrosscert(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    if (partsNoOpt.length != 2) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    try {
      this.ntorOnionKeyCrosscertSign = Integer.parseInt(partsNoOpt[1]);
    } catch (NumberFormatException e) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
  }

  private void parseTunnelledDirServerLine(String line, String lineNoOpt)
      throws DescriptorParseException {
    if (!lineNoOpt.equals(Key.TUNNELLED_DIR_SERVER.keyword)) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    this.tunnelledDirServer = true;
  }

  private void parseIdentityEd25519CryptoBlock(String cryptoString)
      throws DescriptorParseException {
    String masterKeyEd25519FromIdentityEd25519 =
        ParseHelper.parseMasterKeyEd25519FromIdentityEd25519CryptoBlock(
        cryptoString);
    if (this.masterKeyEd25519 != null && !this.masterKeyEd25519.equals(
        masterKeyEd25519FromIdentityEd25519)) {
      throw new DescriptorParseException("Mismatch between "
          + "identity-ed25519 and master-key-ed25519.");
    }
    this.masterKeyEd25519 = masterKeyEd25519FromIdentityEd25519;
  }

  private void parseMasterKeyEd25519Line(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    if (partsNoOpt.length != 2) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    String masterKeyEd25519FromMasterKeyEd25519Line = partsNoOpt[1];
    if (this.masterKeyEd25519 != null && !masterKeyEd25519.equals(
        masterKeyEd25519FromMasterKeyEd25519Line)) {
      throw new DescriptorParseException("Mismatch between "
          + "identity-ed25519 and master-key-ed25519.");
    }
    this.masterKeyEd25519 = masterKeyEd25519FromMasterKeyEd25519Line;
  }

  private void parseRouterSigEd25519Line(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    if (partsNoOpt.length != 2) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    this.routerSignatureEd25519 = partsNoOpt[1];
  }

  private void parseRouterDigestSha256Line(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    if (partsNoOpt.length != 2) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    ParseHelper.verifyThirtyTwoByteBase64String(line, partsNoOpt[1]);
    this.setDigestSha256Base64(partsNoOpt[1]);
  }

  private String nickname;

  @Override
  public String getNickname() {
    return this.nickname;
  }

  private String address;

  @Override
  public String getAddress() {
    return this.address;
  }

  private int orPort;

  @Override
  public int getOrPort() {
    return this.orPort;
  }

  private int socksPort;

  @Override
  public int getSocksPort() {
    return this.socksPort;
  }

  private int dirPort;

  @Override
  public int getDirPort() {
    return this.dirPort;
  }

  private List<String> orAddresses = new ArrayList<>();

  @Override
  public List<String> getOrAddresses() {
    return new ArrayList<>(this.orAddresses);
  }

  private int bandwidthRate;

  @Override
  public int getBandwidthRate() {
    return this.bandwidthRate;
  }

  private int bandwidthBurst;

  @Override
  public int getBandwidthBurst() {
    return this.bandwidthBurst;
  }

  private int bandwidthObserved;

  @Override
  public int getBandwidthObserved() {
    return this.bandwidthObserved;
  }

  private int overloadGeneralVersion = 0;

  @Override
  public int getOverloadGeneralVersion() {
    return this.overloadGeneralVersion;
  }

  private long overloadGeneralTimestamp = -1L;

  @Override
  public long getOverloadGeneralTimestamp() {
    return this.overloadGeneralTimestamp;
  }

  private String platform;

  @Override
  public String getPlatform() {
    return this.platform;
  }

  private SortedMap<String, SortedSet<Long>> protocols;

  @Override
  public SortedMap<String, SortedSet<Long>> getProtocols() {
    return this.protocols;
  }

  private long publishedMillis;

  @Override
  public long getPublishedMillis() {
    return this.publishedMillis;
  }

  private String fingerprint;

  @Override
  public String getFingerprint() {
    return this.fingerprint;
  }

  private boolean hibernating;

  @Override
  public boolean isHibernating() {
    return this.hibernating;
  }

  private Long uptime;

  @Override
  public Long getUptime() {
    return this.uptime;
  }

  private String onionKey;

  @Override
  public String getOnionKey() {
    return this.onionKey;
  }

  private String signingKey;

  @Override
  public String getSigningKey() {
    return this.signingKey;
  }

  private List<String> exitPolicyLines = new ArrayList<>();

  @Override
  public List<String> getExitPolicyLines() {
    return new ArrayList<>(this.exitPolicyLines);
  }

  private String routerSignature;

  @Override
  public String getRouterSignature() {
    return this.routerSignature;
  }

  private String contact;

  @Override
  public String getContact() {
    return this.contact;
  }

  private String bridgeDistributionRequest = null;

  @Override
  public String getBridgeDistributionRequest() {
    return this.bridgeDistributionRequest;
  }

  private String[] familyEntries;

  @Override
  public List<String> getFamilyEntries() {
    return this.familyEntries == null ? null
        : Arrays.asList(this.familyEntries);
  }

  private BandwidthHistory readHistory;

  @Override
  public BandwidthHistory getReadHistory() {
    return this.readHistory;
  }

  private BandwidthHistory writeHistory;

  @Override
  public BandwidthHistory getWriteHistory() {
    return this.writeHistory;
  }

  private boolean usesEnhancedDnsLogic;

  @Override
  public boolean getUsesEnhancedDnsLogic() {
    return this.usesEnhancedDnsLogic;
  }

  private boolean cachesExtraInfo;

  @Override
  public boolean getCachesExtraInfo() {
    return this.cachesExtraInfo;
  }

  private String extraInfoDigest;

  @Override
  public String getExtraInfoDigestSha1Hex() {
    return this.extraInfoDigest;
  }

  private String extraInfoDigestSha256;

  @Override
  public String getExtraInfoDigestSha256Base64() {
    return this.extraInfoDigestSha256;
  }

  private boolean hiddenServiceDir;

  @Override
  public boolean isHiddenServiceDir() {
    return this.hiddenServiceDir;
  }

  @Override
  @Deprecated
  public List<Integer> getHiddenServiceDirVersions() {
    return this.hiddenServiceDir ? null : Collections.singletonList(2);
  }

  private Integer[] linkProtocolVersions;

  @Override
  public List<Integer> getLinkProtocolVersions() {
    return this.linkProtocolVersions == null ? null
        : Arrays.asList(this.linkProtocolVersions);
  }

  private Integer[] circuitProtocolVersions;

  @Override
  public List<Integer> getCircuitProtocolVersions() {
    return this.circuitProtocolVersions == null ? null
        : Arrays.asList(this.circuitProtocolVersions);
  }

  private boolean allowSingleHopExits;

  @Override
  public boolean getAllowSingleHopExits() {
    return this.allowSingleHopExits;
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

  private String ntorOnionKey;

  @Override
  public String getNtorOnionKey() {
    return this.ntorOnionKey;
  }

  private String identityEd25519;

  @Override
  public String getIdentityEd25519() {
    return this.identityEd25519;
  }

  private String masterKeyEd25519;

  @Override
  public String getMasterKeyEd25519() {
    return this.masterKeyEd25519;
  }

  private String routerSignatureEd25519;

  @Override
  public String getRouterSignatureEd25519() {
    return this.routerSignatureEd25519;
  }

  private String onionKeyCrosscert;

  @Override
  public String getOnionKeyCrosscert() {
    return this.onionKeyCrosscert;
  }

  private String ntorOnionKeyCrosscert;

  @Override
  public String getNtorOnionKeyCrosscert() {
    return this.ntorOnionKeyCrosscert;
  }

  private int ntorOnionKeyCrosscertSign = -1;

  @Override
  public int getNtorOnionKeyCrosscertSign() {
    return ntorOnionKeyCrosscertSign;
  }

  private boolean tunnelledDirServer;

  @Override
  public boolean getTunnelledDirServer() {
    return this.tunnelledDirServer;
  }
}

