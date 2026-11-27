package org.torproject.descriptor.impl;

import org.junit.jupiter.api.Test;
import org.torproject.descriptor.Descriptor;
import org.torproject.descriptor.UnparseableDescriptor;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Local patches to metrics-lib 2.28.0 for identity-ed25519 crypto blocks, found by DescriptorParserFuzzer. Upstream
 * failed with an IndexOutOfBoundsException, which stopped DescriptorReaderImpl from reading all remaining descriptor
 * files. Such descriptors must just be unparseable.
 */
class IdentityEd25519CryptoBlockTest {

  private static List<Descriptor> parse(byte[] certificate) {
    return parse(Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(certificate));
  }

  private static List<Descriptor> parse(String cryptoBlockBody) {
    String descriptor = "@type server-descriptor 1.0\n"
        + "router relay 94.134.192.243 9001 0 0\n"
        + "identity-ed25519\n"
        + "-----BEGIN ED25519 CERT-----\n"
        + cryptoBlockBody + "\n"
        + "-----END ED25519 CERT-----\n"
        + "published 2026-11-17 15:59:41\n";
    List<Descriptor> descriptors = new ArrayList<>();
    new DescriptorParserImpl()
        .parseDescriptors(descriptor.getBytes(StandardCharsets.US_ASCII), null, "descriptor")
        .forEach(descriptors::add);
    return descriptors;
  }

  /** Version 1, type 4, certified key type 1, two extensions, the first of {@code extensionLengthHigh << 8} bytes */
  private static byte[] certificate(int extensionLengthHigh) {
    byte[] certificate = new byte[140];
    certificate[0] = 0x01;
    certificate[1] = 0x04;
    certificate[6] = 0x01;
    certificate[39] = 0x02;
    certificate[40] = (byte) extensionLengthHigh;
    certificate[42] = 0x04;
    return certificate;
  }

  @Test
  void extensionLengthAbove0x7FFFIsUnparseableInsteadOfFailing() {
    List<Descriptor> descriptors = parse(certificate(0xB3));
    assertEquals(1, descriptors.size());
    UnparseableDescriptor descriptor = assertInstanceOf(UnparseableDescriptor.class, descriptors.get(0));
    // A regular parse error, not the wrapped ArrayIndexOutOfBoundsException
    assertNull(descriptor.getDescriptorParseException().getCause());
    assertEquals("Invalid extension with id 1 in identity-ed25519.",
        descriptor.getDescriptorParseException().getMessage());
  }

  @Test
  void blockWithManyShortLinesIsUnparseableInsteadOfFailing() {
    List<Descriptor> descriptors = parse("AAAA\n".repeat(40).strip());
    assertEquals(1, descriptors.size());
    UnparseableDescriptor descriptor = assertInstanceOf(UnparseableDescriptor.class, descriptors.get(0));
    // A regular parse error, not the wrapped StringIndexOutOfBoundsException
    assertNull(descriptor.getDescriptorParseException().getCause());
    assertEquals("Unknown version in identity-ed25519: 0",
        descriptor.getDescriptorParseException().getMessage());
  }
}
