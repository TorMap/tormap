/* Copyright 2012--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.torproject.descriptor.Descriptor;
import org.torproject.descriptor.DescriptorParseException;
import org.torproject.descriptor.DescriptorParser;
import org.torproject.descriptor.log.LogDescriptorImpl;
import org.torproject.descriptor.onionperf.OnionPerfAnalysisConverter;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.torproject.descriptor.impl.DescriptorImpl.NL;
import static org.torproject.descriptor.impl.DescriptorImpl.SP;

public class DescriptorParserImpl implements DescriptorParser {

  private static final Logger logger
      = LoggerFactory.getLogger(DescriptorParserImpl.class);

  @Override
  public Iterable<Descriptor> parseDescriptors(byte[] rawDescriptorBytes,
      File sourceFile, String fileName) {
    try {
      return this.detectTypeAndParseDescriptors(rawDescriptorBytes,
          sourceFile, fileName);
    } catch (DescriptorParseException e) {
      logger.debug("Cannot parse descriptor file '{}'.", sourceFile, e);
      List<Descriptor> parsedDescriptors = new ArrayList<>();
      parsedDescriptors.add(new UnparseableDescriptorImpl(rawDescriptorBytes,
          new int[] { 0, rawDescriptorBytes.length }, sourceFile, e));
      return parsedDescriptors;
    }
  }

  private List<Descriptor> detectTypeAndParseDescriptors(
      byte[] rawDescriptorBytes, File sourceFile, String fileName)
      throws DescriptorParseException {
    byte[] first100Chars = new byte[Math.min(100,
        rawDescriptorBytes.length)];
    System.arraycopy(rawDescriptorBytes, 0, first100Chars, 0,
        first100Chars.length);
    String firstLines = new String(first100Chars);
    if (firstLines.startsWith("@type network-status-consensus-3 1.")
        || firstLines.startsWith(
        "@type network-status-microdesc-consensus-3 1.")
        || ((firstLines.startsWith(
        Key.NETWORK_STATUS_VERSION.keyword + SP + "3")
        || firstLines.contains(
        NL + Key.NETWORK_STATUS_VERSION.keyword + SP + "3"))
        && firstLines.contains(
        NL + Key.VOTE_STATUS.keyword + SP + "consensus" + NL))) {
      return this.parseOneOrMoreDescriptors(rawDescriptorBytes, sourceFile,
          Key.NETWORK_STATUS_VERSION, RelayNetworkStatusConsensusImpl.class);
    } else if (firstLines.startsWith("@type network-status-vote-3 1.")
        || ((firstLines.startsWith(
        Key.NETWORK_STATUS_VERSION.keyword + SP + "3" + NL)
        || firstLines.contains(
        NL + Key.NETWORK_STATUS_VERSION.keyword + SP + "3" + NL))
        && firstLines.contains(
        NL + Key.VOTE_STATUS.keyword + SP + "vote" + NL))) {
      return this.parseOneOrMoreDescriptors(rawDescriptorBytes, sourceFile,
          Key.NETWORK_STATUS_VERSION, RelayNetworkStatusVoteImpl.class);
    } else if (firstLines.startsWith("@type bridge-network-status 1.")
        || firstLines.startsWith(Key.R.keyword + SP)) {
      List<Descriptor> parsedDescriptors = new ArrayList<>();
      parsedDescriptors.add(new BridgeNetworkStatusImpl(
          rawDescriptorBytes, new int[] { 0, rawDescriptorBytes.length },
          sourceFile, fileName));
      return parsedDescriptors;
    } else if (firstLines.startsWith("@type bridge-server-descriptor 1.")) {
      return this.parseOneOrMoreDescriptors(rawDescriptorBytes, sourceFile,
          Key.ROUTER, BridgeServerDescriptorImpl.class);
    } else if (firstLines.startsWith("@type server-descriptor 1.")
        || firstLines.startsWith(Key.ROUTER.keyword + SP)
        || firstLines.contains(NL + Key.ROUTER.keyword + SP)) {
      return this.parseOneOrMoreDescriptors(rawDescriptorBytes, sourceFile,
          Key.ROUTER, RelayServerDescriptorImpl.class);
    } else if (firstLines.startsWith("@type bridge-extra-info 1.")) {
      return this.parseOneOrMoreDescriptors(rawDescriptorBytes, sourceFile,
          Key.EXTRA_INFO, BridgeExtraInfoDescriptorImpl.class);
    } else if (firstLines.startsWith("@type extra-info 1.")
        || firstLines.startsWith(Key.EXTRA_INFO.keyword + SP)
        || firstLines.contains(NL + Key.EXTRA_INFO.keyword + SP)) {
      return this.parseOneOrMoreDescriptors(rawDescriptorBytes, sourceFile,
          Key.EXTRA_INFO, RelayExtraInfoDescriptorImpl.class);
    } else if (firstLines.startsWith("@type microdescriptor 1.")
        || firstLines.startsWith(Key.ONION_KEY.keyword + NL)
        || firstLines.contains(NL + Key.ONION_KEY.keyword + NL)) {
      return this.parseOneOrMoreDescriptors(rawDescriptorBytes, sourceFile,
          Key.ONION_KEY, MicrodescriptorImpl.class);
    } else if (firstLines.startsWith("@type bridge-pool-assignment 1.")
        || firstLines.startsWith(Key.BRIDGE_POOL_ASSIGNMENT.keyword + SP)
        || firstLines.contains(NL + Key.BRIDGE_POOL_ASSIGNMENT.keyword + SP)) {
      return this.parseOneOrMoreDescriptors(rawDescriptorBytes, sourceFile,
          Key.BRIDGE_POOL_ASSIGNMENT, BridgePoolAssignmentImpl.class);
    } else if (firstLines.startsWith("@type dir-key-certificate-3 1.")
        || firstLines.startsWith(Key.DIR_KEY_CERTIFICATE_VERSION.keyword + SP)
        || firstLines.contains(
        NL + Key.DIR_KEY_CERTIFICATE_VERSION.keyword + SP)) {
      return this.parseOneOrMoreDescriptors(rawDescriptorBytes, sourceFile,
          Key.DIR_KEY_CERTIFICATE_VERSION, DirectoryKeyCertificateImpl.class);
    } else if (firstLines.startsWith("@type tordnsel 1.")
        || firstLines.startsWith("ExitNode" + SP)
        || firstLines.contains(NL + "ExitNode" + SP)) {
      List<Descriptor> parsedDescriptors = new ArrayList<>();
      parsedDescriptors.add(new ExitListImpl(rawDescriptorBytes, sourceFile,
          fileName));
      return parsedDescriptors;
    } else if (firstLines.startsWith("@type network-status-2 1.")
        || firstLines.startsWith(
        Key.NETWORK_STATUS_VERSION.keyword + SP + "2" + NL)
        || firstLines.contains(
        NL + Key.NETWORK_STATUS_VERSION.keyword + SP + "2" + NL)) {
      return this.parseOneOrMoreDescriptors(rawDescriptorBytes, sourceFile,
          Key.NETWORK_STATUS_VERSION, RelayNetworkStatusImpl.class);
    } else if (firstLines.startsWith("@type directory 1.")
        || firstLines.startsWith(Key.SIGNED_DIRECTORY.keyword + NL)
        || firstLines.contains(NL + Key.SIGNED_DIRECTORY.keyword + NL)) {
      return this.parseOneOrMoreDescriptors(rawDescriptorBytes, sourceFile,
          Key.SIGNED_DIRECTORY, RelayDirectoryImpl.class);
    } else if (firstLines.startsWith("@type torperf 1.")) {
      return TorperfResultImpl.parseTorperfResults(rawDescriptorBytes,
          sourceFile);
    } else if (firstLines.startsWith("@type snowflake-stats 1.")
        || firstLines.startsWith(Key.SNOWFLAKE_STATS_END.keyword + SP)
        || firstLines.contains(NL + Key.SNOWFLAKE_STATS_END.keyword + SP)) {
      return this.parseOneOrMoreDescriptors(rawDescriptorBytes, sourceFile,
          Key.SNOWFLAKE_STATS_END, SnowflakeStatsImpl.class);
    } else if (firstLines.startsWith("@type bridgedb-metrics 1.")
        || firstLines.startsWith(Key.BRIDGEDB_METRICS_END.keyword + SP)
        || firstLines.contains(NL + Key.BRIDGEDB_METRICS_END.keyword + SP)) {
      return this.parseOneOrMoreDescriptors(rawDescriptorBytes, sourceFile,
              Key.BRIDGEDB_METRICS_END, BridgedbMetricsImpl.class);
    } else if (firstLines.startsWith("@type bridgestrap-stats 1.")
        || firstLines.startsWith(Key.BRIDGESTRAP_STATS_END.keyword + SP)
        || firstLines.contains(NL + Key.BRIDGESTRAP_STATS_END.keyword + SP)) {
      return this.parseOneOrMoreDescriptors(rawDescriptorBytes, sourceFile,
              Key.BRIDGESTRAP_STATS_END, BridgestrapStatsImpl.class);
    } else if (firstLines.startsWith("@type bandwidth-file 1.")
        || firstLines.matches("(?s)[0-9]{10}\\n.*")) {
      /* Identifying bandwidth files by a 10-digit timestamp in the first line
       * breaks with files generated before 2002 or after 2286 and when the next
       * descriptor identifier starts with just a timestamp in the first line
       * rather than a document type identifier. */
      List<Descriptor> parsedDescriptors = new ArrayList<>();
      parsedDescriptors.add(new BandwidthFileImpl(rawDescriptorBytes,
          sourceFile));
      return parsedDescriptors;
    } else if (null != fileName
        && fileName.contains(LogDescriptorImpl.MARKER)) {
      return LogDescriptorImpl.parse(rawDescriptorBytes, sourceFile,
          fileName);
    } else if (null != fileName
        && fileName.endsWith(".onionperf.analysis.json.xz")) {
      return new OnionPerfAnalysisConverter(rawDescriptorBytes, sourceFile)
          .asTorperfResults();
    } else if (null != fileName
        && (fileName.equals("geoip") || fileName.equals("geoip6"))) {
      return GeoipFileImpl.parse(rawDescriptorBytes, sourceFile);
    } else if (null != fileName
            && (fileName.equals("countries.txt")
              || fileName.equals("asn.txt"))) {
      return GeoipNamesFileImpl.parse(rawDescriptorBytes, sourceFile);
    } else {
      throw new DescriptorParseException("Could not detect descriptor "
          + "type in descriptor starting with '" + firstLines + "'.");
    }
  }

  private List<Descriptor> parseOneOrMoreDescriptors(byte[] rawDescriptorBytes,
      File sourceFile, Key key,
      Class<? extends DescriptorImpl> descriptorClass) {
    List<Descriptor> parsedDescriptors = new ArrayList<>();
    Constructor<? extends DescriptorImpl> constructor;
    try {
      constructor = descriptorClass.getDeclaredConstructor(byte[].class,
          int[].class, File.class);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
    int startAnnotations = 0;
    int endAllDescriptors = rawDescriptorBytes.length;
    String ascii = new String(rawDescriptorBytes, StandardCharsets.US_ASCII);
    boolean containsAnnotations = ascii.startsWith("@")
        || ascii.contains(NL + "@");
    boolean containsKeywordSpace = ascii.startsWith(key.keyword + SP)
        || ascii.contains(NL + key.keyword + SP);
    boolean containsKeywordNewline = ascii.startsWith(key.keyword + NL)
        || ascii.contains(NL + key.keyword + NL);
    while (startAnnotations < endAllDescriptors) {
      int startDescriptor = -1;
      if ((containsKeywordSpace
          && startAnnotations == ascii.indexOf(key.keyword + SP,
          startAnnotations))
          || (containsKeywordNewline
          && startAnnotations == ascii.indexOf(key.keyword + NL,
          startAnnotations))) {
        startDescriptor = startAnnotations;
      } else {
        if (containsKeywordSpace) {
          startDescriptor = ascii.indexOf(NL + key.keyword + SP,
              startAnnotations - 1);
        }
        if (startDescriptor < 0 && containsKeywordNewline) {
          startDescriptor = ascii.indexOf(NL + key.keyword + NL,
              startAnnotations - 1);
        }
        if (startDescriptor < 0) {
          break;
        } else {
          startDescriptor += 1;
        }
      }
      int endDescriptor = -1;
      if (containsAnnotations) {
        endDescriptor = ascii.indexOf(NL + "@", startDescriptor);
      }
      if (endDescriptor < 0 && containsKeywordSpace) {
        endDescriptor = ascii.indexOf(NL + key.keyword + SP, startDescriptor);
      }
      if (endDescriptor < 0 && containsKeywordNewline) {
        endDescriptor = ascii.indexOf(NL + key.keyword + NL, startDescriptor);
      }
      if (endDescriptor < 0) {
        endDescriptor = endAllDescriptors - 1;
      }
      endDescriptor += 1;
      int[] offsetAndLength = new int[] { startAnnotations,
          endDescriptor - startAnnotations };
      try {
        parsedDescriptors.add(this.parseOneDescriptor(rawDescriptorBytes,
            offsetAndLength, sourceFile, constructor));
      } catch (DescriptorParseException e) {
        parsedDescriptors.add(new UnparseableDescriptorImpl(
            rawDescriptorBytes, offsetAndLength, sourceFile, e));
      }
      startAnnotations = endDescriptor;
    }
    return parsedDescriptors;
  }

  Descriptor parseOneDescriptor(byte[] rawDescriptorBytes,
      int[] offsetAndLength, File sourceFile,
      Constructor<? extends DescriptorImpl> constructor)
      throws DescriptorParseException {
    try {
      return constructor.newInstance(rawDescriptorBytes, offsetAndLength,
          sourceFile);
    } catch (InvocationTargetException e) {
      if (null != e.getCause()
          && e.getCause() instanceof DescriptorParseException) {
        throw (DescriptorParseException) e.getCause();
      } else {
        throw new RuntimeException(e);
      }
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }
}
