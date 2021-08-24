/* Copyright 2012--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.BandwidthHistory;
import org.torproject.descriptor.DescriptorParseException;
import org.torproject.descriptor.ExtraInfoDescriptor;

import java.io.File;
import java.util.*;

/**
 * Contains logic to work with extra-info descriptor files.
 *
 * <p>The extra-info document format is defined in the dir-spec
 * document.</p>
 *
 * @see <a href="https://github.com/torproject/torspec/blob/main/dir-spec.txt#L893">dir-spec.txt</a>
 *
 * <p>Descriptor keys are defined and their occurency is checked to
 * make sure the descriptor file format is valid.</p>
 *
 * <p>Descriptor lines are parsed and their values can be accessed
 * through the defined methods.</p>
 *
 * <p>When metrics library learns how to parse a new line for extra-info
 * descriptor files all its fields, from the dir-spec document, are
 * parsed in this class.</p>
 *
 * <p>The new key is added to {@link Key} and its occurence is checked in
 * {@link ExtraInfoDescriptorImpl} constructor.</p>
 *
 * @since 1.0.0
 */


public abstract class ExtraInfoDescriptorImpl extends DescriptorImpl
    implements ExtraInfoDescriptor {

  private static final long serialVersionUID = -4720810362228341775L;

  /**
   * Define keys that MUST occur exactly one time in every
   * instance of the document type.
   */
  private Set<Key> exactlyOnceKeys = EnumSet.of(
      Key.EXTRA_INFO, Key.PUBLISHED);

  /**
   * Define keys that MAY occur zero or one times in any instance of the
   * document type, but MUST NOT occur more than once.
   */
  private static final Set<Key> atMostOnceKeys = EnumSet.of(
      Key.IDENTITY_ED25519, Key.MASTER_KEY_ED25519, Key.READ_HISTORY,
      Key.WRITE_HISTORY, Key.DIRREQ_READ_HISTORY, Key.DIRREQ_WRITE_HISTORY,
      Key.GEOIP_DB_DIGEST, Key.GEOIP6_DB_DIGEST, Key.ROUTER_SIG_ED25519,
      Key.ROUTER_SIGNATURE, Key.ROUTER_DIGEST_SHA256, Key.ROUTER_DIGEST,
      Key.PADDING_COUNTS, Key.OVERLOAD_RATELIMITS, Key.OVERLOAD_FD_EXHAUSTED);

  protected ExtraInfoDescriptorImpl(byte[] descriptorBytes,
      int[] offsetAndLimit, File descriptorFile)
      throws DescriptorParseException {
    super(descriptorBytes, offsetAndLimit, descriptorFile, false);
    this.parseDescriptorBytes();
    this.checkExactlyOnceKeys(exactlyOnceKeys);

    /**
     * When a key is added to {@link Key} it needs to be added here too.
     * Either directly or to the methods that check the occurrence of the key.
     */
    Set<Key> dirreqStatsKeys = EnumSet.of(
        Key.DIRREQ_STATS_END, Key.DIRREQ_V2_IPS, Key.DIRREQ_V3_IPS,
        Key.DIRREQ_V2_REQS, Key.DIRREQ_V3_REQS, Key.DIRREQ_V2_SHARE,
        Key.DIRREQ_V3_SHARE, Key.DIRREQ_V2_RESP, Key.DIRREQ_V3_RESP,
        Key.DIRREQ_V2_DIRECT_DL, Key.DIRREQ_V3_DIRECT_DL,
        Key.DIRREQ_V2_TUNNELED_DL, Key.DIRREQ_V3_TUNNELED_DL);
    Set<Key> entryStatsKeys = EnumSet.of(
        Key.ENTRY_STATS_END, Key.ENTRY_IPS);
    Set<Key> cellStatsKeys = EnumSet.of(
        Key.CELL_STATS_END, Key.CELL_PROCESSED_CELLS, Key.CELL_QUEUED_CELLS,
        Key.CELL_TIME_IN_QUEUE, Key.CELL_CIRCUITS_PER_DECILE);
    Set<Key> connBiDirectStatsKeys = EnumSet.of(Key.CONN_BI_DIRECT);
    Set<Key> exitStatsKeys = EnumSet.of(
        Key.EXIT_STATS_END, Key.EXIT_KIBIBYTES_WRITTEN, Key.EXIT_KIBIBYTES_READ,
        Key.EXIT_STREAMS_OPENED);
    Set<Key> bridgeStatsKeys = EnumSet.of(
        Key.BRIDGE_STATS_END, Key.BRIDGE_IPS);
    atMostOnceKeys.addAll(dirreqStatsKeys);
    atMostOnceKeys.addAll(entryStatsKeys);
    atMostOnceKeys.addAll(cellStatsKeys);
    atMostOnceKeys.addAll(connBiDirectStatsKeys);
    atMostOnceKeys.addAll(exitStatsKeys);
    atMostOnceKeys.addAll(bridgeStatsKeys);
    this.checkAtMostOnceKeys(atMostOnceKeys);
    this.checkKeysDependOn(dirreqStatsKeys, Key.DIRREQ_STATS_END);
    this.checkKeysDependOn(entryStatsKeys, Key.ENTRY_STATS_END);
    this.checkKeysDependOn(cellStatsKeys, Key.CELL_STATS_END);
    this.checkKeysDependOn(exitStatsKeys, Key.EXIT_STATS_END);
    this.checkKeysDependOn(bridgeStatsKeys, Key.BRIDGE_STATS_END);
    this.checkFirstKey(Key.EXTRA_INFO);
    this.clearParsedKeys();
  }

  /**
   * Parse the descriptor file.
   *
   * @throws DescriptorParseException
   *
   */
  private void parseDescriptorBytes() throws DescriptorParseException {
    Scanner scanner = this.newScanner().useDelimiter(NL);
    Key nextCrypto = Key.EMPTY;
    List<String> cryptoLines = null;
    while (scanner.hasNext()) {
      String line = scanner.next();
      String lineNoOpt = line.startsWith(Key.OPT.keyword + SP)
          ? line.substring(Key.OPT.keyword.length() + 1) : line;
      String[] partsNoOpt = lineNoOpt.split("[ \t]+");
      Key key = Key.get(partsNoOpt[0]);
      switch (key) {
        case EXTRA_INFO:
          this.parseExtraInfoLine(line, partsNoOpt);
          break;
        case PUBLISHED:
          this.parsePublishedLine(line, partsNoOpt);
          break;
        case READ_HISTORY:
          this.parseReadHistoryLine(line, partsNoOpt);
          break;
        case WRITE_HISTORY:
          this.parseWriteHistoryLine(line, partsNoOpt);
          break;
        case OVERLOAD_RATELIMITS:
          this.parseOverloadRatelimits(line, partsNoOpt);
          break;
        case OVERLOAD_FD_EXHAUSTED:
          this.parseOverloadFdExhausted(line, partsNoOpt);
          break;
        case IPV6_READ_HISTORY:
          this.parseIpv6ReadHistoryLine(line, partsNoOpt);
          break;
        case IPV6_WRITE_HISTORY:
          this.parseIpv6WriteHistoryLine(line, partsNoOpt);
          break;
        case GEOIP_DB_DIGEST:
          this.parseGeoipDbDigestLine(line, partsNoOpt);
          break;
        case GEOIP6_DB_DIGEST:
          this.parseGeoip6DbDigestLine(line, partsNoOpt);
          break;
        case GEOIP_START_TIME:
          this.parseGeoipStartTimeLine(line, partsNoOpt);
          break;
        case GEOIP_CLIENT_ORIGINS:
          this.parseGeoipClientOriginsLine(line, partsNoOpt);
          break;
        case DIRREQ_STATS_END:
          this.parseDirreqStatsEndLine(line, partsNoOpt);
          break;
        case DIRREQ_V2_IPS:
          this.parseDirreqV2IpsLine(line, partsNoOpt);
          break;
        case DIRREQ_V3_IPS:
          this.parseDirreqV3IpsLine(line, partsNoOpt);
          break;
        case DIRREQ_V2_REQS:
          this.parseDirreqV2ReqsLine(line, partsNoOpt);
          break;
        case DIRREQ_V3_REQS:
          this.parseDirreqV3ReqsLine(line, partsNoOpt);
          break;
        case DIRREQ_V2_SHARE:
          this.parseDirreqV2ShareLine(line, partsNoOpt);
          break;
        case DIRREQ_V3_SHARE:
          this.parseDirreqV3ShareLine(line, partsNoOpt);
          break;
        case DIRREQ_V2_RESP:
          this.parseDirreqV2RespLine(line, partsNoOpt);
          break;
        case DIRREQ_V3_RESP:
          this.parseDirreqV3RespLine(line, partsNoOpt);
          break;
        case DIRREQ_V2_DIRECT_DL:
          this.parseDirreqV2DirectDlLine(line, partsNoOpt);
          break;
        case DIRREQ_V3_DIRECT_DL:
          this.parseDirreqV3DirectDlLine(line, partsNoOpt);
          break;
        case DIRREQ_V2_TUNNELED_DL:
          this.parseDirreqV2TunneledDlLine(line, partsNoOpt);
          break;
        case DIRREQ_V3_TUNNELED_DL:
          this.parseDirreqV3TunneledDlLine(line, partsNoOpt);
          break;
        case DIRREQ_READ_HISTORY:
          this.parseDirreqReadHistoryLine(line, partsNoOpt);
          break;
        case DIRREQ_WRITE_HISTORY:
          this.parseDirreqWriteHistoryLine(line, partsNoOpt);
          break;
        case ENTRY_STATS_END:
          this.parseEntryStatsEndLine(line, partsNoOpt);
          break;
        case ENTRY_IPS:
          this.parseEntryIpsLine(line, partsNoOpt);
          break;
        case CELL_STATS_END:
          this.parseCellStatsEndLine(line, partsNoOpt);
          break;
        case CELL_PROCESSED_CELLS:
          this.parseCellProcessedCellsLine(line, partsNoOpt);
          break;
        case CELL_QUEUED_CELLS:
          this.parseCellQueuedCellsLine(line, partsNoOpt);
          break;
        case CELL_TIME_IN_QUEUE:
          this.parseCellTimeInQueueLine(line, partsNoOpt);
          break;
        case CELL_CIRCUITS_PER_DECILE:
          this.parseCellCircuitsPerDecileLine(line,
              partsNoOpt);
          break;
        case CONN_BI_DIRECT:
          this.parseConnBiDirectLine(line, partsNoOpt);
          break;
        case IPV6_CONN_BI_DIRECT:
          this.parseIpv6ConnBiDirectLine(line, partsNoOpt);
          break;
        case EXIT_STATS_END:
          this.parseExitStatsEndLine(line, partsNoOpt);
          break;
        case EXIT_KIBIBYTES_WRITTEN:
          this.parseExitKibibytesWrittenLine(line, partsNoOpt);
          break;
        case EXIT_KIBIBYTES_READ:
          this.parseExitKibibytesReadLine(line, partsNoOpt);
          break;
        case EXIT_STREAMS_OPENED:
          this.parseExitStreamsOpenedLine(line, partsNoOpt);
          break;
        case BRIDGE_STATS_END:
          this.parseBridgeStatsEndLine(line, partsNoOpt);
          break;
        case BRIDGE_IPS:
          this.parseBridgeStatsIpsLine(line, partsNoOpt);
          break;
        case BRIDGE_IP_VERSIONS:
          this.parseBridgeIpVersionsLine(line, partsNoOpt);
          break;
        case BRIDGE_IP_TRANSPORTS:
          this.parseBridgeIpTransportsLine(line, partsNoOpt);
          break;
        case TRANSPORT:
          this.parseTransportLine(line, partsNoOpt);
          break;
        case HIDSERV_STATS_END:
          this.parseHidservStatsEndLine(line, partsNoOpt);
          break;
        case HIDSERV_REND_RELAYED_CELLS:
          this.parseHidservRendRelayedCellsLine(line,
              partsNoOpt);
          break;
        case HIDSERV_DIR_ONIONS_SEEN:
          this.parseHidservDirOnionsSeenLine(line, partsNoOpt);
          break;
        case HIDSERV_V3_STATS_END:
          this.parseHidservV3StatsEndLine(line, partsNoOpt);
          break;
        case HIDSERV_REND_V3_RELAYED_CELLS:
          this.parseHidservRendV3RelayedCellsLine(line, partsNoOpt);
          break;
        case HIDSERV_DIR_V3_ONIONS_SEEN:
          this.parseHidservDirV3OnionsSeenLine(line, partsNoOpt);
          break;
        case PADDING_COUNTS:
          this.parsePaddingCountsLine(line, partsNoOpt);
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
        case ROUTER_SIGNATURE:
          this.parseRouterSignatureLine(line, lineNoOpt);
          nextCrypto = key;
          break;
        case ROUTER_DIGEST:
          this.parseRouterDigestLine(line, partsNoOpt);
          break;
        case ROUTER_DIGEST_SHA256:
          this.parseRouterDigestSha256Line(line, partsNoOpt);
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
            case ROUTER_SIGNATURE:
              this.routerSignature = cryptoString;
              break;
            case IDENTITY_ED25519:
              this.identityEd25519 = cryptoString;
              this.parseIdentityEd25519CryptoBlock(cryptoString);
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

  private void parseExtraInfoLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    if (partsNoOpt.length != 3) {
      throw new DescriptorParseException("Illegal line '" + line
          + "' in extra-info descriptor.");
    }
    this.nickname = ParseHelper.parseNickname(line, partsNoOpt[1]);
    this.fingerprint = ParseHelper.parseTwentyByteHexString(line,
        partsNoOpt[2]);
  }

  private void parsePublishedLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.publishedMillis = ParseHelper.parseTimestampAtIndex(line,
        partsNoOpt, 1, 2);
  }

  private void parseReadHistoryLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.readHistory = new BandwidthHistoryImpl(line,
        partsNoOpt);
  }

  private void parseWriteHistoryLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.writeHistory = new BandwidthHistoryImpl(line,
        partsNoOpt);
  }

  private void parseOverloadRatelimits(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.overloadRatelimitsVersion = Integer.parseInt(partsNoOpt[1]);
    if (this.overloadRatelimitsVersion != 1) {
      throw new DescriptorParseException("Unknown version number for line '"
              + line + "' in extra-info descriptor.");
    }
    if (partsNoOpt.length < 8) {
      throw new DescriptorParseException("Missing fields for line '"
          + line + "' in extra-info descriptor.");
    }
    this.overloadRatelimitsTimestamp = ParseHelper.parseTimestampAtIndex(line,
        partsNoOpt, 2, 3);
    this.overloadRatelimitsRateLimit = Long.parseLong(partsNoOpt[4]);
    this.overloadRatelimitsBurstLimit = Long.parseLong(partsNoOpt[5]);
    this.overloadRatelimitsReadCount = Integer.parseInt(partsNoOpt[6]);
    this.overloadRatelimitsWriteCount = Integer.parseInt(partsNoOpt[7]);
  }

  private void parseOverloadFdExhausted(String line,
                                       String[] partsNoOpt)
      throws DescriptorParseException {
    this.overloadFdExhaustedVersion = Integer.parseInt(partsNoOpt[1]);
    if (this.overloadFdExhaustedVersion != 1) {
      throw new DescriptorParseException("Unknown version number for line '"
              + line + "' in extra-info descriptor.");
    }
    if (partsNoOpt.length < 4) {
      throw new DescriptorParseException("Missing fields for line '"
          + line + "' in extra-info descriptor.");
    }
    this.overloadFdExhaustedTimestamp = ParseHelper.parseTimestampAtIndex(line,
            partsNoOpt, 2, 3);
  }

  private void parseIpv6ReadHistoryLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.ipv6ReadHistory = new BandwidthHistoryImpl(line,
        partsNoOpt);
  }

  private void parseIpv6WriteHistoryLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.ipv6WriteHistory = new BandwidthHistoryImpl(line,
        partsNoOpt);
  }

  private void parseGeoipDbDigestLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    if (partsNoOpt.length < 2) {
      throw new DescriptorParseException("Illegal line '" + line
          + "' in extra-info descriptor.");
    }
    this.geoipDbDigest = ParseHelper.parseTwentyByteHexString(line,
        partsNoOpt[1]);
  }

  private void parseGeoip6DbDigestLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    if (partsNoOpt.length < 2) {
      throw new DescriptorParseException("Illegal line '" + line
          + "' in extra-info descriptor.");
    }
    this.geoip6DbDigest = ParseHelper.parseTwentyByteHexString(line,
        partsNoOpt[1]);
  }

  private void parseGeoipStartTimeLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    if (partsNoOpt.length < 3) {
      throw new DescriptorParseException("Illegal line '" + line
          + "' in extra-info descriptor.");
    }
    this.geoipStartTimeMillis = ParseHelper.parseTimestampAtIndex(line,
        partsNoOpt, 1, 2);
  }

  private void parseGeoipClientOriginsLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.geoipClientOrigins =
        ParseHelper.parseCommaSeparatedKeyIntegerValueList(line,
        partsNoOpt, 1, 2);
  }

  private void parseDirreqStatsEndLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    long[] parsedStatsEndData = this.parseStatsEndLine(line, partsNoOpt,
        5);
    this.dirreqStatsEndMillis = parsedStatsEndData[0];
    this.dirreqStatsIntervalLength = parsedStatsEndData[1];
  }

  private long[] parseStatsEndLine(String line, String[] partsNoOpt,
      int partsNoOptExpectedLength) throws DescriptorParseException {
    if (partsNoOpt.length < partsNoOptExpectedLength
        || partsNoOpt[3].length() < 2 || !partsNoOpt[3].startsWith("(")
        || !partsNoOpt[4].equals("s)")) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    long[] result = new long[2];
    result[0] = ParseHelper.parseTimestampAtIndex(line, partsNoOpt, 1, 2);
    result[1] = ParseHelper.parseSeconds(line,
        partsNoOpt[3].substring(1));
    if (result[1] <= 0) {
      throw new DescriptorParseException("Interval length must be "
          + "positive in line '" + line + "'.");
    }
    return result;
  }

  private void parseDirreqV2IpsLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.dirreqV2Ips = ParseHelper.parseCommaSeparatedKeyIntegerValueList(
        line, partsNoOpt, 1, 2);
  }

  private void parseDirreqV3IpsLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.dirreqV3Ips = ParseHelper.parseCommaSeparatedKeyIntegerValueList(
        line, partsNoOpt, 1, 2);
  }

  private void parseDirreqV2ReqsLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.dirreqV2Reqs =
        ParseHelper.parseCommaSeparatedKeyIntegerValueList(line,
        partsNoOpt, 1, 2);
  }

  private void parseDirreqV3ReqsLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.dirreqV3Reqs =
        ParseHelper.parseCommaSeparatedKeyIntegerValueList(line,
        partsNoOpt, 1, 2);
  }

  private void parseDirreqV2ShareLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.dirreqV2Share = this.parseShareLine(line, partsNoOpt);
  }

  private void parseDirreqV3ShareLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.dirreqV3Share = this.parseShareLine(line, partsNoOpt);
  }

  private double parseShareLine(String line, String[] partsNoOpt)
      throws DescriptorParseException {
    double share = -1.0;
    if (partsNoOpt.length >= 2 && partsNoOpt[1].length() >= 2
        && partsNoOpt[1].endsWith("%")) {
      String shareString = partsNoOpt[1];
      shareString = shareString.substring(0, shareString.length() - 1);
      try {
        share = Double.parseDouble(shareString);
      } catch (NumberFormatException e) {
        /* Handle below. */
      }
    }
    if (share < 0.0) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    return share;
  }

  private void parseDirreqV2RespLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.dirreqV2Resp =
        ParseHelper.parseCommaSeparatedKeyIntegerValueList(line,
        partsNoOpt, 1, 0);
  }

  private void parseDirreqV3RespLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.dirreqV3Resp =
        ParseHelper.parseCommaSeparatedKeyIntegerValueList(line,
        partsNoOpt, 1, 0);
  }

  private void parseDirreqV2DirectDlLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.dirreqV2DirectDl =
        ParseHelper.parseCommaSeparatedKeyIntegerValueList(line,
        partsNoOpt, 1, 0);
  }

  private void parseDirreqV3DirectDlLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.dirreqV3DirectDl =
        ParseHelper.parseCommaSeparatedKeyIntegerValueList(line,
        partsNoOpt, 1, 0);
  }

  private void parseDirreqV2TunneledDlLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.dirreqV2TunneledDl =
        ParseHelper.parseCommaSeparatedKeyIntegerValueList(line,
        partsNoOpt, 1, 0);
  }

  private void parseDirreqV3TunneledDlLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.dirreqV3TunneledDl =
        ParseHelper.parseCommaSeparatedKeyIntegerValueList(
        line,partsNoOpt, 1, 0);
  }

  private void parseDirreqReadHistoryLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.dirreqReadHistory = new BandwidthHistoryImpl(line,
        partsNoOpt);
  }

  private void parseDirreqWriteHistoryLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.dirreqWriteHistory = new BandwidthHistoryImpl(line,
        partsNoOpt);
  }

  private void parseEntryStatsEndLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    long[] parsedStatsEndData = this.parseStatsEndLine(line, partsNoOpt,
        5);
    this.entryStatsEndMillis = parsedStatsEndData[0];
    this.entryStatsIntervalLength = parsedStatsEndData[1];
  }

  private void parseEntryIpsLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.entryIps = ParseHelper.parseCommaSeparatedKeyIntegerValueList(
        line, partsNoOpt, 1, 2);
  }

  private void parseCellStatsEndLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    long[] parsedStatsEndData = this.parseStatsEndLine(line, partsNoOpt,
        5);
    this.cellStatsEndMillis = parsedStatsEndData[0];
    this.cellStatsIntervalLength = parsedStatsEndData[1];
  }

  private void parseCellProcessedCellsLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.cellProcessedCells = ParseHelper
        .parseCommaSeparatedIntegerValueList(line, partsNoOpt, 1);
    if (this.cellProcessedCells.length != 10) {
      throw new DescriptorParseException("There must be exact ten values "
          + "in line '" + line + "'.");
    }
  }

  private void parseCellQueuedCellsLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.cellQueuedCells = ParseHelper.parseCommaSeparatedDoubleValueList(
        line, partsNoOpt, 1);
    if (this.cellQueuedCells.length != 10) {
      throw new DescriptorParseException("There must be exact ten values "
          + "in line '" + line + "'.");
    }
  }

  private void parseCellTimeInQueueLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.cellTimeInQueue = ParseHelper
        .parseCommaSeparatedIntegerValueList(line, partsNoOpt, 1);
    if (this.cellTimeInQueue.length != 10) {
      throw new DescriptorParseException("There must be exact ten values "
          + "in line '" + line + "'.");
    }
  }

  private void parseCellCircuitsPerDecileLine(String line,
      String[] partsNoOpt)
      throws DescriptorParseException {
    int circuits = -1;
    if (partsNoOpt.length >= 2) {
      try {
        circuits = Integer.parseInt(partsNoOpt[1]);
      } catch (NumberFormatException e) {
        /* Handle below. */
      }
    }
    if (circuits < 0) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    this.cellCircuitsPerDecile = circuits;
  }

  private void parseConnBiDirectLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    long[] parsedStatsEndData = this.parseStatsEndLine(line, partsNoOpt,
        6);
    this.connBiDirectStatsEndMillis = parsedStatsEndData[0];
    this.connBiDirectStatsIntervalLength = parsedStatsEndData[1];
    Integer[] parsedConnBiDirectStats = ParseHelper
        .parseCommaSeparatedIntegerValueList(line, partsNoOpt, 5);
    if (parsedConnBiDirectStats.length != 4) {
      throw new DescriptorParseException("Illegal line '" + line + "' in "
          + "extra-info descriptor.");
    }
    this.connBiDirectBelow = parsedConnBiDirectStats[0];
    this.connBiDirectRead = parsedConnBiDirectStats[1];
    this.connBiDirectWrite = parsedConnBiDirectStats[2];
    this.connBiDirectBoth = parsedConnBiDirectStats[3];
  }

  private void parseIpv6ConnBiDirectLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    long[] parsedStatsEndData = this.parseStatsEndLine(line, partsNoOpt,
        6);
    this.ipv6ConnBiDirectStatsEndMillis = parsedStatsEndData[0];
    this.ipv6ConnBiDirectStatsIntervalLength = parsedStatsEndData[1];
    Integer[] parsedIpv6ConnBiDirectStats = ParseHelper
        .parseCommaSeparatedIntegerValueList(line, partsNoOpt, 5);
    if (parsedIpv6ConnBiDirectStats.length != 4) {
      throw new DescriptorParseException("Illegal line '" + line + "' in "
          + "extra-info descriptor.");
    }
    this.ipv6ConnBiDirectBelow = parsedIpv6ConnBiDirectStats[0];
    this.ipv6ConnBiDirectRead = parsedIpv6ConnBiDirectStats[1];
    this.ipv6ConnBiDirectWrite = parsedIpv6ConnBiDirectStats[2];
    this.ipv6ConnBiDirectBoth = parsedIpv6ConnBiDirectStats[3];
  }

  private void parseExitStatsEndLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    long[] parsedStatsEndData = this.parseStatsEndLine(line, partsNoOpt,
        5);
    this.exitStatsEndMillis = parsedStatsEndData[0];
    this.exitStatsIntervalLength = parsedStatsEndData[1];
  }

  private void parseExitKibibytesWrittenLine(String line,
      String[] partsNoOpt)
      throws DescriptorParseException {
    this.exitKibibytesWritten = this.sortByPorts(ParseHelper
        .parseCommaSeparatedKeyLongValueList(line, partsNoOpt, 1, 0));
    this.verifyPorts(line, this.exitKibibytesWritten.keySet());
    this.verifyBytesOrStreams(line, this.exitKibibytesWritten.values());
  }

  private void parseExitKibibytesReadLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.exitKibibytesRead = this.sortByPorts(ParseHelper
        .parseCommaSeparatedKeyLongValueList(line, partsNoOpt, 1, 0));
    this.verifyPorts(line, this.exitKibibytesRead.keySet());
    this.verifyBytesOrStreams(line, this.exitKibibytesRead.values());
  }

  private void parseExitStreamsOpenedLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.exitStreamsOpened = this.sortByPorts(ParseHelper
        .parseCommaSeparatedKeyLongValueList(line, partsNoOpt, 1, 0));
    this.verifyPorts(line, this.exitStreamsOpened.keySet());
    this.verifyBytesOrStreams(line, this.exitStreamsOpened.values());
  }

  private SortedMap<String, Long> sortByPorts(
      SortedMap<String, Long> naturalOrder) {
    SortedMap<String, Long> byPortNumber =
        new TreeMap<>(new ExitStatisticsPortComparator());
    byPortNumber.putAll(naturalOrder);
    return byPortNumber;
  }

  private void verifyPorts(String line, Set<String> ports)
      throws DescriptorParseException {
    boolean valid = true;
    try {
      for (String port : ports) {
        if (!port.equals("other") && Integer.parseInt(port) <= 0) {
          valid = false;
          break;
        }
      }
    } catch (NumberFormatException e) {
      valid = false;
    }
    if (!valid) {
      throw new DescriptorParseException("Invalid port in line '" + line
          + "'.");
    }
  }

  private void verifyBytesOrStreams(String line,
      Collection<Long> bytesOrStreams) throws DescriptorParseException {
    boolean valid = true;
    for (long s : bytesOrStreams) {
      if (s < 0L) {
        valid = false;
        break;
      }
    }
    if (!valid) {
      throw new DescriptorParseException("Invalid value in line '" + line
          + "'.");
    }
  }

  private void parseBridgeStatsEndLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    long[] parsedStatsEndData = this.parseStatsEndLine(line, partsNoOpt,
        5);
    this.bridgeStatsEndMillis = parsedStatsEndData[0];
    this.bridgeStatsIntervalLength = parsedStatsEndData[1];
  }

  private void parseBridgeStatsIpsLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.bridgeIps =
        ParseHelper.parseCommaSeparatedKeyIntegerValueList(line,
        partsNoOpt, 1, 2);
  }

  private void parseBridgeIpVersionsLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.bridgeIpVersions =
        ParseHelper.parseCommaSeparatedKeyIntegerValueList(line,
        partsNoOpt, 1, 2);
  }

  private void parseBridgeIpTransportsLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    this.bridgeIpTransports =
        ParseHelper.parseCommaSeparatedKeyIntegerValueList(line,
        partsNoOpt, 1, 0);
  }

  private void parseTransportLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    if (partsNoOpt.length < 2) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    this.transports.add(partsNoOpt[1]);
  }

  private void parseHidservStatsEndLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    long[] parsedStatsEndData = this.parseStatsEndLine(line, partsNoOpt,
        5);
    this.hidservStatsEndMillis = parsedStatsEndData[0];
    this.hidservStatsIntervalLength = parsedStatsEndData[1];
  }

  private void parseHidservRendRelayedCellsLine(String line,
      String[] partsNoOpt)
      throws DescriptorParseException {
    if (partsNoOpt.length < 2) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    try {
      this.hidservRendRelayedCells = Double.parseDouble(partsNoOpt[1]);
    } catch (NumberFormatException e) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    this.hidservRendRelayedCellsParameters =
        ParseHelper.parseSpaceSeparatedStringKeyDoubleValueMap(line,
        partsNoOpt, 2);
  }

  private void parseHidservDirOnionsSeenLine(String line,
      String[] partsNoOpt)
      throws DescriptorParseException {
    if (partsNoOpt.length < 2) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    try {
      this.hidservDirOnionsSeen = Double.parseDouble(partsNoOpt[1]);
    } catch (NumberFormatException e) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    this.hidservDirOnionsSeenParameters =
        ParseHelper.parseSpaceSeparatedStringKeyDoubleValueMap(line,
        partsNoOpt, 2);
  }

  private void parseHidservV3StatsEndLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    long[] parsedStatsEndData = this.parseStatsEndLine(line, partsNoOpt,
        5);
    this.hidservV3StatsEndMillis = parsedStatsEndData[0];
    this.hidservV3StatsIntervalLength = parsedStatsEndData[1];
  }

  private void parseHidservRendV3RelayedCellsLine(String line,
      String[] partsNoOpt)
      throws DescriptorParseException {
    if (partsNoOpt.length < 2) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    try {
      this.hidservRendV3RelayedCells = Double.parseDouble(partsNoOpt[1]);
    } catch (NumberFormatException e) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    this.hidservRendV3RelayedCellsParameters =
        ParseHelper.parseSpaceSeparatedStringKeyDoubleValueMap(line,
            partsNoOpt, 2);
  }

  private void parseHidservDirV3OnionsSeenLine(String line,
      String[] partsNoOpt)
      throws DescriptorParseException {
    if (partsNoOpt.length < 2) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    try {
      this.hidservDirV3OnionsSeen = Double.parseDouble(partsNoOpt[1]);
    } catch (NumberFormatException e) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    this.hidservDirV3OnionsSeenParameters =
        ParseHelper.parseSpaceSeparatedStringKeyDoubleValueMap(line,
            partsNoOpt, 2);
  }

  private void parsePaddingCountsLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    long[] parsedStatsEndData = this.parseStatsEndLine(line, partsNoOpt,
        6);
    this.paddingCountsStatsEndMillis = parsedStatsEndData[0];
    this.paddingCountsStatsIntervalLength = parsedStatsEndData[1];
    this.paddingCounts = ParseHelper.parseSpaceSeparatedStringKeyLongValueMap(
        line, partsNoOpt, 5);
  }

  private void parseRouterSignatureLine(String line, String lineNoOpt)
      throws DescriptorParseException {
    if (!lineNoOpt.equals("router-signature")) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
  }

  private void parseRouterDigestLine(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    if (partsNoOpt.length != 2) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
    this.setDigestSha1Hex(ParseHelper.parseTwentyByteHexString(line,
        partsNoOpt[1]));
  }

  private void parseIdentityEd25519Line(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    if (partsNoOpt.length != 1) {
      throw new DescriptorParseException("Illegal line '" + line + "'.");
    }
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

  private String fingerprint;

  @Override
  public String getFingerprint() {
    return this.fingerprint;
  }

  private long publishedMillis;

  @Override
  public long getPublishedMillis() {
    return this.publishedMillis;
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

  private BandwidthHistory ipv6ReadHistory;

  @Override
  public BandwidthHistory getIpv6ReadHistory() {
    return this.ipv6ReadHistory;
  }

  private BandwidthHistory ipv6WriteHistory;

  @Override
  public BandwidthHistory getIpv6WriteHistory() {
    return this.ipv6WriteHistory;
  }

  private String geoipDbDigest;

  @Override
  public String getGeoipDbDigestSha1Hex() {
    return this.geoipDbDigest;
  }

  private String geoip6DbDigest;

  @Override
  public String getGeoip6DbDigestSha1Hex() {
    return this.geoip6DbDigest;
  }

  private int overloadRatelimitsVersion = 0;

  @Override
  public int getOverloadRatelimitsVersion() {
    return this.overloadRatelimitsVersion;
  }

  private long overloadRatelimitsTimestamp = -1L;

  @Override
  public long getOverloadRatelimitsTimestamp() {
    return this.overloadRatelimitsTimestamp;
  }

  private long overloadRatelimitsRateLimit = -1L;

  @Override
  public long getOverloadRatelimitsRateLimit() {
    return this.overloadRatelimitsRateLimit;
  }

  private long overloadRatelimitsBurstLimit = -1L;

  @Override
  public long getOverloadRatelimitsBurstLimit() {
    return this.overloadRatelimitsBurstLimit;
  }

  private int overloadRatelimitsReadCount = -1;

  @Override
  public int getOverloadRatelimitsReadCount() {
    return this.overloadRatelimitsReadCount;
  }

  private int overloadRatelimitsWriteCount = -1;

  @Override
  public int getOverloadRatelimitsWriteCount() {
    return this.overloadRatelimitsWriteCount;
  }

  private int overloadFdExhaustedVersion = 0;

  @Override
  public int getOverloadFdExhaustedVersion() {
    return this.overloadFdExhaustedVersion;
  }

  private long overloadFdExhaustedTimestamp = -1L;

  @Override
  public long getOverloadFdExhaustedTimestamp() {
    return this.overloadFdExhaustedTimestamp;
  }

  private long dirreqStatsEndMillis = -1L;

  @Override
  public long getDirreqStatsEndMillis() {
    return this.dirreqStatsEndMillis;
  }

  private long dirreqStatsIntervalLength = -1L;

  @Override
  public long getDirreqStatsIntervalLength() {
    return this.dirreqStatsIntervalLength;
  }

  private String dirreqV2Ips;

  @Override
  public SortedMap<String, Integer> getDirreqV2Ips() {
    return ParseHelper.convertCommaSeparatedKeyIntegerValueList(
        this.dirreqV2Ips);
  }

  private String dirreqV3Ips;

  @Override
  public SortedMap<String, Integer> getDirreqV3Ips() {
    return ParseHelper.convertCommaSeparatedKeyIntegerValueList(
        this.dirreqV3Ips);
  }

  private String dirreqV2Reqs;

  @Override
  public SortedMap<String, Integer> getDirreqV2Reqs() {
    return ParseHelper.convertCommaSeparatedKeyIntegerValueList(
        this.dirreqV2Reqs);
  }

  private String dirreqV3Reqs;

  @Override
  public SortedMap<String, Integer> getDirreqV3Reqs() {
    return ParseHelper.convertCommaSeparatedKeyIntegerValueList(
        this.dirreqV3Reqs);
  }

  private double dirreqV2Share = -1.0;

  @Override
  public double getDirreqV2Share() {
    return this.dirreqV2Share;
  }

  private double dirreqV3Share = -1.0;

  @Override
  public double getDirreqV3Share() {
    return this.dirreqV3Share;
  }

  private String dirreqV2Resp;

  @Override
  public SortedMap<String, Integer> getDirreqV2Resp() {
    return ParseHelper.convertCommaSeparatedKeyIntegerValueList(
        this.dirreqV2Resp);
  }

  private String dirreqV3Resp;

  @Override
  public SortedMap<String, Integer> getDirreqV3Resp() {
    return ParseHelper.convertCommaSeparatedKeyIntegerValueList(
        this.dirreqV3Resp);
  }

  private String dirreqV2DirectDl;

  @Override
  public SortedMap<String, Integer> getDirreqV2DirectDl() {
    return ParseHelper.convertCommaSeparatedKeyIntegerValueList(
        this.dirreqV2DirectDl);
  }

  private String dirreqV3DirectDl;

  @Override
  public SortedMap<String, Integer> getDirreqV3DirectDl() {
    return ParseHelper.convertCommaSeparatedKeyIntegerValueList(
        this.dirreqV3DirectDl);
  }

  private String dirreqV2TunneledDl;

  @Override
  public SortedMap<String, Integer> getDirreqV2TunneledDl() {
    return ParseHelper.convertCommaSeparatedKeyIntegerValueList(
        this.dirreqV2TunneledDl);
  }

  private String dirreqV3TunneledDl;

  @Override
  public SortedMap<String, Integer> getDirreqV3TunneledDl() {
    return ParseHelper.convertCommaSeparatedKeyIntegerValueList(
        this.dirreqV3TunneledDl);
  }

  private BandwidthHistory dirreqReadHistory;

  @Override
  public BandwidthHistory getDirreqReadHistory() {
    return this.dirreqReadHistory;
  }

  private BandwidthHistory dirreqWriteHistory;

  @Override
  public BandwidthHistory getDirreqWriteHistory() {
    return this.dirreqWriteHistory;
  }

  private long entryStatsEndMillis = -1L;

  @Override
  public long getEntryStatsEndMillis() {
    return this.entryStatsEndMillis;
  }

  private long entryStatsIntervalLength = -1L;

  @Override
  public long getEntryStatsIntervalLength() {
    return this.entryStatsIntervalLength;
  }

  private String entryIps;

  @Override
  public SortedMap<String, Integer> getEntryIps() {
    return ParseHelper.convertCommaSeparatedKeyIntegerValueList(
        this.entryIps);
  }

  private long cellStatsEndMillis = -1L;

  @Override
  public long getCellStatsEndMillis() {
    return this.cellStatsEndMillis;
  }

  private long cellStatsIntervalLength = -1L;

  @Override
  public long getCellStatsIntervalLength() {
    return this.cellStatsIntervalLength;
  }

  private Integer[] cellProcessedCells;

  @Override
  public List<Integer> getCellProcessedCells() {
    return this.cellProcessedCells == null ? null
        : Arrays.asList(this.cellProcessedCells);
  }

  private Double[] cellQueuedCells;

  @Override
  public List<Double> getCellQueuedCells() {
    return this.cellQueuedCells == null ? null
        : Arrays.asList(this.cellQueuedCells);
  }

  private Integer[] cellTimeInQueue;

  @Override
  public List<Integer> getCellTimeInQueue() {
    return this.cellTimeInQueue == null ? null
        : Arrays.asList(this.cellTimeInQueue);
  }

  private int cellCircuitsPerDecile = -1;

  @Override
  public int getCellCircuitsPerDecile() {
    return this.cellCircuitsPerDecile;
  }

  private long connBiDirectStatsEndMillis = -1L;

  @Override
  public long getConnBiDirectStatsEndMillis() {
    return this.connBiDirectStatsEndMillis;
  }

  private long connBiDirectStatsIntervalLength = -1L;

  @Override
  public long getConnBiDirectStatsIntervalLength() {
    return this.connBiDirectStatsIntervalLength;
  }

  private int connBiDirectBelow = -1;

  @Override
  public int getConnBiDirectBelow() {
    return this.connBiDirectBelow;
  }

  private int connBiDirectRead = -1;

  @Override
  public int getConnBiDirectRead() {
    return this.connBiDirectRead;
  }

  private int connBiDirectWrite = -1;

  @Override
  public int getConnBiDirectWrite() {
    return this.connBiDirectWrite;
  }

  private int connBiDirectBoth = -1;

  @Override
  public int getConnBiDirectBoth() {
    return this.connBiDirectBoth;
  }

  private long ipv6ConnBiDirectStatsEndMillis = -1L;

  @Override
  public long getIpv6ConnBiDirectStatsEndMillis() {
    return this.ipv6ConnBiDirectStatsEndMillis;
  }

  private long ipv6ConnBiDirectStatsIntervalLength = -1L;

  @Override
  public long getIpv6ConnBiDirectStatsIntervalLength() {
    return this.ipv6ConnBiDirectStatsIntervalLength;
  }

  private int ipv6ConnBiDirectBelow = -1;

  @Override
  public int getIpv6ConnBiDirectBelow() {
    return this.ipv6ConnBiDirectBelow;
  }

  private int ipv6ConnBiDirectRead = -1;

  @Override
  public int getIpv6ConnBiDirectRead() {
    return this.ipv6ConnBiDirectRead;
  }

  private int ipv6ConnBiDirectWrite = -1;

  @Override
  public int getIpv6ConnBiDirectWrite() {
    return this.ipv6ConnBiDirectWrite;
  }

  private int ipv6ConnBiDirectBoth = -1;

  @Override
  public int getIpv6ConnBiDirectBoth() {
    return this.ipv6ConnBiDirectBoth;
  }

  private long exitStatsEndMillis = -1L;

  @Override
  public long getExitStatsEndMillis() {
    return this.exitStatsEndMillis;
  }

  private long exitStatsIntervalLength = -1L;

  @Override
  public long getExitStatsIntervalLength() {
    return this.exitStatsIntervalLength;
  }

  private SortedMap<String, Long> exitKibibytesWritten;

  @Override
  public SortedMap<String, Long> getExitKibibytesWritten() {
    return this.exitKibibytesWritten == null ? null
        : new TreeMap<>(this.exitKibibytesWritten);
  }

  private SortedMap<String, Long> exitKibibytesRead;

  @Override
  public SortedMap<String, Long> getExitKibibytesRead() {
    return this.exitKibibytesRead == null ? null
        : new TreeMap<>(this.exitKibibytesRead);
  }

  private SortedMap<String, Long> exitStreamsOpened;

  @Override
  public SortedMap<String, Long> getExitStreamsOpened() {
    return this.exitStreamsOpened == null ? null
        : new TreeMap<>(this.exitStreamsOpened);
  }

  private long geoipStartTimeMillis = -1L;

  @Override
  public long getGeoipStartTimeMillis() {
    return this.geoipStartTimeMillis;
  }

  private String geoipClientOrigins;

  @Override
  public SortedMap<String, Integer> getGeoipClientOrigins() {
    return ParseHelper.convertCommaSeparatedKeyIntegerValueList(
        this.geoipClientOrigins);
  }

  private long bridgeStatsEndMillis = -1L;

  @Override
  public long getBridgeStatsEndMillis() {
    return this.bridgeStatsEndMillis;
  }

  private long bridgeStatsIntervalLength = -1L;

  @Override
  public long getBridgeStatsIntervalLength() {
    return this.bridgeStatsIntervalLength;
  }

  private String bridgeIps;

  @Override
  public SortedMap<String, Integer> getBridgeIps() {
    return ParseHelper.convertCommaSeparatedKeyIntegerValueList(
        this.bridgeIps);
  }

  private String bridgeIpVersions;

  @Override
  public SortedMap<String, Integer> getBridgeIpVersions() {
    return ParseHelper.convertCommaSeparatedKeyIntegerValueList(
        this.bridgeIpVersions);
  }

  private String bridgeIpTransports;

  @Override
  public SortedMap<String, Integer> getBridgeIpTransports() {
    return ParseHelper.convertCommaSeparatedKeyIntegerValueList(
        this.bridgeIpTransports);
  }

  private List<String> transports = new ArrayList<>();

  @Override
  public List<String> getTransports() {
    return new ArrayList<>(this.transports);
  }

  private long hidservStatsEndMillis = -1L;

  @Override
  public long getHidservStatsEndMillis() {
    return this.hidservStatsEndMillis;
  }

  private long hidservStatsIntervalLength = -1L;

  @Override
  public long getHidservStatsIntervalLength() {
    return this.hidservStatsIntervalLength;
  }

  private Double hidservRendRelayedCells;

  @Override
  public Double getHidservRendRelayedCells() {
    return this.hidservRendRelayedCells;
  }

  private Map<String, Double> hidservRendRelayedCellsParameters;

  @Override
  public Map<String, Double> getHidservRendRelayedCellsParameters() {
    return this.hidservRendRelayedCellsParameters == null ? null
        : new HashMap<>(this.hidservRendRelayedCellsParameters);
  }

  private Double hidservDirOnionsSeen;

  @Override
  public Double getHidservDirOnionsSeen() {
    return this.hidservDirOnionsSeen;
  }

  private Map<String, Double> hidservDirOnionsSeenParameters;

  @Override
  public Map<String, Double> getHidservDirOnionsSeenParameters() {
    return this.hidservDirOnionsSeenParameters == null ? null
        : new HashMap<>(this.hidservDirOnionsSeenParameters);
  }

  private long hidservV3StatsEndMillis = -1L;

  @Override
  public long getHidservV3StatsEndMillis() {
    return this.hidservV3StatsEndMillis;
  }

  private long hidservV3StatsIntervalLength = -1L;

  @Override
  public long getHidservV3StatsIntervalLength() {
    return this.hidservV3StatsIntervalLength;
  }

  private Double hidservRendV3RelayedCells;

  @Override
  public Double getHidservRendV3RelayedCells() {
    return this.hidservRendV3RelayedCells;
  }

  private Map<String, Double> hidservRendV3RelayedCellsParameters;

  @Override
  public Map<String, Double> getHidservRendV3RelayedCellsParameters() {
    return this.hidservRendV3RelayedCellsParameters == null ? null
        : new HashMap<>(this.hidservRendV3RelayedCellsParameters);
  }

  private Double hidservDirV3OnionsSeen;

  @Override
  public Double getHidservDirV3OnionsSeen() {
    return this.hidservDirV3OnionsSeen;
  }

  private Map<String, Double> hidservDirV3OnionsSeenParameters;

  @Override
  public Map<String, Double> getHidservDirV3OnionsSeenParameters() {
    return this.hidservDirV3OnionsSeenParameters == null ? null
        : new HashMap<>(this.hidservDirV3OnionsSeenParameters);
  }

  private long paddingCountsStatsEndMillis = -1L;

  @Override
  public long getPaddingCountsStatsEndMillis() {
    return this.paddingCountsStatsEndMillis;
  }

  private long paddingCountsStatsIntervalLength = -1L;

  @Override
  public long getPaddingCountsStatsIntervalLength() {
    return this.paddingCountsStatsIntervalLength;
  }

  private Map<String, Long> paddingCounts;

  @Override
  public Map<String, Long> getPaddingCounts() {
    return this.paddingCounts == null ? null
        : new HashMap<>(this.paddingCounts);
  }

  private String routerSignature;

  @Override
  public String getRouterSignature() {
    return this.routerSignature;
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
}
