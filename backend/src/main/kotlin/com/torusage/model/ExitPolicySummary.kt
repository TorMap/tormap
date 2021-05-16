package com.torusage.model

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

    @OneToOne(mappedBy = "exit_policy_summary")
    var relay: Relay
)
