package se.matb.turf.route.dao

import se.matb.turf.route.manager.ZoneInfo

interface ZoneDao {
    fun lookupZone(id: Int): ZoneInfo?
    fun storeZone(route: ZoneInfo)
    fun getAllZones(): Collection<ZoneInfo>
    fun countZones(): Int
}
