/* Copyright 2011--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor;

import java.io.File;
import java.io.Serializable;
import java.util.List;

/**
 * Superinterface for any descriptor with access to generic information
 * about the descriptor.
 *
 * @since 1.0.0
 */
public interface Descriptor extends Serializable {

  /**
   * Return the raw descriptor bytes.
   *
   * <p>This method creates a new copy of raw descriptor bytes from a
   * potentially larger byte array containing multiple descriptors.
   * Applications that only want to learn about raw descriptor length in bytes
   * should instead use {@link #getRawDescriptorLength()}.</p>
   *
   * @since 1.0.0
   */
  byte[] getRawDescriptorBytes();

  /**
   * Return the raw descriptor length in bytes.
   *
   * <p>Returns the exact same result as {@code getRawDescriptorBytes().length},
   * but much more efficiently.</p>
   *
   * @since 1.9.0
   */
  int getRawDescriptorLength();

  /**
   * Return the (possibly empty) list of annotations in the format
   * {@code "@key( value)*"}.
   *
   * <p>Some implementations might not support this operation and will throw an
   * {@code UnsupportedOperationException}, e.g.,
   * {@link UnparseableDescriptor}.</p>
   *
   * @since 1.0.0
   */
  List<String> getAnnotations();

  /**
   * Return any unrecognized lines when parsing this descriptor, or an
   * empty list if there were no unrecognized lines.
   *
   * <p>Some implementations might not support this operation and will throw an
   * {@code UnsupportedOperationException}, e.g.,
   * {@link UnparseableDescriptor}.</p>
   *
   * @since 1.0.0
   */
  List<String> getUnrecognizedLines();

  /**
   * Return the file, tarball or plain file, that contained this descriptor, or
   * {@code null} if this descriptor was not read from a file.
   *
   * @return Descriptor file that contained this descriptor.
   *
   * @since 1.9.0
   */
  File getDescriptorFile();
}

