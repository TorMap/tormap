package org.torproject.descriptor.impl;

import org.junit.jupiter.api.Test;
import org.torproject.descriptor.Descriptor;
import org.torproject.descriptor.ServerDescriptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Local patch to metrics-lib 2.28.0: the CollecTor archive
 * server-descriptors-2026-04.tar.xz holds one 2 GB entry with ~320,000
 * concatenated descriptors. Reading it into one byte array exhausts the heap,
 * so large entries are split at "@type " lines.
 */
class DescriptorReaderLargeEntryTest {

  private static String descriptor(int index) {
    return "@type server-descriptor 1.0\n"
        + "router relay" + index + " 94.134.192.243 9001 0 0\n"
        + "bandwidth 51200 51200 53470\n"
        + "published 2012-01-01 04:03:19\n"
        + "fingerprint D873 3048 FC8E C910 2466 AD8F 3098 622B F1BF 71FD\n"
        + "reject *:*\n"
        + "router-signature\n"
        + "-----BEGIN SIGNATURE-----\n"
        + "o4j+kH8UQfjBwepUnr99v0ebN8RpzHJ/lqYsTojXHy9kMr1RNI9IDeSzA7PSqTuV\n"
        + "4PL8QsGtlfwthtIoZpB2srZeyN/mcpA9fa1JXUrt/UN9K/+32Cyaad7h0nHE6Xfb\n"
        + "jqpXDpnBpvk4zjmzjjKYnIsUWTnADmu0fo3xTRqXi7g=\n"
        + "-----END SIGNATURE-----\n";
  }

  private static String descriptors(int from, int to) {
    StringBuilder sb = new StringBuilder();
    for (int i = from; i < to; i++) {
      sb.append(descriptor(i));
    }
    return sb.toString();
  }

  private static List<byte[]> split(String input, int target, int max,
      long expectedDiscardedBytes) throws IOException {
    List<byte[]> chunks = new ArrayList<>();
    long discarded = DescriptorReaderImpl.splitAtTypeAnnotations(
        new ByteArrayInputStream(input.getBytes(StandardCharsets.US_ASCII)),
        target, max, chunks::add);
    assertEquals(expectedDiscardedBytes, discarded);
    return chunks;
  }

  @Test
  void splitsAtTypeAnnotationsWithoutLosingBytes() throws IOException {
    String input = descriptors(0, 100);
    int descriptorLength = descriptor(0).length();
    List<byte[]> chunks = split(input, 5 * descriptorLength, 20 * descriptorLength, 0);

    assertTrue(chunks.size() > 1);
    ByteArrayOutputStream joined = new ByteArrayOutputStream();
    for (byte[] chunk : chunks) {
      assertTrue(new String(chunk, StandardCharsets.US_ASCII).startsWith("@type server-descriptor 1.0\n"));
      assertTrue(chunk.length <= 20 * descriptorLength);
      joined.write(chunk);
    }
    assertEquals(input, joined.toString(StandardCharsets.US_ASCII));
  }

  @Test
  void chunksParseIntoAllDescriptors() throws IOException {
    List<byte[]> chunks = split(descriptors(0, 50), 1000, 100_000, 0);

    DescriptorParserImpl parser = new DescriptorParserImpl();
    List<String> nicknames = new ArrayList<>();
    for (byte[] chunk : chunks) {
      for (Descriptor descriptor : parser.parseDescriptors(chunk, null, "entry")) {
        nicknames.add(((ServerDescriptor) descriptor).getNickname());
      }
    }
    assertEquals(50, nicknames.size());
    assertEquals("relay0", nicknames.get(0));
    assertEquals("relay49", nicknames.get(49));
  }

  @Test
  void discardsOversizedPartWithoutTypeAnnotationAndResumes() throws IOException {
    String garbage = "x".repeat(5000) + "\n";
    String input = descriptor(0) + garbage + descriptors(1, 3);
    List<byte[]> chunks = split(input, 100_000, 4000, descriptor(0).length() + garbage.length());

    assertEquals(1, chunks.size());
    assertEquals(descriptors(1, 3), new String(chunks.get(0), StandardCharsets.US_ASCII));
  }
}
