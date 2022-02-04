package org.tormap.database.entity

import org.springframework.data.util.ProxyUtils
import java.io.Serializable
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.MappedSuperclass

@MappedSuperclass
abstract class AbstractBaseEntity<T: Serializable>  {

    @Id
    @GeneratedValue
    var id: T? = null

    override fun equals(other: Any?): Boolean {
        other ?: return false

        if (this === other) return true

        if (javaClass != ProxyUtils.getUserClass(other)) return false

        other as AbstractBaseEntity<*>

        return  this.id != null && this.id == other.id
    }

    override fun hashCode() = 31

    override fun toString() = "Entity of type ${this.javaClass.simpleName} with id: $id"
}