package se.matb.turf.route.manager

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import se.matb.turf.client.TurfApiClient
import se.matb.turf.dto.TakeOver
import se.matb.turf.logging.Logging
import se.matb.turf.route.dao.RouteDao
import se.matb.turf.route.dao.ZoneDao
import java.time.Duration
import java.time.Instant
import kotlin.math.max

const val INTERVAL_MILLIS: Long = 20000
const val ROUTE_MAX_TIME_MINUTES: Long = 30

class TakeEventManager(
    val routeDao: RouteDao,
    val zoneDao: ZoneDao
) {

    companion object : Logging()

    private val turfApiClient = TurfApiClient()
    private val playerCache: Cache<Int, TakeInfo> = CacheBuilder.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(ROUTE_MAX_TIME_MINUTES))
        .build()

    // State
    private var running = false
    private var finished = true
    private var lastEventTime: Instant? = null

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
        while (running) {
            kotlin.runCatching {
                val events = turfApiClient.fetchEvents(lastEventTime)
                handleEvents(events)
            }.getOrElse { LOG.warn { "Failed to fetch events due to ${it::class.simpleName} – ${it.message}" } }
            delay(INTERVAL_MILLIS)
        }
        finished = true
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
                take.zone.run {
                    zoneDao.storeZone(ZoneInfo(id, name, latitude, longitude, region.name, region.country))
                }
            }
        }
        logStats()
    }

    var statCounter = 0
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
            LOG.info { "| Known routes: ${routeDao.countRoutes()}" }
            LOG.info { "\\ Active players: ${playerCache.size()}" }
            LOG.info { " ---------------------------------------" }
        }
    }
}

data class TakeInfo(
    val zoneId: Int,
    val time: Instant
)

data class ZoneInfo(
    val id: Int,
    val name: String,
    val lat: Double,
    val long: Double,
    val region: String,
    val country: String
)

data class RouteInfo(
    val fromZone: Int,
    val toZone: Int,
    val times: MutableList<Int> = ArrayList()
) {
    companion object : Logging()

    val fastestTime: Int
        get() = times[0]
    var fastestUser = ""
    var fastestTimestamp = Instant.now()

    // Sorted insert
    fun addTime(time: Int, user: String = "-", timestamp: Instant = Instant.now()) {
        if (times.isEmpty()) {
            times.add(time)
            fastestUser = user
            fastestTimestamp = timestamp
            return
        }
        var pos = times.size / 2
        var step = pos
        while (true) {
            step = max(step / 2, 1)
            when {
                pos == times.size -> break
                pos == 0 && times[0] >= time -> break
                times[pos] < time -> pos += step
                times[pos - 1] > time -> pos -= step
                else -> break
            }
        }
        if (time < times[0]) {
            fastestUser = user
            fastestTimestamp = timestamp
        }
        times.add(pos, time)
    }

    fun avg(): Int? {
        return if (times.isEmpty()) null
        else times.sum() / times.size
    }

    fun med(): Int? {
        return if (times.isEmpty()) null
        else times.toList()[times.size / 2]
    }

    fun min(): Int? {
        return times.firstOrNull()
    }

    fun max(): Int? {
        return times.lastOrNull()
    }
}

fun niceTime(seconds: Int) = "${seconds / 60}m ${seconds % 60}s"
