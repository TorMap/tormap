/* Copyright 2017--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor;

import java.util.List;

/**
 * Contains a descriptor that could not be parsed.
 * Only {@link UnparseableDescriptor#getRawDescriptorBytes} and
 * {@link UnparseableDescriptor#getDescriptorFile} are supported.
 *
 * @since 1.9.0
 */
public interface UnparseableDescriptor extends Descriptor {

  /**
   * Return the first exception thrown while attempting to parse this
   * descriptor.
   *
   * @return First exception thrown.
   *
   * @since 1.9.0
   */
  DescriptorParseException getDescriptorParseException();

  /**
   * Will always throw an {@code UnsupportedOperationException}.
   *
   * @since 1.9.0
   */
  @Override
  List<String> getAnnotations();

  /**
   * Will always throw an {@code UnsupportedOperationException}.
   *
   * @since 1.9.0
   */
  @Override
  List<String> getUnrecognizedLines();

}

