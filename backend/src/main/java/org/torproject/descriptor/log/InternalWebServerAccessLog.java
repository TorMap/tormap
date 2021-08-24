/* Copyright 2018--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.log;

/**
 * This interface provides methods for internal use only.
 *
 * @since 2.2.0
 */
public interface InternalWebServerAccessLog extends InternalLogDescriptor {

  /** The log's name should include this string. */
  String MARKER = "access.log";

}

