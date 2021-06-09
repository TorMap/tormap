package com.torusage.database.entity

import javax.persistence.*

/**
 * This entity matches the [exit_policy_v6_summary object](https://metrics.torproject.org/onionoo.html#details_relay_exit_policy_summary)
 * of the Onionoo API and is also used to generate the DB structure.
 */
@Suppress("unused")
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
