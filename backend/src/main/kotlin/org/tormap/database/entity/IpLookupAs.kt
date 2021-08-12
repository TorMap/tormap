package org.tormap.database.entity

import javax.persistence.EmbeddedId
import javax.persistence.Entity

@Entity
class IpLookupAs(
    @EmbeddedId
    var ipRange: IpRangeId,
    var cidr: String,
    var asn: String,
    var `as`: String,
)
