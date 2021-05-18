package se.matb.turf.route.dao.model

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import se.matb.turf.logging.Logging
import java.sql.ResultSet
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.max

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
    val country: String,
) {
    class ZoneRowMapper() : RowMapper<ZoneInfo> {
        override fun map(rs: ResultSet, ctx: StatementContext): ZoneInfo = with(rs) {
            ZoneInfo(
                getInt("id"),
                getString("name"),
                getDouble("latitude"),
                getDouble("longitude"),
                getString("region"),
                getString("country")
            )
        }
    }
}

data class RouteInfo(
    val fromZone: Int,
    val toZone: Int,
    val times: MutableList<Int> = ArrayList(),
    var fastestUser: String = "",
    var fastestTimestamp: Instant = Instant.now(),
    val updated: Instant = Instant.now()
) {
    companion object : Logging()

    val timesString: String
        get() = times.joinToString(",")
    val fastestTime: Int
        get() = times[0]

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

    fun avg(): Int {
        return if (times.isEmpty()) 5949
        else times.sum() / times.size
    }

    fun med(): Int {
        return if (times.isEmpty()) 5949
        else times.toList()[times.size / 2]
    }

    fun min(): Int {
        return times.firstOrNull() ?: 5949
    }

    fun max(): Int {
        return times.lastOrNull() ?: 5949
    }

    class RouteRowMapper : RowMapper<RouteInfo> {
        override fun map(rs: ResultSet, ctx: StatementContext): RouteInfo = with(rs) {
            RouteInfo(
                getInt("fromZone"),
                getInt("toZone"),
                getString("times").split(",").map(String::toInt).toMutableList(),
                getString("fastestUser"),
                getObject("fastestTimestamp", LocalDateTime::class.java).toInstant(ZoneOffset.UTC),
                getObject("updated", LocalDateTime::class.java).toInstant(ZoneOffset.UTC),
            )
        }
    }
}
