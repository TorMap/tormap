/* Copyright 2015--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.DescriptorParseException;
import org.torproject.descriptor.RelayExtraInfoDescriptor;

import java.io.File;

public class RelayExtraInfoDescriptorImpl
    extends ExtraInfoDescriptorImpl implements RelayExtraInfoDescriptor {

  private static final long serialVersionUID = 2526561625458492428L;

  protected RelayExtraInfoDescriptorImpl(byte[] descriptorBytes,
      int[] offsetAndLimit, File descriptorFile)
      throws DescriptorParseException {
    super(descriptorBytes, offsetAndLimit, descriptorFile);
    this.calculateDigestSha1Hex(Key.EXTRA_INFO.keyword + SP,
        NL + Key.ROUTER_SIGNATURE.keyword + NL);
    this.calculateDigestSha256Base64(Key.EXTRA_INFO.keyword + SP,
        NL + "-----END SIGNATURE-----" + NL);
  }
}

