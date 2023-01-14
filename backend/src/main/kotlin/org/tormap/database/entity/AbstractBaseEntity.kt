package org.tormap.database.entity

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.hibernate.Hibernate
import java.io.Serializable

@MappedSuperclass
abstract class AbstractBaseEntity<T : Serializable> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    var id: T? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as AbstractBaseEntity<*>

        return this.id != null && this.id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Entity of type ${this.javaClass.simpleName} with id: $id"
}
