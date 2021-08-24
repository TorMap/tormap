/* Copyright 2017--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.log;

import org.torproject.descriptor.DescriptorParseException;
import org.torproject.descriptor.LogDescriptor;

/**
 * This interface provides methods for internal use only.
 *
 * @since 2.2.0
 */
public interface InternalLogDescriptor extends LogDescriptor {

  /** Logfile name parts separator. */
  String SEP = "_";

  /**
   * Validate log lines.
   *
   * @since 2.2.0
   */
  void validate() throws DescriptorParseException;

  /**
   * Set the {@code Validator} that will perform the validation on log
   * lines.
   *
   * <p>Usually set by the implementing class.</p>
   *
   * @since 2.2.0
   */
  void setValidator(Validator validator);

  /**
   * Set the descriptor's bytes.
   *
   * @since 2.2.0
   */
  void setRawDescriptorBytes(byte[] bytes);

  /** Return the descriptor's preferred compression. */
  String getCompressionType();

  /**
   * Provides a single function for validating a single log line.
   *
   * @since 2.2.0
   */
  interface Validator {

    /**
     * Verifies a log line.
     *
     * @since 2.2.0
     */
    boolean validate(String line);

  }

}

