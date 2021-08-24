/* Copyright 2015--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.BridgeExtraInfoDescriptor;
import org.torproject.descriptor.DescriptorParseException;

import java.io.File;

public class BridgeExtraInfoDescriptorImpl
    extends ExtraInfoDescriptorImpl implements BridgeExtraInfoDescriptor {

  private static final long serialVersionUID = -1458417007735415506L;

  protected BridgeExtraInfoDescriptorImpl(byte[] descriptorBytes,
      int[] offsetAndLimit, File descriptorFile)
      throws DescriptorParseException {
    super(descriptorBytes, offsetAndLimit, descriptorFile);
  }
}

