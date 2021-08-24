/* Copyright 2018--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.torproject.descriptor.Method;
import org.torproject.descriptor.WebServerAccessLog;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebServerAccessLogLine implements WebServerAccessLog.Line {

  private static final long serialVersionUID = 6160416810587561460L;

  private static final Logger logger = LoggerFactory
      .getLogger(WebServerAccessLogLine.class);

  private static final String DATE_PATTERN = "dd/MMM/yyyy";
  private static final String DASH = "-";

  private static final DateTimeFormatter dateTimeFormatter
      = DateTimeFormatter.ofPattern(DATE_PATTERN + ":HH:mm:ss xxxx");

  private static Pattern logLinePattern = Pattern.compile(
      "^((?:\\d{1,3}\\.){3}\\d{1,3}) (\\S+) (\\S+) "
      + "\\[([\\w/]+)([\\w:]+)(\\s[+\\-]\\d{4})\\] "
      + "\"([A-Z]+) ([^\"]+) ([A-Z]+/\\d\\.\\d)\" "
      + "(\\d{3}) (\\d+|-)(.*)");

  private static Map<String, String> ipMap
      = Collections.synchronizedMap(new HashMap<>());
  private static Map<LocalDate, LocalDate> dateMap
      = Collections.synchronizedMap(new HashMap<>(500));
  private static Map<String, String> protocolMap
      = Collections.synchronizedMap(new HashMap<>());
  private static Map<String, String> requestMap
      = Collections.synchronizedMap(new HashMap<>(50_000));

  private String ip;
  private int response;
  private String request;
  private Method method;
  private LocalDate date;
  private int size = -1;
  private boolean valid = false;
  private String protocol;

  /** Returns a log line string. Possibly empty. */
  @Override
  public String toLogString() {
    if (!this.valid) {
      return "";
    }
    return toString();
  }

  @Override
  public String toString() {
    return String.format("%s - - [%s:00:00:00 +0000] \"%s %s %s\" %d %s",
        this.ip, this.getDateString(), this.method.name(), this.request,
        this.protocol, this.response, this.size < 0 ? DASH : this.size);
  }

  /** Only used internally during sanitization.
   * Returns the string of the date using 'dd/MMM/yyyy' format. */
  public String getDateString() {
    return this.date.format(DateTimeFormatter.ofPattern(DATE_PATTERN));
  }

  @Override
  public String getIp() {
    return this.ip;
  }

  /** Only used internally during sanitization. */
  public void setIp(String ip) {
    this.ip = fromMap(ip, ipMap);
  }

  @Override
  public Method getMethod() {
    return this.method;
  }

  @Override
  public String getProtocol() {
    return this.protocol;
  }

  @Override
  public String getRequest() {
    return this.request;
  }

  @Override
  public Optional<Integer> getSize() {
    return this.size < 0 ? Optional.empty() : Optional.of(this.size);
  }

  @Override
  public int getResponse() {
    return this.response;
  }

  /** Only used internally during sanitization. */
  public void setRequest(String request) {
    this.request = fromMap(request, requestMap);
  }

  @Override
  public LocalDate getDate() {
    return this.date;
  }

  @Override
  public boolean isValid() {
    return this.valid;
  }

  /** Creates a Line from a string. */
  public static WebServerAccessLogLine makeLine(String line) {
    WebServerAccessLogLine res = new WebServerAccessLogLine();
    try {
      Matcher mat = logLinePattern.matcher(line);
      if (mat.find()) {
        res.response = Integer.valueOf(mat.group(10));
        res.method = Method.valueOf(mat.group(7));
        String dateTimeString = mat.group(4) + mat.group(5) + mat.group(6);
        res.date = fromMap(ZonedDateTime.parse(dateTimeString,
            dateTimeFormatter).withZoneSameInstant(ZoneOffset.UTC)
            .toLocalDate(), dateMap);
        res.ip = fromMap(mat.group(1), ipMap);
        res.request = fromMap(mat.group(8), requestMap);
        res.protocol = fromMap(mat.group(9), protocolMap);
        if (DASH.equals(mat.group(11))) {
          res.size = -1;
        } else {
          res.size = Integer.valueOf(mat.group(11));
        }
        res.valid = true;
      }
    } catch (Throwable th) {
      logger.debug("Unmatchable line: '{}'.", line, th);
      return new WebServerAccessLogLine();
    }
    return res;
  }

  private static <T> T fromMap(T val, Map<T, T> map) {
    synchronized (map) {
      map.putIfAbsent(Objects.requireNonNull(val), val);
      return map.get(val);
    }
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof WebServerAccessLogLine) {
      return this.toLogString()
          .equals(((WebServerAccessLogLine)other).toLogString());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.toLogString().hashCode();
  }

}

