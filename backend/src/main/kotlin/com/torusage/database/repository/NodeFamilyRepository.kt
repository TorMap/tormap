package com.torusage.database.repository;

import com.torusage.database.entity.NodeFamily
import org.springframework.data.repository.CrudRepository

interface NodeFamilyRepository : CrudRepository<NodeFamily, Long> {
    fun existsByMonthAndFingerprintsIsLike(month: String, fingerprintsLike: String): Boolean
}
