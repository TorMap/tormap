package com.torusage.database.entity.recent

import javax.persistence.ElementCollection
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.Id

/**
 * This entity matches the [Relay summary object](https://metrics.torproject.org/onionoo.html#summary_relay)
 * of the Onionoo API and is also used to generate the DB structure.
 */
@Suppress("unused")
@Entity
class RelaySummary (
    @Id
    var f: String,

    var n: String,

    @ElementCollection(fetch = FetchType.EAGER)
    var a: List<String>,

    var r: Boolean,
)