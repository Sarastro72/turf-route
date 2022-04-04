package se.matb.turf.route.manager

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import se.matb.turf.client.TakeOver
import se.matb.turf.client.TurfApiClient
import se.matb.turf.logging.Logging
import se.matb.turf.route.dao.RouteDao
import se.matb.turf.route.dao.ZoneDao
import se.matb.turf.route.dao.model.RouteInfo
import se.matb.turf.route.dao.model.TakeInfo
import se.matb.turf.route.dao.model.ZoneInfo
import java.time.Duration
import java.time.Instant

const val INTERVAL_MILLIS: Long = 60000
const val ROUTE_MAX_TIME_MINUTES: Long = 30

class TakeEventManager(
    private val routeDao: RouteDao,
    private val zoneDao: ZoneDao,
    private val queryManager: QueryManager
) {

    companion object : Logging()

    private val turfApiClient = TurfApiClient()
    private val playerCache: Cache<Int, TakeInfo> = CacheBuilder.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(ROUTE_MAX_TIME_MINUTES))
        .build()

    // State
    private var running = false
    private var finished = true
    private var lastEventTime: Instant? = routeDao.getLastTimestamp()
    private var statCounter = 0

    fun start() {
        if (!running) {
            running = true
            finished = false
            CoroutineScope(Dispatchers.IO).launch { takeLoop() }
        }
    }

    fun stop() {
        running = false
        runBlocking {
            while (!finished) delay(500L)
        }
    }

    private suspend fun takeLoop() {
        preLoadPlayerCache()
        LOG.info { "Starting take loop with last updated timestamp = $lastEventTime" }
        delay(2000) // Respect 1 call per second limitation of API
        while (running) {
            kotlin.runCatching {
                val events = turfApiClient.fetchEvents(lastEventTime)
                handleEvents(events)
            }
                .getOrElse { ex -> LOG.warn(ex) { "Failed to fetch events due to ${ex::class.simpleName} – ${ex.message}" } }
            delay(INTERVAL_MILLIS)
        }
        LOG.info { "Take loop stopped with last event timestamp $lastEventTime" }
        finished = true
    }

    private fun preLoadPlayerCache() {
        LOG.info { "Preloading player cache from takes until $lastEventTime" }
        kotlin.runCatching {
            turfApiClient.fetchEvents()
                .reversed()
                .filter { take -> !take.time.isAfter(lastEventTime) }
                .forEach { take ->
                    take.zone.run {
                        zoneDao.storeZone(ZoneInfo(id, name, latitude, longitude, region.name, region.country))
                    }
                    playerCache.put(take.currentOwner.id, TakeInfo(take.zone.id, take.time))
                }
        }
        LOG.info { "Preloaded ${playerCache.size()} players" }
    }

    private fun handleEvents(events: List<TakeOver>) {
        if (events.isNotEmpty()) {
            LOG.info { "+++ Handling ${events.size} new takes since $lastEventTime" }
            lastEventTime = events[0].time
            events.reversed().forEach { take ->
                take.zone.run {
                    // Keep zone data updated
                    zoneDao.storeZone(ZoneInfo(id, name, latitude, longitude, region.name, region.country))
                }
                playerCache.getIfPresent(take.currentOwner.id)?.let { lastTake ->
                    val time = Duration.between(lastTake.time, take.time).toSeconds().toInt()
                    val route = routeDao.getRoute(lastTake.zoneId, take.zone.id)
                        ?: RouteInfo(
                            lastTake.zoneId,
                            take.zone.id,
                            RouteInfo.Times("$time"),
                            take.currentOwner.name,
                            take.time
                        )
                    routeDao.storeRoute(route)
                    logRoute(lastTake, take, route, time)
                }
                playerCache.put(take.currentOwner.id, TakeInfo(take.zone.id, take.time))
            }
        }
        logStats()
    }

    private fun logRoute(lastTake: TakeInfo, take: TakeOver, route: RouteInfo, time: Int) {
        kotlin.runCatching {
            val speed = queryManager.routeSpeed(lastTake.zoneId, take.zone.id, time).toInt()
            var logString = "${take.zone.region.name}: ${zoneDao.lookupZone(lastTake.zoneId)?.name} " +
                "-> ${take.zone.name} ${niceTime(time)} " +
                "${speed}km/h " +
                "${take.currentOwner.name} – ${route.size} ${route.fastest}/${route.med}/${route.avg}"
            if (route.times.size == 1) logString += " 🆕"
            if (route.times.size > 1 && route.time(1) > time) logString += " 🥇"
            if (speed > 50) logString += " 🚨"
            LOG.info { logString }
        }.onFailure { ex ->
            LOG.warn(ex) {
                "Failed logging ${lastTake.zoneId} to ${take.zone.name} for ${take.currentOwner.name} due to ${ex::class.simpleName}: ${ex.message}"
            }
        }
    }

    private fun logStats() {
        if (statCounter++ % 4 == 0) {
            val traverses = routeDao.getAllRoutes().sumOf { it.times.size }
            LOG.info { "/--------------- Stats ----------------" }
            LOG.info { "| Known zones: ${zoneDao.countZones()}" }
            LOG.info { "| Registered traverses: $traverses" }
            LOG.info { "| Registered routes: ${routeDao.countRoutes()}" }
            LOG.info { "| Active players (30m): ${playerCache.size()}" }
            LOG.info { "\\--------------------------------------" }
        }
    }
}

fun niceTime(seconds: Int) = "${seconds / 60}m${seconds % 60}s"
