/* Copyright 2015--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.DescriptorParseException;
import org.torproject.descriptor.RelayServerDescriptor;

import java.io.File;

public class RelayServerDescriptorImpl extends ServerDescriptorImpl
    implements RelayServerDescriptor {

  private static final long serialVersionUID = -8871465152198614055L;

  protected RelayServerDescriptorImpl(byte[] descriptorBytes,
      int[] offsetAndLength, File descriptorFile)
      throws DescriptorParseException {
    super(descriptorBytes, offsetAndLength, descriptorFile);
    this.calculateDigestSha1Hex(Key.ROUTER.keyword + SP,
        NL + Key.ROUTER_SIGNATURE.keyword + NL);
    this.calculateDigestSha256Base64(Key.ROUTER.keyword + SP,
        NL + "-----END SIGNATURE-----" + NL);
  }
}

