package org.tormap.database.repository

import org.springframework.data.repository.ListCrudRepository
import org.springframework.stereotype.Repository
import org.tormap.database.entity.UserTrace

@Repository
interface UserTraceRepository : ListCrudRepository<UserTrace, Long>
