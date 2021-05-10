package com.torusage.model

import javax.persistence.*

@Entity
class RelaySummary (
    var n: String,
    var f: String,
    var a: String,
    var r: Boolean,
    @Id @GeneratedValue var id: Long? = null
)