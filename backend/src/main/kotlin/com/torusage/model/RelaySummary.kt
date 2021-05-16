package com.torusage.model

import javax.persistence.*

@Entity
class RelaySummary (
    var n: String,
    var f: String,


    @ElementCollection(fetch = FetchType.EAGER)
    var a: List<String>,
    var r: Boolean,
    @Id @GeneratedValue var id: Long? = null
)