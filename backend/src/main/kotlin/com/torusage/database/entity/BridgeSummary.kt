package com.torusage.database.entity

import javax.persistence.*

@Entity
class BridgeSummary (
    @Id
    var h: String,
    var n: String,
    var r: Boolean,
)
