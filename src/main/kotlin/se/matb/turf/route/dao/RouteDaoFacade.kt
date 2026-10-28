package se.matb.turf.route.dao

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import se.matb.turf.logging.Logging
import se.matb.turf.route.dao.model.RouteInfo
import se.matb.turf.route.dao.model.RouteTimes
import se.matb.turf.route.dao.model.RouteWithDestination
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class RouteDaoFacade(val routeDao: RouteDao) : RouteDao {
    companion object : Logging()

    val routesCache: LoadingCache<Int, List<RouteInfo>> = CacheBuilder.newBuilder()
        .expireAfterWrite(60, TimeUnit.MINUTES)
        .build(CacheLoader.from { id -> routeDao.lookupRoutesByFromZone(id!!) })

    override fun getRoute(from: Int, to: Int): RouteInfo? =
        routeDao.getRoute(from, to)

    override fun lookupRoutesByFromZone(from: Int): List<RouteInfo> =
        kotlin.runCatching { routesCache.get(from) }.getOrElse { ex ->
            LOG.warn(ex) { "Failed to find route originating from zone $from" }
            emptyList()
        }

    override fun lookupRoutesInArea(
        swLat: Double,
        swLong: Double,
        neLat: Double,
        neLong: Double
    ): List<RouteWithDestination> =
        routeDao.lookupRoutesInArea(swLat, swLong, neLat, neLong)

    override fun storeRoute(route: RouteInfo) {
        routeDao.storeRoute(route)
        // Force update cache
        routesCache.invalidate(route.fromZone)
    }

    override fun forEachRouteTimes(consumer: Consumer<RouteTimes>) =
        routeDao.forEachRouteTimes(consumer)

    override fun lookupRoutesWithoutStats(afterFromZone: Int, afterToZone: Int, limit: Int): List<RouteTimes> =
        routeDao.lookupRoutesWithoutStats(afterFromZone, afterToZone, limit)

    override fun storeRouteStats(routes: List<RouteTimes>) =
        routeDao.storeRouteStats(routes)

    override fun getLastTimestamp(): Instant? =
        routeDao.getLastTimestamp()
}
