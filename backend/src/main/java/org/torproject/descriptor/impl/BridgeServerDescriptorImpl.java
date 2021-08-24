/* Copyright 2015--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.BridgeServerDescriptor;
import org.torproject.descriptor.DescriptorParseException;

import java.io.File;

public class BridgeServerDescriptorImpl extends ServerDescriptorImpl
    implements BridgeServerDescriptor {

  private static final long serialVersionUID = -9158883686763377765L;

  protected BridgeServerDescriptorImpl(byte[] rawDescriptorBytes,
      int[] offsetAndLength, File descriptorFile)
      throws DescriptorParseException {
    super(rawDescriptorBytes, offsetAndLength, descriptorFile);
  }
}

