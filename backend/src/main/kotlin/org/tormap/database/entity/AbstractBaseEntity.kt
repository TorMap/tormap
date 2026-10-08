package org.tormap.database.entity

import org.hibernate.Hibernate
import java.io.Serializable
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.SequenceGenerator

@MappedSuperclass
abstract class AbstractBaseEntity<T: Serializable>  {

    // Explicit generator: all entities share the DB sequence created in V1__Create_tables.sql with an increment of 1.
    // Without it, Hibernate 6 would expect one sequence per entity with an increment of 50.
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @SequenceGenerator(name = "hibernate_sequence", sequenceName = "hibernate_sequence", allocationSize = 1)
    var id: T? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
        other as AbstractBaseEntity<*>

        return  this.id != null && this.id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Entity of type ${this.javaClass.simpleName} with id: $id"
}
