/* Copyright 2015--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor;

/**
 * Contains a relay extra-info descriptor.
 *
 * <p>Relay extra-info descriptors share many contents with sanitized
 * bridge extra-info descriptors ({@link BridgeExtraInfoDescriptor}),
 * which is why they share a common superinterface
 * ({@link ExtraInfoDescriptor}).  The main purpose of having two
 * subinterfaces is being able to distinguish descriptor types more
 * easily.</p>
 *
 * @since 1.1.0
 */
public interface RelayExtraInfoDescriptor extends ExtraInfoDescriptor {

}

