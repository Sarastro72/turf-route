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
import java.time.Duration
import java.time.Instant
import kotlin.math.max

const val INTERVAL = 20000L

class TakeEventManager {

    companion object : Logging()

    private val turfApiClient = TurfApiClient()
    private val playerCache: Cache<Int, TakeInfo> = CacheBuilder.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(30))
        .build()
    private val zones = HashMap<Int, ZoneInfo>()
    private val routes = HashMap<Pair<Int, Int>, RouteInfo>()

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
            val events = turfApiClient.fetchEvents(lastEventTime)
            handleEvents(events)
            println()
            delay(INTERVAL)
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
                    val route = routes.getOrPut(Pair(lastTake.zoneId, take.zone.id)) {
                        RouteInfo(lastTake.zoneId, take.zone.id)
                    }
                    route.addTime(time)
                    LOG.info {
                        "New time in ${take.zone.region.name} from ${zones[lastTake.zoneId]?.name} to ${take.zone.name} " +
                            "in ${niceTime(time)} by ${take.currentOwner.name} – ${route.times}"
                    }
                }
                playerCache.put(take.currentOwner.id, TakeInfo(take.zone.id, take.time))
                zones[take.zone.id] = ZoneInfo(take.zone.id, take.zone.name, take.zone.latitude, take.zone.longitude)
            }
            LOG.info { "Known routes: ${routes.size}" }
        }
        logStats()
    }

    private fun logStats() {

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
    val long: Double
)

data class RouteInfo(
    val fromZone: Int,
    val toZone: Int,
    val times: MutableList<Int> = ArrayList()
) {
    // Sorted insert
    fun addTime(time: Int) {
        if (times.isEmpty()) {
            times.add(time)
            return
        }
        var pos = times.size / 2
        var step = pos
        while (true) {
            step = max(step / 2, 1)
            if (pos == times.size)
                break
            if (pos == 0)
                break
            when {
                times[pos - 1] > time -> pos -= step
                times[pos] < time -> pos += step
                else -> break
            }
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
