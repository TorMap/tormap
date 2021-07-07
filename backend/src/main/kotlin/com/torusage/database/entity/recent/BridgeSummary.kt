package com.torusage.database.entity.recent

import javax.persistence.Entity
import javax.persistence.Id

/**
 * This entity matches the [Bridge summary object](https://metrics.torproject.org/onionoo.html#summary_bridge)
 * of the Onionoo API and is also used to generate the DB structure.
 */
@Suppress("unused")
@Entity
class BridgeSummary (
    @Id
    var h: String,

    var n: String,

    var r: Boolean,
)
