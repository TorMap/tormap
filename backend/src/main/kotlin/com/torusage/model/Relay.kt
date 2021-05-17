package com.torusage.model

import javax.persistence.*

@Entity
class Relay(
    //identification
    @Column(length = 19)
    var nickname: String,

    @Id @Column(length = 40)
    var fingerprint: String,

    //networking
    @ElementCollection
    var or_addresses: List<String>,

    @ElementCollection
    var exit_addresses: List<String>?,

    @ElementCollection
    var verified_host_names: List<String>?,

    @ElementCollection
    var exit_policy: List<String>?,

    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "exit_policy_summary_id", referencedColumnName = "id")
    var exit_policy_summary: ExitPolicySummary?,

    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "exit_policy_v6_summary_id", referencedColumnName = "id")
    var exit_policy_v6_summary: ExitPolicySummary?,

    @ElementCollection
    var effective_family: List<String>?,

    @ElementCollection
    var alleged_family: List<String>?,

    @ElementCollection
    var indirect_family: List<String>?,

    @ElementCollection
    var unverified_host_names: List<String>?,

    @ElementCollection
    var unreachable_or_addresses: List<String>?,
    var dir_address: String?,
    var last_changed_address_or_port: String,
    var `as`: String?,
    var as_name: String?,
    var consensus_weight: Int,

    //information
    @ElementCollection
    var flags: List<String>?,
    var first_seen: String,
    var running: Boolean,
    var last_seen: String,
    var last_restarted: String?,
    var bandwidth_rate: Int?,
    var bandwidth_burst: Int?,
    var observed_bandwidth: Int?,
    var advertised_bandwidth: Int?,
    var platform: String?,
    var version: String?,
    var version_status: String?,
    var recommended_version: Boolean?,
    var measured: Boolean?,
    var contact: String?,
    var consensus_weight_fraction: Double?,
    var guard_probability: Double?,
    var middle_probability: Double?,
    var exit_probability: Double?,
    var hibernating: Boolean?,

    //location
    @Column(length = 2)
    var country: String?,
    var country_name: String?,
    var region_name: String?,
    var city_name: String?,
    var latitude: Double?,
    var longitude: Double?,
)
