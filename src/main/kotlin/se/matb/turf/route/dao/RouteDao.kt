package se.matb.turf.route.dao

import se.matb.turf.route.dao.model.RouteInfo
import java.time.Instant
import java.util.function.Consumer

interface RouteDao {
    fun getRoute(from: Int, to: Int): RouteInfo?
    fun lookupRoutesByFromZone(from: Int): List<RouteInfo>
    fun storeRoute(route: RouteInfo)
    fun forEachTimes(consumer: Consumer<String>)
    fun getLastTimestamp(): Instant? = null
}
