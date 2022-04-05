package se.matb.turf.route.dao.model

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import se.matb.turf.logging.Logging
import java.sql.ResultSet
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

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
    class ZoneRowMapper : RowMapper<ZoneInfo> {
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
    val times: Times,
    var fastestUser: String = "",
    var fastestTimestamp: Instant = Instant.now(),
    val updated: Instant = Instant.now()
) {
    companion object : Logging()

    val fastest: Int
        get() = times.fastest()

    val avg: Int
        get() = times.avg()

    val med: Int
        get() = times.med()

    val size: Int
        get() = times.size

    fun time(i: Int) = times.get(i)

    class Times(str: String) {
        val times: MutableList<Pair<Int, Int>>
        var size: Int

        init {
            if (str.contains(":")) {
                times = str
                    .split(",")
                    .map {
                        it.split(":").let { p ->
                            Pair(p[0].toInt(), p.getOrNull(1)?.toInt() ?: 1)
                        }
                    }
                    .toMutableList()
            } else {
                times = ArrayList()
                str.split(",").forEach { add(it.toInt()) }
            }
            size = times.sumOf { it.second }
        }

        fun add(time: Int) {
            if (times.isEmpty()) {
                times.add(Pair(time, 1))
            } else {
                for ((i, t) in times.withIndex()) {
                    if (time == t.first) {
                        times[i] = Pair(time, t.second + 1)
                        break
                    }
                    if (time < t.first) {
                        times.add(i, Pair(time, 1))
                        break
                    }
                    if (i == times.size - 1) {
                        times.add(Pair(time, 1))
                        break
                    }
                }
            }
            size++
        }

        fun get(i: Int): Int {
            var acc = 0
            for (t in times) {
                acc += t.second
                if (acc > i)
                    return t.first
            }
            throw IndexOutOfBoundsException("Index i out of bounds for size $size")
        }

        fun fastest() = times[0].first

        fun avg() = times.sumOf { it.first * it.second } / size

        fun med(): Int {
            val target = (size + 1) / 2
            var pos = 0
            for (t in times) {
                pos += t.second
                if (pos >= target)
                    return t.first
            }
            return -1
        }

        override fun toString(): String =
            times.joinToString(",") {
                if (it.second == 1)
                    "${it.first}"
                else
                    "${it.first}:${it.second}"
            }
    }

    class RouteRowMapper : RowMapper<RouteInfo> {
        override fun map(rs: ResultSet, ctx: StatementContext): RouteInfo = with(rs) {
            RouteInfo(
                getInt("fromZone"),
                getInt("toZone"),
                Times(getString("times")),
                getString("fastestUser"),
                getObject("fastestTimestamp", LocalDateTime::class.java).toInstant(ZoneOffset.UTC),
                getObject("updated", LocalDateTime::class.java).toInstant(ZoneOffset.UTC),
            )
        }
    }
}
