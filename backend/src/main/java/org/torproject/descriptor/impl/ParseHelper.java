/* Copyright 2011--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.torproject.descriptor.DescriptorParseException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Parse helper for descriptor contents.
 *
 * <p>Naming convention: methods starting with {@code parse} return valid and
 * sometimes changed data items (e.g. string to port int), methods starting with
 * {@code verify} only check data items without returning them, and methods
 * starting with {@code convert} return checked and explicitly changed data
 * items (e.g. base64 to hex encoding).</p>
 */
public class ParseHelper {

  private static Pattern keywordPattern = Pattern.compile("^[A-Za-z0-9-]+$");

  protected static String parseKeyword(String line, String keyword)
      throws DescriptorParseException {
    if (!keywordPattern.matcher(keyword).matches()) {
      throw new DescriptorParseException("Unrecognized character in "
          + "keyword '" + keyword + "' in line '" + line + "'.");
    }
    return keyword;
  }

  private static Pattern ipv4Pattern = Pattern.compile("^[0-9.]{7,15}$");

  protected static String parseIpv4Address(String line, String address)
      throws DescriptorParseException {
    boolean isValid = true;
    if (!ipv4Pattern.matcher(address).matches()) {
      isValid = false;
    } else {
      String[] parts = address.split("\\.", -1);
      if (parts.length != 4) {
        isValid = false;
      } else {
        for (int i = 0; i < 4; i++) {
          try {
            int octetValue = Integer.parseInt(parts[i]);
            if (octetValue < 0 || octetValue > 255) {
              isValid = false;
            }
          } catch (NumberFormatException e) {
            isValid = false;
          }
        }
      }
    }
    if (!isValid) {
      throw new DescriptorParseException("'" + address + "' in line '"
          + line + "' is not a valid IPv4 address.");
    }
    return address;
  }

  protected static int parsePort(String line, String portString)
      throws DescriptorParseException {
    int port;
    try {
      port = Integer.parseInt(portString);
    } catch (NumberFormatException e) {
      throw new DescriptorParseException("'" + portString + "' in line '"
          + line + "' is not a valid port number.");
    }
    if (port < 0 || port > 65535) {
      throw new DescriptorParseException("'" + portString + "' in line '"
          + line + "' is not a valid port number.");
    }
    return port;
  }

  protected static long parseSeconds(String line, String secondsString)
      throws DescriptorParseException {
    try {
      return Long.parseLong(secondsString);
    } catch (NumberFormatException e) {
      throw new DescriptorParseException("'" + secondsString + "' in "
          + "line '" + line + "' is not a valid time in seconds.");
    }
  }

  static Duration parseDuration(String line, String secondsString)
      throws DescriptorParseException {
    long parsedSeconds = parseSeconds(line, secondsString);
    if (parsedSeconds <= 0L) {
      throw new DescriptorParseException("Duration must be positive in line '"
          + line + "'.");
    }
    return Duration.ofSeconds(parsedSeconds);
  }

  protected static Long parseLong(String line, String[] parts, int index)
      throws DescriptorParseException {
    if (index >= parts.length) {
      throw new DescriptorParseException(String.format(
          "Line '%s' does not contain a long value at index %d.", line, index));
    }
    try {
      return Long.parseLong(parts[index]);
    } catch (NumberFormatException e) {
      throw new DescriptorParseException(String.format(
          "Unable to parse long value '%s' in line '%s'.", parts[index], line));
    }
  }

  protected static String parseExitPattern(String line, String exitPattern)
      throws DescriptorParseException {
    if (!exitPattern.contains(":")) {
      throw new DescriptorParseException("'" + exitPattern + "' in line '"
          + line + "' must contain address and port.");
    }
    String[] parts = exitPattern.split(":");
    String addressPart = parts[0];
    /* TODO Extend to IPv6. */
    if (addressPart.equals("*")) {
      /* Nothing to check. */
    } else if (addressPart.contains("/")) {
      String[] addressParts = addressPart.split("/");
      String address = addressParts[0];
      String mask = addressParts[1];
      ParseHelper.parseIpv4Address(line, address);
      if (addressParts.length != 2) {
        throw new DescriptorParseException("'" + addressPart + "' in "
            + "line '" + line + "' is not a valid address part.");
      }
      if (mask.contains(".")) {
        ParseHelper.parseIpv4Address(line, mask);
      } else {
        int maskValue = -1;
        try {
          maskValue = Integer.parseInt(mask);
        } catch (NumberFormatException e) {
          /* Handle below. */
        }
        if (maskValue < 0 || maskValue > 32) {
          throw new DescriptorParseException("'" + mask + "' in line '"
              + line + "' is not a valid IPv4 mask.");
        }
      }
    } else {
      ParseHelper.parseIpv4Address(line, addressPart);
    }
    String portPart = parts[1];
    if (portPart.equals("*")) {
      /* Nothing to check. */
    } else if (portPart.contains("-")) {
      String[] portParts = portPart.split("-");
      String fromPort = portParts[0];
      ParseHelper.parsePort(line, fromPort);
      String toPort = portParts[1];
      ParseHelper.parsePort(line, toPort);
    } else {
      ParseHelper.parsePort(line, portPart);
    }
    return exitPattern;
  }

  private static ThreadLocal<Map<String, DateFormat>> dateFormats =
      ThreadLocal.withInitial(HashMap::new);

  static DateFormat getDateFormat(String format) {
    Map<String, DateFormat> threadDateFormats = dateFormats.get();
    if (!threadDateFormats.containsKey(format)) {
      DateFormat dateFormat = new SimpleDateFormat(format, Locale.US);
      dateFormat.setLenient(false);
      dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      threadDateFormats.put(format, dateFormat);
    }
    return threadDateFormats.get(format);
  }

  protected static long parseTimestampAtIndex(String line, String[] parts,
      int dateIndex, int timeIndex) throws DescriptorParseException {
    if (dateIndex >= parts.length || timeIndex >= parts.length) {
      throw new DescriptorParseException("Line '" + line + "' does not "
          + "contain a timestamp at the expected position.");
    }
    long result = -1L;
    try {
      DateFormat dateTimeFormat = getDateFormat("yyyy-MM-dd HH:mm:ss");
      result = dateTimeFormat.parse(
          parts[dateIndex] + " " + parts[timeIndex]).getTime();
    } catch (ParseException e) {
      /* Leave result at -1L. */
    }
    if (result < 0L || result / 1000L > (long) Integer.MAX_VALUE) {
      throw new DescriptorParseException("Illegal timestamp format in "
          + "line '" + line + "'.");
    }
    return result;
  }

  static LocalDateTime parseLocalDateTime(String line, String[] parts,
      int dateIndex, int timeIndex) throws DescriptorParseException {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(
        parseTimestampAtIndex(line, parts, dateIndex, timeIndex)),
        ZoneOffset.UTC);
  }

  protected static String parseTwentyByteHexString(String line,
      String hexString) throws DescriptorParseException {
    return parseHexString(line, hexString, 40);
  }

  protected static String parseHexString(String line, String hexString)
      throws DescriptorParseException {
    return parseHexString(line, hexString, -1);
  }

  private static Pattern hexPattern = Pattern.compile("^[0-9a-fA-F]*$");

  private static String parseHexString(String line, String hexString,
      int expectedLength) throws DescriptorParseException {
    if (!hexPattern.matcher(hexString).matches()
        || hexString.length() % 2 != 0
        || (expectedLength >= 0
        && hexString.length() != expectedLength)) {
      throw new DescriptorParseException("Illegal hex string in line '"
          + line + "'.");
    }
    return hexString.toUpperCase();
  }

  protected static SortedMap<String, String> parseKeyValueStringPairs(
      String line, String[] parts, int startIndex)
      throws DescriptorParseException {
    return (new KeyValueMap<>(String.class))
        .parseKeyValueList(line, parts, startIndex, 0, " ");
  }

  protected static SortedMap<String, Integer> parseKeyValueIntegerPairs(
      String line, String[] parts, int startIndex)
      throws DescriptorParseException {
    return (new KeyValueMap<>(Integer.class))
        .parseKeyValueList(line, parts, startIndex, 0, " ");
  }

  private static Pattern nicknamePattern =
      Pattern.compile("^[0-9a-zA-Z]{1,19}$");

  protected static String parseNickname(String line, String nickname)
      throws DescriptorParseException {
    if (!nicknamePattern.matcher(nickname).matches()) {
      throw new DescriptorParseException("Illegal nickname in line '"
          + line + "'.");
    }
    return nickname;
  }

  protected static boolean parseBoolean(String boolString, String line)
      throws DescriptorParseException {
    switch (boolString) {
      case "1":
        return true;
      case "0":
        return false;
      default:
        throw new DescriptorParseException("Illegal line '" + line
            + "'.");
    }
  }

  private static Pattern twentyByteBase64Pattern =
      Pattern.compile("^[0-9a-zA-Z+/]{27}$");

  protected static void verifyTwentyByteBase64String(String line,
      String base64String) throws DescriptorParseException {
    convertTwentyByteBase64StringToHex(line, base64String);
  }

  protected static String convertTwentyByteBase64StringToHex(String line,
      String base64String) throws DescriptorParseException {
    if (!twentyByteBase64Pattern.matcher(base64String).matches()) {
      throw new DescriptorParseException("'" + base64String
          + "' in line '" + line + "' is not a valid base64-encoded "
          + "20-byte value.");
    }
    return Hex.encodeHexString(Base64.decodeBase64(base64String + "="))
        .toUpperCase();
  }

  private static Pattern thirtyTwoByteBase64Pattern =
      Pattern.compile("^[0-9a-zA-Z+/]{43}$");

  protected static void verifyThirtyTwoByteBase64String(String line,
      String base64String) throws DescriptorParseException {
    if (!thirtyTwoByteBase64Pattern.matcher(base64String).matches()) {
      throw new DescriptorParseException("'" + base64String
          + "' in line '" + line + "' is not a valid base64-encoded "
          + "32-byte value.");
    }
  }

  protected static String parseCommaSeparatedKeyIntegerValueList(
      String line, String[] partsNoOpt, int index, int keyLength)
      throws DescriptorParseException {
    return parseStringKeyIntegerList(line, partsNoOpt, index, keyLength);
  }

  protected static SortedMap<String, Integer>
      convertCommaSeparatedKeyIntegerValueList(String validatedString) {
    if (null == validatedString) {
      return null;
    }
    KeyValueMap<Integer> result = new KeyValueMap<>(Integer.class);
    if (!validatedString.isEmpty()) {
      try {
        result.parseKeyValueList(validatedString,
            new String[]{ validatedString }, 0, 0, ",");
      } catch (DescriptorParseException e) {
        throw new RuntimeException("Should have been caught in earlier "
            + "validation step, but wasn't. ", e);
      }
    }
    return result;
  }

  protected static SortedMap<String, Long>
      parseCommaSeparatedKeyLongValueList(String line,
      String[] partsNoOpt, int index, int keyLength)
      throws DescriptorParseException {
    return (new KeyValueMap<>(Long.class))
        .parseKeyValueList(line, partsNoOpt, index, keyLength, ",");
  }

  protected static Integer[] parseCommaSeparatedIntegerValueList(
      String line, String[] partsNoOpt, int index)
      throws DescriptorParseException {
    Integer[] result = null;
    if (partsNoOpt.length < index) {
      throw new DescriptorParseException("Line '" + line + "' does not "
          + "contain a comma-separated value list at index " + index
          + ".");
    } else if (partsNoOpt.length > index) {
      String[] listElements = partsNoOpt[index].split(",", -1);
      result = new Integer[listElements.length];
      for (int i = 0; i < listElements.length; i++) {
        try {
          result[i] = Integer.parseInt(listElements[i]);
        } catch (NumberFormatException e) {
          throw new DescriptorParseException("Line '" + line + "' "
              + "contains an illegal value in list element '"
              + listElements[i] + "'.");
        }
      }
    }
    return result;
  }

  protected static Double[] parseCommaSeparatedDoubleValueList(
      String line, String[] partsNoOpt, int index)
      throws DescriptorParseException {
    Double[] result = null;
    if (partsNoOpt.length < index) {
      throw new DescriptorParseException("Line '" + line + "' does not "
          + "contain a comma-separated value list at index " + index
          + ".");
    } else if (partsNoOpt.length > index) {
      String[] listElements = partsNoOpt[index].split(",", -1);
      result = new Double[listElements.length];
      for (int i = 0; i < listElements.length; i++) {
        try {
          result[i] = Double.parseDouble(listElements[i]);
        } catch (NumberFormatException e) {
          throw new DescriptorParseException("Line '" + line + "' "
              + "contains an illegal value in list element '"
              + listElements[i] + "'.");
        }
      }
    }
    return result;
  }

  protected static Map<String, Double>
      parseSpaceSeparatedStringKeyDoubleValueMap(String line,
      String[] partsNoOpt, int startIndex)
      throws DescriptorParseException {
    return (new KeyValueMap<>(Double.class))
        .parseKeyValueList(line, partsNoOpt, startIndex, -1, " ");
  }

  protected static Map<String, Long>
      parseSpaceSeparatedStringKeyLongValueMap(String line,
      String[] partsNoOpt, int startIndex)
      throws DescriptorParseException {
    return (new KeyValueMap<>(Long.class))
        .parseKeyValueList(line, partsNoOpt, startIndex, -1, " ");
  }

  private static String parseStringKeyIntegerList(String line,
      String[] partsNoOpt, int startIndex, int keyLength)
      throws DescriptorParseException {
    if (startIndex >= partsNoOpt.length) {
      return "";
    }
    KeyValueMap<Integer> result = new KeyValueMap<>(Integer.class);
    result.parseKeyValueList(line, partsNoOpt, startIndex, keyLength, ",");
    return partsNoOpt[startIndex];
  }

  protected static String
      parseMasterKeyEd25519FromIdentityEd25519CryptoBlock(
      String identityEd25519CryptoBlock) throws DescriptorParseException {
    String identityEd25519CryptoBlockNoNewlines =
        identityEd25519CryptoBlock.replaceAll("\n", "");
    String beginEd25519CertLine = "-----BEGIN ED25519 CERT-----";
    String endEd25519CertLine = "-----END ED25519 CERT-----";
    if (!identityEd25519CryptoBlockNoNewlines.startsWith(
        beginEd25519CertLine)) {
      throw new DescriptorParseException("Illegal start of "
          + "identity-ed25519 crypto block '" + identityEd25519CryptoBlock
          + "'.");
    }
    if (!identityEd25519CryptoBlockNoNewlines.endsWith(
        endEd25519CertLine)) {
      throw new DescriptorParseException("Illegal end of "
          + "identity-ed25519 crypto block '" + identityEd25519CryptoBlock
          + "'.");
    }
    String identityEd25519Base64 = identityEd25519CryptoBlockNoNewlines
        .substring(beginEd25519CertLine.length(),
        identityEd25519CryptoBlock.length()
        - endEd25519CertLine.length()).replaceAll("=", "");
    byte[] identityEd25519 = Base64.decodeBase64(identityEd25519Base64);
    if (identityEd25519.length < 40) {
      throw new DescriptorParseException("Invalid length of "
          + "identity-ed25519 (in bytes): " + identityEd25519.length);
    } else if (identityEd25519[0] != 0x01) {
      throw new DescriptorParseException("Unknown version in "
          + "identity-ed25519: " + identityEd25519[0]);
    } else if (identityEd25519[1] != 0x04) {
      throw new DescriptorParseException("Unknown cert type in "
          + "identity-ed25519: " + identityEd25519[1]);
    } else if (identityEd25519[6] != 0x01) {
      throw new DescriptorParseException("Unknown certified key type in "
          + "identity-ed25519: " + identityEd25519[1]);
    } else if (identityEd25519[39] == 0x00) {
      throw new DescriptorParseException("No extensions in "
          + "identity-ed25519 (which would contain the encoded "
          + "master-key-ed25519): " + identityEd25519[39]);
    } else {
      int extensionStart = 40;
      for (int i = 0; i < (int) identityEd25519[39]; i++) {
        if (identityEd25519.length < extensionStart + 4) {
          throw new DescriptorParseException("Invalid extension with id "
              + i + " in identity-ed25519.");
        }
        int extensionLength = identityEd25519[extensionStart];
        extensionLength <<= 8;
        extensionLength += identityEd25519[extensionStart + 1];
        int extensionType = identityEd25519[extensionStart + 2];
        if (extensionLength == 32 && extensionType == 4) {
          if (identityEd25519.length < extensionStart + 4 + 32) {
            throw new DescriptorParseException("Invalid extension with "
                + "id " + i + " in identity-ed25519.");
          }
          byte[] masterKeyEd25519 = new byte[32];
          System.arraycopy(identityEd25519, extensionStart + 4,
              masterKeyEd25519, 0, masterKeyEd25519.length);
          String masterKeyEd25519Base64
              = Base64.encodeBase64String(masterKeyEd25519).replaceAll("=", "");
          return masterKeyEd25519Base64.replaceAll("=", "");
        }
        extensionStart += 4 + extensionLength;
      }
    }
    throw new DescriptorParseException("Unable to locate "
        + "master-key-ed25519 in identity-ed25519.");
  }

  private static Map<String, SortedMap<String, SortedSet<Long>>>
      parsedProtocolVersions = new HashMap<>();

  protected static SortedMap<String, SortedSet<Long>> parseProtocolVersions(
      String line, String lineNoOpt, String[] partsNoOpt)
      throws DescriptorParseException {
    if (!parsedProtocolVersions.containsKey(lineNoOpt)) {
      SortedMap<String, SortedSet<Long>> parsed = new TreeMap<>();
      boolean invalid = false;
      try {
        for (int i = 1; i < partsNoOpt.length; i++) {
          String[] part = partsNoOpt[i].split("=");
          SortedSet<Long> versions = new TreeSet<>();
          for (String val : part[1].split(",")) {
            if (val.contains("-")) {
              String[] fromTo = val.split("-");
              long from = Long.parseLong(fromTo[0]);
              long to = Long.parseLong(fromTo[1]);
              if (from > to || to >= 0x1_0000_0000L) {
                invalid = true;
              } else {
                for (long j = from;
                    j <= to; j++) {
                  versions.add(j);
                }
              }
            } else {
              versions.add(Long.parseLong(val));
            }
          }
          parsed.put(part[0], Collections.unmodifiableSortedSet(versions));
        }
      } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
        throw new DescriptorParseException("Invalid line '" + line + "'.", e);
      }
      if (invalid) {
        throw new DescriptorParseException("Invalid line '" + line + "'.");
      }
      parsedProtocolVersions.put(lineNoOpt,
          Collections.unmodifiableSortedMap(parsed));
    }
    return parsedProtocolVersions.get(lineNoOpt);
  }
}

