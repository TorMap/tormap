package org.tormap.database.entity

import javax.persistence.EmbeddedId
import javax.persistence.Entity

@Entity
class AutonomousSystem(
    @EmbeddedId
    var ipRange: IpRangeId,
    var cidr: String,
    var autonomousSystemNumber: String,
    var autonomousSystemName: String,
)
