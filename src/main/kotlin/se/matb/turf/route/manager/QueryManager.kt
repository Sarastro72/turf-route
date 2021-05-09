package se.matb.turf.route.manager

import io.ktor.features.NotFoundException
import se.matb.turf.route.dao.RouteDao
import se.matb.turf.route.dao.ZoneDao
import se.matb.turf.route.dto.RouteToDto
import se.matb.turf.route.dto.ZoneDto

class QueryManager(
    private val zoneDao: ZoneDao,
    private val routeDao: RouteDao
) {
    fun fetchZoneByName(name: String): ZoneDto? {
        return zoneDao.lookupZone(name)
            ?.let { zone -> decorateZoneWithRoutes(ZoneDto.from(zone)) }
    }

    fun fetchZonesByArea(swLat: Double, swLong: Double, neLat: Double, neLong: Double): List<ZoneDto> {
        val zones = zoneDao.lookupZonesInArea(swLat, swLong, neLat, neLong).map(ZoneDto::from)
        if (zones.size < 400) {
            return zones.map(this::decorateZoneWithRoutes)
        } else {
            return zones
        }
    }

    private fun decorateZoneWithRoutes(zone: ZoneDto): ZoneDto {
        val originalRoutes = routeDao.lookupRoutesByFromZone(zone.id)
        val totalExits = originalRoutes.sumOf { it.times.size }
        val routes = originalRoutes.map { route ->
            val toZone = zoneDao.lookupZone(route.toZone)
                ?: throw NotFoundException("Found no zone of id ${route.toZone}")
            RouteToDto.from(route, toZone, totalExits, originalRoutes.size)
        }
        return zone.copy(routes = routes)
    }
}
