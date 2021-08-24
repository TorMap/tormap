/* Copyright 2012--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor.impl;

import org.torproject.descriptor.BandwidthHistory;
import org.torproject.descriptor.DescriptorParseException;

import java.util.SortedMap;
import java.util.TreeMap;

public class BandwidthHistoryImpl implements BandwidthHistory {

  private static final long serialVersionUID = -5266052169817153234L;

  protected BandwidthHistoryImpl(String line,
      String[] partsNoOpt) throws DescriptorParseException {
    boolean isValid = false;
    this.line = line;
    if (partsNoOpt.length >= 5) {
      try {
        this.historyEndMillis = ParseHelper.parseTimestampAtIndex(line,
            partsNoOpt, 1, 2);
        if (partsNoOpt[3].startsWith("(")
            && partsNoOpt[4].startsWith("s)")) {
          this.intervalLength = Long.parseLong(partsNoOpt[3]
              .substring(1));
          if (this.intervalLength <= 0L) {
            throw new DescriptorParseException("Only positive interval "
                + "lengths are allowed in line '" + line + "'.");
          }
          String[] values = null;
          if (partsNoOpt.length == 5
              && partsNoOpt[4].equals("s)")) {
            /* There are no bandwidth values to parse. */
            isValid = true;
          } else if (partsNoOpt.length >= 6) {
            /* There are bandwidth values to parse. */
            values = partsNoOpt[5].split(",", -1);
          } else if (partsNoOpt[4].length() > 2) {
            /* There are bandwidth values to parse, but there is no space
             * between "s)" and "0,0,0,0".  Very old Tor versions around
             * Tor 0.0.8 wrote such history lines, and even though
             * dir-spec.txt implies a space here, the old format isn't
             * totally broken.  Let's pretend there's a space. */
            values = partsNoOpt[4].substring(2).split(",", -1);
          }
          if (values != null) {
            this.bandwidthValues = new long[values.length];
            for (int i = values.length - 1; i >= 0; i--) {
              long bandwidthValue = Long.parseLong(values[i]);
              if (bandwidthValue < 0L) {
                throw new DescriptorParseException("Negative bandwidth "
                    + "values are not allowed in line '" + line + "'.");
              }
              this.bandwidthValues[i] = bandwidthValue;
            }
            isValid = true;
          }
        }
      } catch (NumberFormatException e) {
        /* Handle below. */
      }
    }
    if (!isValid) {
      throw new DescriptorParseException("Invalid bandwidth-history line "
          + "'" + line + "'.");
    }
  }

  private String line;

  @Override
  public String getLine() {
    return this.line;
  }

  private long historyEndMillis;

  @Override
  public long getHistoryEndMillis() {
    return this.historyEndMillis;
  }

  private long intervalLength;

  @Override
  public long getIntervalLength() {
    return this.intervalLength;
  }

  private long[] bandwidthValues;

  @Override
  public SortedMap<Long, Long> getBandwidthValues() {
    SortedMap<Long, Long> result = new TreeMap<>();
    if (this.bandwidthValues != null) {
      long endMillis = this.historyEndMillis;
      for (int i = this.bandwidthValues.length - 1; i >= 0; i--) {
        result.put(endMillis, bandwidthValues[i]);
        endMillis -= this.intervalLength * 1000L;
      }
    }
    return result;
  }
}

