package se.matb.turf.route.dto

import se.matb.turf.route.dao.model.RouteInfo
import se.matb.turf.route.dao.model.ZoneInfo
import java.time.Instant
import kotlin.math.max
import kotlin.math.min

data class ZoneDto(
    val id: Int,
    val name: String,
    val lat: Double,
    val long: Double,
    val region: String,
    val country: String,
    val routes: List<RouteToDto>? = null
) {
    companion object {
        fun from(z: ZoneInfo) =
            ZoneDto(
                z.id,
                z.name,
                z.lat,
                z.long,
                z.region,
                z.country
            )
    }
}

data class RouteToDto(
    val toId: Int,
    val toName: String,
    val toLat: Double,
    val toLong: Double,
    val timesRun: Int,
    val avgTime: Int,
    val medTime: Int,
    val fastestTime: Int,
    val fastestUser: String,
    val fastestTimestamp: Instant,
    val weight: Int
) {
    companion object {
        fun from(r: RouteInfo, z: ZoneInfo, totalExits: Int, totalRoutes: Int) =
            RouteToDto(
                r.toZone,
                z.name,
                z.lat,
                z.long,
                r.times.size,
                r.avg(),
                r.med(),
                r.min(),
                r.fastestUser,
                r.fastestTimestamp,
                calculateWeight(r.times.size, totalExits, totalRoutes)
            )

        const val WEIGHT_LIMIT = 5

        fun calculateWeight(routeRecords: Int, zoneRecords: Int, numberRoutes: Int): Int {
            //println("calculateWeight($routeRecords, $zoneRecords, $numberRoutes)")
            val evenDistribution = max(100 / numberRoutes, 25) // Consider max 4 routes
            val routePart = 100 * routeRecords / zoneRecords
            if (routePart < WEIGHT_LIMIT) return 0
            val weight = if (routePart <= evenDistribution) {
                1 + 4 * routePart / evenDistribution
            } else {
                5 + (routePart - evenDistribution) * 5 / (100 - evenDistribution)
            }
            //println("$percent / $part => ${min(weight, min(10, routeRecords))} ($weight)")
            return min(
                weight,
                min(10, max(zoneRecords / 2, 3)) // min 20 traverses for max weight
            )
        }
    }
}
