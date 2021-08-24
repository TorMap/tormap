/* Copyright 2017--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.log;

import org.torproject.descriptor.DescriptorParseException;
import org.torproject.descriptor.WebServerAccessLog;
import org.torproject.descriptor.internal.FileType;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Implementation of web server access log descriptors.
 *
 * <p>Defines sanitization and validation for web server access logs.</p>
 *
 * @since 2.2.0
 */
public class WebServerAccessLogImpl extends LogDescriptorImpl
    implements InternalWebServerAccessLog, WebServerAccessLog {

  private static final long serialVersionUID = 7528914359452568309L;

  /** The log's name should include this string. */
  public static final String MARKER = InternalWebServerAccessLog.MARKER;

  /** The mandatory web server log descriptor file name pattern. */
  public static final Pattern filenamePattern
      = Pattern.compile("(\\S*)" + SEP + "(\\S*)" + SEP + "" + MARKER
      + SEP + "(\\d*)(?:\\.?)([a-zA-Z]*)");

  private final String physicalHost;

  private final String virtualHost;

  private final LocalDate logDate;

  private boolean validate = true;

  /**
   * Creates a WebServerAccessLog from the given bytes and filename.
   *
   * <p>The given bytes are read, whereas the file is not read.</p>
   *
   * <p>The path of the given file has to be compliant to the following
   * naming pattern
   * {@code
   * <virtualHost>-<physicalHost>-access.log-<yyyymmdd>.<compression>},
   * where an unknown compression type (see {@link #getCompressionType})
   * is interpreted as missing compression.  In this case the bytes
   * will be compressed to the default compression type.
   * The immediate parent name is taken to be the physical host collecting the
   * logs.</p>
   */
  protected WebServerAccessLogImpl(byte[] logBytes, File file, String logName)
      throws DescriptorParseException {
    this(logBytes, file, logName, FileType.XZ);
  }

  /** For internal use only. */
  public WebServerAccessLogImpl(byte[] bytes, String filename,
      boolean validate) throws DescriptorParseException {
    this(bytes, null, filename, FileType.XZ, validate);
  }

  /** For internal use only. */
  public WebServerAccessLogImpl(byte[] bytes, File sourceFile, String filename,
      boolean validate) throws DescriptorParseException {
    this(bytes, sourceFile, filename, FileType.XZ, validate);
  }

  private WebServerAccessLogImpl(byte[] logBytes, File file, String logName,
      FileType defaultCompression) throws DescriptorParseException {
    this(logBytes, file, logName, defaultCompression, true);
  }

  private WebServerAccessLogImpl(byte[] logBytes, File file, String logName,
      FileType defaultCompression, boolean validate)
      throws DescriptorParseException {
    super(logBytes, file, logName, defaultCompression);
    try {
      String fn = Paths.get(logName).getFileName().toString();
      Matcher mat = filenamePattern.matcher(fn);
      if (!mat.find()) {
        throw new DescriptorParseException(
            "WebServerAccessLog file name doesn't comply to standard: " + fn);
      }
      this.virtualHost = mat.group(1);
      this.physicalHost = mat.group(2);
      if (null == this.virtualHost || null == this.physicalHost
          || this.virtualHost.isEmpty() || this.physicalHost.isEmpty()) {
        throw new DescriptorParseException(
            "WebServerAccessLog file name doesn't comply to standard: " + fn);
      }
      String ymd = mat.group(3);
      this.logDate = LocalDate.parse(ymd, DateTimeFormatter.BASIC_ISO_DATE);
      this.setValidator((line)
          -> WebServerAccessLogLine.makeLine(line).isValid());
      if (validate) {
        this.validate();
      }
    } catch (DescriptorParseException dpe) {
      throw dpe; // escalate
    } catch (Exception pe) {
      throw new DescriptorParseException(
          "Cannot parse WebServerAccessLog file: " + logName, pe);
    }
  }

  @Override
  public String getPhysicalHost() {
    return this.physicalHost;
  }

  @Override
  public String getVirtualHost() {
    return this.virtualHost;
  }

  @Override
  public LocalDate getLogDate() {
    return this.logDate;
  }

  private static final int LISTLIMIT = Integer.MAX_VALUE / 2;

  /** Returns a stream of all valid log lines. */
  @Override
  public Stream<WebServerAccessLog.Line> logLines()
      throws DescriptorParseException {
    try (BufferedReader br = new BufferedReader(new InputStreamReader(
        this.decompressedByteStream()))) {
      List<List<WebServerAccessLogLine>> lists = new ArrayList<>();
      List<WebServerAccessLogLine> currentList = new ArrayList<>();
      lists.add(currentList);
      String lineStr = br.readLine();
      int count = 0;
      while (null != lineStr) {
        WebServerAccessLogLine wsal = WebServerAccessLogLine.makeLine(lineStr);
        if (wsal.isValid()) {
          currentList.add(wsal);
          count++;
        }
        if (count >= LISTLIMIT) {
          currentList = new ArrayList<>();
          lists.add(currentList);
          count = 0;
        }
        lineStr = br.readLine();
      }
      br.close();
      return lists.stream().flatMap(Collection::stream);
    } catch (Exception ex) {
      throw new DescriptorParseException("Cannot retrieve log lines.", ex);
    }
  }

}

