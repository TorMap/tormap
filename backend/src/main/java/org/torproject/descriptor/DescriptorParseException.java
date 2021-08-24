/* Copyright 2014--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor;

/**
 * Thrown if raw descriptor contents cannot be parsed to one or more
 * {@link Descriptor} instances, according to descriptor specifications.
 *
 * @since 1.0.0
 */
public class DescriptorParseException extends Exception {

  private static final long serialVersionUID = 100L;

  public DescriptorParseException(String message) {
    super(message);
  }

  public DescriptorParseException(String message, Exception ex) {
    super(message, ex);
  }

}

