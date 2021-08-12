package org.tormap.database.repository;

import org.springframework.data.jpa.repository.Query
import org.tormap.database.entity.IpLookupAs

interface IpLookupAsRepositoryImpl : IpLookupAsRepository {
    @Query("SELECT i FROM IpLookupAs i WHERE i.ipRange.ipFrom <= :ipv4 AND i.ipRange.ipTo >= :ipv4")
    fun findUsingIPv4(ipv4: Long): IpLookupAs?
}
