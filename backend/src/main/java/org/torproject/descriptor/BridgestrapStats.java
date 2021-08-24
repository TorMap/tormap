/* Copyright 2019--2020 The Tor Project
 * Copyright 2021 SR2 Communications Limited
 * See LICENSE for licensing information */

package org.torproject.descriptor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BridgestrapStats extends Descriptor {

  LocalDateTime bridgestrapStatsEnd();

  Duration bridgestrapStatsIntervalLength();

  int bridgestrapCachedRequests();

  Optional<List<BridgestrapTestResult>> bridgestrapTests();

}
