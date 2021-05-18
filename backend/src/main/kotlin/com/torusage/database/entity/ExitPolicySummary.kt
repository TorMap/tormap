package com.torusage.database.entity

import javax.persistence.*

@Entity(name = "exit_policy_summary")
class ExitPolicySummary(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    var id: Long,

    @ElementCollection
    var reject: List<String>?,

    @ElementCollection
    var accept: List<String>?,
)
