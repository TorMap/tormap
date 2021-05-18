package com.torusage.database.entity

import javax.persistence.*

@Entity
class RelaySummary (
    @Id
    var f: String,
    var n: String,

    @ElementCollection(fetch = FetchType.EAGER)
    var a: List<String>,
    var r: Boolean,
)
