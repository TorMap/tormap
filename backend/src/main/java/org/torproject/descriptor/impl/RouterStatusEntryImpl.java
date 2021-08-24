/* Copyright 2012--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.RouterStatusEntry;

public class RouterStatusEntryImpl implements RouterStatusEntry {

  private static final long serialVersionUID = 4362115843485982121L;

  protected RouterStatusEntryImpl(String fingerprint, String nickname,
      boolean isLive, boolean isVerified) {
    this.fingerprint = fingerprint;
    this.nickname = nickname;
    this.isLive = isLive;
    this.isVerified = isVerified;
  }

  private String nickname;

  @Override
  public String getNickname() {
    return this.nickname;
  }

  private String fingerprint;

  @Override
  public String getFingerprint() {
    return this.fingerprint;
  }

  private boolean isLive;

  @Override
  public boolean isLive() {
    return this.isLive;
  }

  private boolean isVerified;

  @Override
  public boolean isVerified() {
    return this.isVerified;
  }
}

