package se.matb.turf.route.dao

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import se.matb.turf.logging.Logging
import se.matb.turf.route.dao.model.RouteInfo
import java.time.Instant
import java.util.concurrent.TimeUnit

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

    override fun storeRoute(route: RouteInfo) {
        routeDao.storeRoute(route)
        // Force update cache
        routesCache.put(route.fromZone, routeDao.lookupRoutesByFromZone(route.fromZone))
    }

    override fun getAllRoutes(): Collection<RouteInfo> =
        routeDao.getAllRoutes()

    override fun countRoutes(): Int =
        routeDao.countRoutes()

    override fun getLastTimestamp(): Instant? =
        routeDao.getLastTimestamp()
}
