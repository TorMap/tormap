/* Copyright 2016--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.internal;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;

import java.io.*;

/**
 * These enums provide compression functionality.
 *
 * <p>{@link #findType} determines the compression type by the given extension.
 * Compression can also be zero-compression, which is currently provided by
 * the PLAIN and JSON enums.</p>
 *
 * @since 1.4.0
 */
public enum FileType {

  BZ2(BZip2CompressorInputStream.class, BZip2CompressorOutputStream.class),
  GZ(GzipCompressorInputStream.class, GzipCompressorOutputStream.class),
  JSON(BufferedInputStream.class, BufferedOutputStream.class),
  PLAIN(BufferedInputStream.class, BufferedOutputStream.class),
  XZ(XZCompressorInputStream.class, XZCompressorOutputStream.class);

  private final Class<? extends InputStream> inClass;
  private final Class<? extends OutputStream> outClass;

  FileType(Class<? extends InputStream> in, Class<? extends OutputStream> out) {
    this.inClass = in;
    this.outClass = out;
  }

  /**
   * Returns {@code valueOf} or the default enum {@link #PLAIN}, i.e.,
   * this method doesn't throw any exceptions and allways returns a valid enum.
   *
   * @since 2.1.0
   */
  public static FileType findType(String ext) {
    FileType res;
    try {
      res = FileType.valueOf(ext.toUpperCase());
      return res;
    } catch (IllegalArgumentException | NullPointerException re) {
      return PLAIN;
    }
  }

  /**
   * Return the appropriate input stream.
   *
   * @since 1.4.0
   */
  public InputStream inputStream(InputStream is) throws Exception {
    return this.inClass.getConstructor(new Class[]{InputStream.class})
        .newInstance(is);
  }

  /**
   * Return the appropriate output stream.
   *
   * @since 1.4.0
   */
  public OutputStream outputStream(OutputStream os) throws Exception {
    return this.outClass.getConstructor(new Class[]{OutputStream.class})
        .newInstance(os);
  }

  /**
   * Compresses the given bytes in memory and returns the compressed bytes.
   *
   * @since 2.2.0
   */
  public byte[] compress(byte[] bytes) throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (OutputStream os = this.outputStream(baos)) {
      os.write(bytes);
      os.flush();
    }
    return baos.toByteArray();
  }

  /**
   * Compresses the given InputStream and returns an OutputStream.
   *
   * @since 2.2.0
   */
  public OutputStream compress(OutputStream os) throws Exception {
    return this.outputStream(os);
  }

  /**
   * Decompresses the given InputStream and returns an OutputStream.
   *
   * @since 2.2.0
   */
  public InputStream decompress(InputStream is) throws Exception {
    return this.inputStream(is);
  }

  /**
   * Decompresses the given bytes in memory and returns the decompressed bytes.
   *
   * @since 2.2.0
   */
  public byte[] decompress(byte[] bytes) throws Exception {
    if (0 == bytes.length) {
      return bytes;
    }
    try (InputStream is
        = this.inputStream(new ByteArrayInputStream(bytes));
        ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      int readByte = is.read();
      while (readByte > 0) {
        baos.write(readByte);
        readByte = is.read();
      }
      baos.flush();
      return baos.toByteArray();
    }
  }

}

