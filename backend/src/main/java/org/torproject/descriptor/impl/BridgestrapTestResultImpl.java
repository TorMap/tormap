/* Copyright 2021 SR2 Communications Limited
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.BridgestrapTestResult;

import java.util.Optional;

public class BridgestrapTestResultImpl implements BridgestrapTestResult {

  private final boolean isReachable;

  private final String hashedFingerprint;

  public BridgestrapTestResultImpl(boolean reachable,
                                   String hashedFingerprint) {
    this.isReachable = reachable;
    this.hashedFingerprint = hashedFingerprint;
  }

  @Override
  public boolean isReachable() {
    return this.isReachable;
  }

  @Override
  public Optional<String> hashedFingerprint() {
    return Optional.ofNullable(this.hashedFingerprint);
  }
}
