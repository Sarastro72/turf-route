package se.matb.turf.route.manager

import se.matb.turf.route.dao.ZoneDao
import se.matb.turf.route.dao.model.ZoneInfo

class ZoneManager(val zoneDao: ZoneDao) {
    fun getZoneByName(name: String): ZoneInfo? =
        zoneDao.lookupZone(name)

}
