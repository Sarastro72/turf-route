package se.matb.turf.route.dao

import se.matb.turf.route.manager.RouteInfo

interface RouteDao {
    fun lookupRoute(from: Int, to: Int): RouteInfo?
    fun storeRoute(route: RouteInfo)
    fun getAllRoutes(): Collection<RouteInfo>
    fun countRoutes(): Int
}
