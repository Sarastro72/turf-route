package se.matb.turf.route.dao

import se.matb.turf.route.manager.RouteInfo

class RouteInMemoryDao : RouteDao {
    private val routes = HashMap<Pair<Int, Int>, RouteInfo>()

    override fun lookupRoute(from: Int, to: Int): RouteInfo? =
        routes[Pair(from, to)]

    override fun storeRoute(route: RouteInfo) {
        routes[Pair(route.fromZone, route.toZone)] = route
    }

    override fun getAllRoutes(): Collection<RouteInfo> =
        routes.values

    override fun countRoutes(): Int =
        routes.size
}
