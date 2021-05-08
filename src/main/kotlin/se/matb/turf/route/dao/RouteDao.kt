package se.matb.turf.route.dao

import se.matb.turf.route.model.RouteInfo
import java.time.Instant

interface RouteDao {
    fun lookupRoute(from: Int, to: Int): RouteInfo?
    fun storeRoute(route: RouteInfo)
    fun getAllRoutes(): Collection<RouteInfo>
    fun countRoutes(): Int
    fun getLastTimestamp(): Instant? = null
}
