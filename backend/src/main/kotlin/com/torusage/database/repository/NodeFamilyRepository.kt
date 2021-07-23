package com.torusage.database.repository;

import com.torusage.database.entity.NodeFamily
import org.springframework.data.repository.CrudRepository

interface NodeFamilyRepository : CrudRepository<NodeFamily, Long> {
    fun existsByMonthAndFingerprints(month: String, fingerprints: String): Boolean
}
