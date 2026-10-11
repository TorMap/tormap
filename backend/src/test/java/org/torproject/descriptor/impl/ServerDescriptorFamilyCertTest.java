package org.torproject.descriptor.impl;

import org.junit.jupiter.api.Test;
import org.torproject.descriptor.DescriptorParseException;
import org.torproject.descriptor.ServerDescriptor;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Local patch to metrics-lib 2.28.0: relays that are members of several
 * families publish one family-cert per family, which upstream rejects.
 */
class ServerDescriptorFamilyCertTest {

  private static final String CERT_1 = "AQwAB5olAa1ZqesV/+lAKjEzzRn5AjezOQhP4TW6NahU2QaFKRneAQAgBABZydjD";

  private static final String CERT_2 = "4qgIlFDIzoC9ieJOtSkwrK+yXJPKlP8ojvgkx8cGKvhokOwA1eYDombzfwHcJ1EV";

  private static String familyCert(String body) {
    return "family-cert\n"
        + "-----BEGIN FAMILY CERT-----\n"
        + body + "\n"
        + "-----END FAMILY CERT-----\n";
  }

  private static ServerDescriptor parse(String familyCertLines) throws DescriptorParseException {
    String descriptor = "router saberrider2008 94.134.192.243 9001 0 0\n"
        + "bandwidth 51200 51200 53470\n"
        + "published 2012-01-01 04:03:19\n"
        + "fingerprint D873 3048 FC8E C910 2466 AD8F 3098 622B F1BF 71FD\n"
        + familyCertLines
        + "reject *:*\n"
        + "router-signature\n"
        + "-----BEGIN SIGNATURE-----\n"
        + "o4j+kH8UQfjBwepUnr99v0ebN8RpzHJ/lqYsTojXHy9kMr1RNI9IDeSzA7PSqTuV\n"
        + "4PL8QsGtlfwthtIoZpB2srZeyN/mcpA9fa1JXUrt/UN9K/+32Cyaad7h0nHE6Xfb\n"
        + "jqpXDpnBpvk4zjmzjjKYnIsUWTnADmu0fo3xTRqXi7g=\n"
        + "-----END SIGNATURE-----\n";
    byte[] bytes = descriptor.getBytes(StandardCharsets.US_ASCII);
    return new RelayServerDescriptorImpl(bytes, new int[] {0, bytes.length}, null);
  }

  @Test
  void singleFamilyCertIsParsed() throws DescriptorParseException {
    ServerDescriptor descriptor = parse(familyCert(CERT_1));
    assertEquals("-----BEGIN FAMILY CERT-----\n" + CERT_1 + "\n-----END FAMILY CERT-----",
        descriptor.getFamilyCert());
    assertTrue(descriptor.getUnrecognizedLines().isEmpty());
  }

  @Test
  void multipleFamilyCertsAreAcceptedAndFirstIsKept() throws DescriptorParseException {
    ServerDescriptor descriptor = parse(familyCert(CERT_1) + familyCert(CERT_2));
    assertEquals("D8733048FC8EC9102466AD8F3098622BF1BF71FD", descriptor.getFingerprint());
    assertTrue(descriptor.getFamilyCert().contains(CERT_1));
    assertTrue(descriptor.getUnrecognizedLines().isEmpty());
  }
}
