package se.matb.turf.route.manager

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import se.matb.turf.client.TakeOver
import se.matb.turf.client.TurfApiClient
import se.matb.turf.logging.Logging
import se.matb.turf.route.dao.RouteDao
import se.matb.turf.route.dao.ZoneDao
import se.matb.turf.route.model.RouteInfo
import se.matb.turf.route.model.TakeInfo
import se.matb.turf.route.model.ZoneInfo
import java.time.Duration
import java.time.Instant

const val INTERVAL_MILLIS: Long = 20000
const val ROUTE_MAX_TIME_MINUTES: Long = 30

class TakeEventManager(
    private val routeDao: RouteDao,
    private val zoneDao: ZoneDao
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
            GlobalScope.launch { takeLoop() }
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
        while (running) {
            kotlin.runCatching {
                val events = turfApiClient.fetchEvents(lastEventTime)
                handleEvents(events)
            }.getOrElse { LOG.warn { "Failed to fetch events due to ${it::class.simpleName} – ${it.message}" } }
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
                    playerCache.put(take.currentOwner.id, TakeInfo(take.zone.id, take.time))
                }
        }
    }

    private fun handleEvents(events: List<TakeOver>) {
        if (events.isNotEmpty()) {
            lastEventTime = events[0].time
            LOG.info { "Handling ${events.size} new takes" }
            events.reversed().forEach { take ->
                playerCache.getIfPresent(take.currentOwner.id)?.let { lastTake ->
                    val time = Duration.between(lastTake.time, take.time).toSeconds().toInt()
                    val route = routeDao.lookupRoute(lastTake.zoneId, take.zone.id)
                        ?: RouteInfo(lastTake.zoneId, take.zone.id)
                    route.addTime(time, take.currentOwner.name, take.time)
                    routeDao.storeRoute(route)
                    LOG.info {
                        "New time in ${take.zone.region.name} from ${zoneDao.lookupZone(lastTake.zoneId)?.name} to ${take.zone.name} " +
                            "in ${niceTime(time)} by ${take.currentOwner.name} – ${route.times}"
                    }
                    if (route.times.size > 1 && time < route.times[1]) {
                        LOG.info {
                            "!!!New best time ${niceTime(time)} registered between " +
                                "${zoneDao.lookupZone(lastTake.zoneId)?.name} and ${take.zone.name} by ${take.currentOwner.name}"
                        }
                    }
                }
                playerCache.put(take.currentOwner.id, TakeInfo(take.zone.id, take.time))
                if (zoneDao.lookupZone(take.zone.id) == null) {
                    // Store new zone
                    take.zone.run {
                        zoneDao.storeZone(ZoneInfo(id, name, latitude, longitude, region.name, region.country))
                    }
                }
            }
        }
        logStats()
    }

    private fun logStats() {
        if (statCounter++ % 4 == 0) {
            val regions: MutableMap<String, Int> = HashMap()
            routeDao.getAllRoutes().forEach { route ->
                zoneDao.lookupZone(route.toZone)?.let { zone ->
                    val key = "${zone.country}.${zone.region}"
                    regions[key] = regions.getOrDefault(key, 0) + 1
                }
            }
            LOG.info { " ---------------- Stats ----------------" }
            LOG.info { "/ Known routes per region:" }
            regions.entries
                .sortedBy { it.key }
                .forEach {
                    LOG.info { "|  ${it.key}: ${it.value} " }
                }
            LOG.info { "| Known zones: ${zoneDao.countZones()}" }
            LOG.info { "| Known routes: ${routeDao.countRoutes()}" }
            LOG.info { "\\ Active players: ${playerCache.size()}" }
            LOG.info { " ---------------------------------------" }
        }
    }
}

fun niceTime(seconds: Int) = "${seconds / 60}m${seconds % 60}s"
