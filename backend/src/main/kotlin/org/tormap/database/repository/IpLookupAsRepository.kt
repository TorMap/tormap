package org.tormap.database.repository;

import org.springframework.data.repository.CrudRepository
import org.tormap.database.entity.IpLookupAs
import org.tormap.database.entity.IpRangeId

interface IpLookupAsRepository : CrudRepository<IpLookupAs, IpRangeId> {
}
