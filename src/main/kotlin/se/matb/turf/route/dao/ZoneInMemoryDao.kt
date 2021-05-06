package se.matb.turf.route.dao

import se.matb.turf.route.manager.ZoneInfo

class ZoneInMemoryDao : ZoneDao {
    private val zones = HashMap<Int, ZoneInfo>()

    override fun lookupZone(id: Int): ZoneInfo? =
        zones[id]

    override fun storeZone(zone: ZoneInfo) {
        zones[zone.id] = zone
    }

    override fun getAllZones(): Collection<ZoneInfo> =
        zones.values

    override fun countZones(): Int =
        zones.size
}
