/* Copyright 2017--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.DescriptorParseException;
import org.torproject.descriptor.UnparseableDescriptor;

import java.io.File;
import java.util.List;

public class UnparseableDescriptorImpl extends DescriptorImpl
    implements UnparseableDescriptor {

  private static final long serialVersionUID = 7750009166142114121L;

  protected UnparseableDescriptorImpl(byte[] rawDescriptorBytes,
      int[] offsetAndLength, File descriptorFile,
      DescriptorParseException descriptorParseException) {
    super(rawDescriptorBytes, offsetAndLength, descriptorFile);
    this.descriptorParseException = descriptorParseException;
  }

  private DescriptorParseException descriptorParseException;

  public DescriptorParseException getDescriptorParseException() {
    return this.descriptorParseException;
  }

  @Override
  public List<String> getAnnotations() {
    throw new UnsupportedOperationException("This operation is not supported.");
  }

  @Override
  public List<String> getUnrecognizedLines() {
    throw new UnsupportedOperationException("This operation is not supported.");
  }
}

