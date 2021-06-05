package com.torusage.database.view

import com.torusage.database.entity.Relay

class RelayView(relay: Relay) {
    val fingerprint: String = relay.fingerprint
    val `as`: String? = relay.`as`
    val first_seen: String = relay.first_seen
    val last_seen: String = relay.last_seen
    val latitude: Double? = relay.latitude
    val longitude: Double? = relay.longitude
}