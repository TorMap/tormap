package com.torusage.database.entity

import javax.persistence.*

/**
 * This entity matches the [Relay summary object](https://metrics.torproject.org/onionoo.html#summary_relay)
 * of the Onionoo API and is also used to generate the DB structure.
 */
@Entity
class RelaySummary (
    @Id
    var f: String,

    var n: String,

    @ElementCollection(fetch = FetchType.EAGER)
    var a: List<String>,

    var r: Boolean,
)
