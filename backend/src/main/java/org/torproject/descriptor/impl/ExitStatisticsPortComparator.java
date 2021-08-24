/* Copyright 2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import java.io.Serializable;
import java.util.Comparator;

public class ExitStatisticsPortComparator implements Comparator<String>,
    Serializable {

  private static final long serialVersionUID = 636628160711742180L;

  @Override
  public int compare(String arg0, String arg1) {
    int port0;
    int port1;
    try {
      port1 = Integer.parseInt(arg1);
    } catch (NumberFormatException e) {
      return -1;
    }
    try {
      port0 = Integer.parseInt(arg0);
    } catch (NumberFormatException e) {
      return 1;
    }
    return Integer.compare(port0, port1);
  }
}

