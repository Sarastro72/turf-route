package se.matb.turf.route.dao

import se.matb.turf.route.dao.model.RouteInfo
import se.matb.turf.route.dao.model.RouteWithDestination
import java.time.Instant
import java.util.function.Consumer

interface RouteDao {
    fun getRoute(from: Int, to: Int): RouteInfo?
    fun lookupRoutesByFromZone(from: Int): List<RouteInfo>
    fun lookupRoutesInArea(swLat: Double, swLong: Double, neLat: Double, neLong: Double): List<RouteWithDestination>
    fun storeRoute(route: RouteInfo)
    fun forEachTimes(consumer: Consumer<String>)
    fun getLastTimestamp(): Instant? = null
}
