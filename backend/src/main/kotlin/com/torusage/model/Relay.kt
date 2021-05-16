package com.torusage.model

import javax.persistence.*

@Entity
class Relay(
    var nickname: String,
    @Id
    var fingerprint: String,

    @ElementCollection
    var or_addresses: List<String>,

    @ElementCollection
    var exit_addresses: List<String>?,
    var dir_address: String?,
    var last_seen: String,
    var last_changed_address_or_port: String,
    var first_seen: String,
    var running: Boolean,

    @ElementCollection
    var flags: List<String>?,
    var country: String?,
    var country_name: String?,
    var region_name: String?,
    var city_name: String?,
    var latitude: Double?,
    var longitude: Double?,
    var `as`: String?,
    var as_name: String?,
    var consensus_weight: Int,

    @ElementCollection
    var verified_host_names: List<String>?,
    var last_restarted: String?,
    var bandwidth_rate: Int?,
    var bandwidth_burst: Int?,
    var observed_bandwidth: Int?,
    var advertised_bandwidth: Int?,

    @ElementCollection
    var exit_policy: List<String>?,

    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "exit_policy_summary_id", referencedColumnName = "id")
    var exit_policy_summary: ExitPolicySummary?,

    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "exit_policy_v6_summary_id", referencedColumnName = "id")
    var exit_policy_v6_summary: ExitPolicySummary?,
    var platform: String?,
    var version: String?,
    var version_status: String?,

    @ElementCollection
    var effective_family: List<String>?,
    var recommended_version: Boolean?,
    var measured: Boolean?,
    var contact: String?,

    @ElementCollection
    var alleged_family: List<String>?,

    @ElementCollection
    var indirect_family: List<String>?,
    var consensus_weight_fraction: Double?,
    var guard_probability: Double?,
    var middle_probability: Double?,
    var exit_probability: Double?,
    var hibernating: Boolean?,

    @ElementCollection
    var unverified_host_names: List<String>?,

    @ElementCollection
    var unreachable_or_addresses: List<String>?,
)
