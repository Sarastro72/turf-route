package se.matb.turf.route.manager

import se.matb.turf.logging.Logging
import se.matb.turf.route.dao.RouteDao
import se.matb.turf.route.dao.model.RouteInfo
import se.matb.turf.route.dao.model.RouteStats
import kotlin.time.Duration.Companion.milliseconds

// Fills in the stats columns (V5) for routes stored before they existed, in key-ordered chunks.
// Safe to run alongside the take loop: routes stored in the meantime already have stats and are skipped.
class RouteStatsBackfill(private val routeDao: RouteDao) {
    companion object : Logging() {
        private const val CHUNK_SIZE = 1000
        private const val LOG_INTERVAL = 100_000
    }

    fun run(routesWithoutStats: Long) {
        LOG.info { "Backfilling stats for $routesWithoutStats routes in the background" }
        val start = System.currentTimeMillis()
        var done = 0L
        var afterFromZone = Int.MIN_VALUE
        var afterToZone = Int.MIN_VALUE
        kotlin.runCatching {
            while (true) {
                val chunk = routeDao.lookupRoutesWithoutStats(afterFromZone, afterToZone, CHUNK_SIZE)
                if (chunk.isEmpty()) break
                routeDao.storeRouteStats(chunk.map { it.copy(stats = RouteStats.from(RouteInfo.Times(it.times!!))) })
                afterFromZone = chunk.last().fromZone
                afterToZone = chunk.last().toZone
                if ((done + chunk.size) / LOG_INTERVAL > done / LOG_INTERVAL) {
                    LOG.info { "Backfilled stats for ${done + chunk.size} of $routesWithoutStats routes" }
                }
                done += chunk.size
            }
        }.onFailure { ex -> LOG.warn(ex) { "Stats backfill stopped after $done routes due to ${ex::class.simpleName} – ${ex.message}" } }
        val duration = (System.currentTimeMillis() - start).milliseconds
        LOG.info { "Backfilled stats for $done routes in $duration" }
    }
}
