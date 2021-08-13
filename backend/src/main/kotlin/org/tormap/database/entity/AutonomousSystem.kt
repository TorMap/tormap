package org.tormap.database.entity

import javax.persistence.EmbeddedId
import javax.persistence.Entity

@Entity
class AutonomousSystem(
    @EmbeddedId
    var ipRange: IpRangeId,
    var cidr: String,
    var autonomous_system_number: String,
    var autonomous_system_name: String,
)
