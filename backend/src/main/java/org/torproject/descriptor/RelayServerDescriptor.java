/* Copyright 2015--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor;

/**
 * Contains a relay server descriptor.
 *
 * <p>Relay server descriptors share many contents with sanitized bridge
 * server descriptors ({@link BridgeServerDescriptor}), which is why they
 * share a common superinterface ({@link ServerDescriptor}).  The main
 * purpose of having two subinterfaces is being able to distinguish
 * descriptor types more easily.</p>
 *
 * @since 1.1.0
 */
public interface RelayServerDescriptor extends ServerDescriptor {

}

