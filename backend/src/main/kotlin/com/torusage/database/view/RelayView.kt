package com.torusage.database.view

import com.torusage.database.entity.Relay

@Suppress("unused")
class RelayView(relay: Relay) {
    val fingerprint = relay.fingerprint
    val firstSeen = relay.first_seen
    val lastSeen = relay.last_seen
    val latitude = relay.latitude
    val longitude = relay.longitude
}
