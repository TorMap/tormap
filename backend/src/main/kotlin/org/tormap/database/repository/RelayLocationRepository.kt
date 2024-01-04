@file:Suppress("FunctionName")

package org.tormap.database.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.tormap.database.entity.RelayLocation


/**
 * Repository to interact with DB
 */
interface RelayLocationRepository : JpaRepository<RelayLocation, Long>
