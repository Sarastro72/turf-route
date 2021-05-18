package se.matb.turf.route.manager

import io.ktor.features.NotFoundException
import se.matb.turf.logging.Logging
import se.matb.turf.route.dao.RouteDao
import se.matb.turf.route.dao.ZoneDao
import se.matb.turf.route.dto.RouteToDto
import se.matb.turf.route.dto.ZoneDto
import java.lang.Math.PI
import java.util.PriorityQueue
import kotlin.math.cos
import kotlin.math.sqrt

const val MAX_ZONES_WITH_ROUTES = 2000
const val LONGEST_PATH = 20

class QueryManager(
    private val zoneDao: ZoneDao,
    private val routeDao: RouteDao
) {
    companion object : Logging() {
        private const val KMFACTOR = 40000 / 360
        private const val RADFACTOR = PI / 180
        fun distance(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
            val latKM = (lat2 - lat1) * KMFACTOR
            val longKM = (long2 - long1) * cos(lat1 * RADFACTOR) * KMFACTOR
            return sqrt(latKM * latKM + longKM * longKM)
        }
    }

    fun routeDistance(zoneId1: Int, zoneId2: Int): Double {
        val zone1 = zoneDao.lookupZone(zoneId1) ?: throw NotFoundException("No such zone $zoneId1")
        val zone2 = zoneDao.lookupZone(zoneId2) ?: throw NotFoundException("No such zone $zoneId2")
        return distance(zone1.lat, zone1.long, zone2.lat, zone2.long)
    }

    fun routeSpeed(zoneId1: Int, zoneId2: Int, time: Int): Double {
        val distance = routeDistance(zoneId1, zoneId2)
        return distance * 3600 / time
    }

    fun fetchZoneById(id: Int): ZoneDto? {
        return zoneDao.lookupZone(id)
            ?.let { zone -> decorateZoneWithRoutes(ZoneDto.from(zone)) }
    }

    fun fetchZoneByName(name: String): ZoneDto? {
        return zoneDao.lookupZone(name)
            ?.let { zone -> decorateZoneWithRoutes(ZoneDto.from(zone)) }
    }

    fun fetchZonesByArea(swLat: Double, swLong: Double, neLat: Double, neLong: Double): List<ZoneDto> {
        val zones = zoneDao.lookupZonesInArea(swLat, swLong, neLat, neLong).map(ZoneDto::from)
        if (zones.size <= MAX_ZONES_WITH_ROUTES) {
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

    fun routeTo(
        from: Int,
        to: Int?,
        goal: (RouteState?) -> Boolean = { state -> to == state?.zoneId },
        order: (RouteState, RouteState) -> Int = { a, b -> a.time - b.time }
    ): RouteState? {
        val context = RouteContext(goal, routeDao)
        val initialState = RouteState(from, 0, 0, order)
        val result = context.handle(initialState)
        if (result != null)
            LOG.info { result.path.map { zoneDao.lookupZone(it)?.name } + zoneDao.lookupZone(result.zoneId)?.name }
        return result
    }

    class RouteContext(private val goal: (RouteState?) -> Boolean, private val routeDao: RouteDao) {
        companion object : Logging()

        val queue: PriorityQueue<RouteState> = PriorityQueue()

        fun handle(state: RouteState): RouteState? {
            var thisState = state
            var iter = 0
            while (!goal(thisState) && thisState.length <= LONGEST_PATH) {
                routeDao.lookupRoutesByFromZone(thisState.zoneId).forEach { route ->
                    if (!thisState.path.contains(route.toZone)) {
                        queue.offer(
                            RouteState(
                                route.toZone,
                                thisState.length + 1,
                                thisState.time + route.avg(),
                                thisState.comparator,
                                thisState.path + thisState.zoneId
                            )
                        )
                        iter++
                    }
                }
                thisState = queue.poll()
            }
            return if (goal(thisState)) {
                LOG.info { "Found a route of length ${thisState.length} in $iter iterations" }
                thisState
            } else {
                LOG.info { "Found a route in $iter iterations" }
                null
            }
        }
    }

    data class RouteState(
        val zoneId: Int,
        val length: Int,
        val time: Int,
        val comparator: Comparator<RouteState>,
        val path: List<Int> = emptyList()
    ) : Comparable<RouteState> {
        override fun compareTo(other: RouteState): Int {
            return comparator.compare(this, other)
        }
    }
}
