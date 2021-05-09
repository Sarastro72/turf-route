package se.matb.turf.route.dao

import se.matb.turf.route.dao.model.RouteInfo
import java.time.Instant

interface RouteDao {
    fun getRoute(from: Int, to: Int): RouteInfo?
    fun lookupRoutesByFromZone(from: Int): List<RouteInfo>
    fun storeRoute(route: RouteInfo)
    fun getAllRoutes(): Collection<RouteInfo>
    fun countRoutes(): Int
    fun getLastTimestamp(): Instant? = null
}
