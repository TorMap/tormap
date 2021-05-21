package com.torusage.database.entity

import javax.persistence.*

/**
 * This entity matches the [Bridge summary object](https://metrics.torproject.org/onionoo.html#summary_bridge)
 * of the Onionoo API and is also used to generate the DB structure.
 */
@Entity
class BridgeSummary (
    @Id
    var h: String,

    var n: String,

    var r: Boolean,
)
