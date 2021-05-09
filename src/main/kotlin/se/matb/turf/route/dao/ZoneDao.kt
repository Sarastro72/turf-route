package se.matb.turf.route.dao

import se.matb.turf.route.dao.model.ZoneInfo

interface ZoneDao {
    fun lookupZone(id: Int): ZoneInfo?
    fun lookupZone(name: String): ZoneInfo?
    fun lookupZonesInArea(swLat: Double, swLong: Double, neLat: Double, neLong: Double): List<ZoneInfo>
    fun storeZone(route: ZoneInfo)
    fun getAllZones(): Collection<ZoneInfo>
    fun countZones(): Int
}
