/* Copyright 2015--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor;

/**
 * Contains a sanitized bridge server descriptor.
 *
 * <p>Sanitized bridge server descriptors share many contents with relay
 * server descriptors ({@link RelayServerDescriptor}), which is why they
 * share a common superinterface ({@link ServerDescriptor}).  The main
 * purpose of having two subinterfaces is being able to distinguish
 * descriptor types more easily.</p>
 *
 * <p>Details about sanitizing bridge server descriptors can be found
 * <a href="https://collector.torproject.org/#type-bridge-server-descriptor">here</a>.
 * </p>
 *
 * @since 1.1.0
 */
public interface BridgeServerDescriptor extends ServerDescriptor {

}

