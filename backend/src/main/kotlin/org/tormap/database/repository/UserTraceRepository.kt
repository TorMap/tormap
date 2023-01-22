package org.tormap.database.repository

import org.springframework.data.repository.ListCrudRepository
import org.tormap.database.entity.UserTrace

interface UserTraceRepository : ListCrudRepository<UserTrace, Long>
