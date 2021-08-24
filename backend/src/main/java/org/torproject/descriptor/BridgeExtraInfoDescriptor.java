/* Copyright 2015--2020 The Tor Project
 * See LICENSE for licensing information */

package org.torproject.descriptor;

/**
 * Contains a sanitized bridge extra-info descriptor.
 *
 * <p>Sanitized bridge extra-info descriptors share many contents with
 * relay extra-info descriptors ({@link RelayExtraInfoDescriptor}), which
 * is why they share a common
 * superinterface ({@link ExtraInfoDescriptor}).  The main purpose of
 * having two subinterfaces is being able to distinguish descriptor types
 * more easily.</p>
 *
 * <p>Details about sanitizing bridge extra-info descriptors can be found
 * <a href="https://collector.torproject.org/#type-bridge-extra-info">here</a>.
 * </p>
 *
 * @since 1.1.0
 */
public interface BridgeExtraInfoDescriptor extends ExtraInfoDescriptor {

}

