/* Copyright 2021 SR2 Communications Limited
 * See LICENSE for licensing information */

package org.torproject.descriptor;

import java.util.Optional;

public interface BridgestrapTestResult {

  boolean isReachable();

  Optional<String> hashedFingerprint();

}
