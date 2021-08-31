package org.tormap.database.entity

import org.hibernate.Hibernate
import java.io.Serializable
import javax.persistence.Embeddable

@Embeddable
class IpRangeId(
    var ipFrom: Long,
    var ipTo: Long,
) : Serializable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as IpRangeId

        if (ipFrom != other.ipFrom) return false
        if (ipTo != other.ipTo) return false
        return true
    }

    override fun hashCode(): Int {
        var result = ipFrom.hashCode()
        result = 31 * result + ipTo.hashCode()
        return result
    }
}
