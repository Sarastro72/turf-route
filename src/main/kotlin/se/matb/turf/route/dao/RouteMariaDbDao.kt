package se.matb.turf.route.dao

import org.jdbi.v3.sqlobject.config.RegisterArgumentFactory
import org.jdbi.v3.sqlobject.config.RegisterRowMapper
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.customizer.FetchSize
import org.jdbi.v3.sqlobject.statement.SqlBatch
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import se.matb.turf.route.dao.db.TimesArgumentFactory
import se.matb.turf.route.dao.model.RouteInfo
import se.matb.turf.route.dao.model.RouteTimes
import se.matb.turf.route.dao.model.RouteWithDestination
import java.time.Instant
import java.util.function.Consumer

const val ROUTE_SELECT_COLUMNS = "fromZone, toZone, times, fastestUser, fastestTimestamp, updated"
const val ROUTE_INSERT_COLUMNS = "fromZone, toZone, times, fastestUser, fastestTimestamp, timesRun, avgTime, medTime, fastestTime"
// times is only read for routes without stats, i.e. not reached by the backfill yet
const val ROUTE_SUMMARY_COLUMNS = "r.fromZone, r.toZone, r.fastestUser, r.fastestTimestamp, r.timesRun, r.avgTime, r.medTime, r.fastestTime, " +
    "IF(r.timesRun IS NULL, r.times, NULL) AS times"

@RegisterArgumentFactory(TimesArgumentFactory::class)
@RegisterRowMapper(RouteInfo.RouteRowMapper::class)
interface RouteMariaDbDao : RouteDao {

    @SqlQuery(
        """SELECT $ROUTE_SELECT_COLUMNS FROM route
        WHERE fromZone=:fromZone
        AND toZone=:toZone
        """
    )
    override fun getRoute(
        @Bind("fromZone") from: Int,
        @Bind("toZone") to: Int
    ): RouteInfo?

    @SqlQuery(
        """SELECT $ROUTE_SELECT_COLUMNS FROM route
        WHERE fromZone=:fromZone
        """
    )
    override fun lookupRoutesByFromZone(
        @Bind("fromZone") from: Int
    ): List<RouteInfo>

    // All routes starting in the area, with their destination zone (t), in one query
    @SqlQuery(
        """SELECT $ROUTE_SUMMARY_COLUMNS, t.id, t.name, t.latitude, t.longitude, t.region, t.country
        FROM zone f
        JOIN route r ON r.fromZone = f.id
        JOIN zone t ON t.id = r.toZone
        WHERE f.latitude BETWEEN :swLat AND :neLat
        AND f.longitude BETWEEN :swLong AND :neLong
        """
    )
    @RegisterRowMapper(RouteWithDestination.RouteWithDestinationRowMapper::class)
    override fun lookupRoutesInArea(
        @Bind("swLat") swLat: Double,
        @Bind("swLong") swLong: Double,
        @Bind("neLat") neLat: Double,
        @Bind("neLong") neLong: Double
    ): List<RouteWithDestination>

    @SqlUpdate(
        """INSERT INTO route ($ROUTE_INSERT_COLUMNS)
            VALUES (
                :route.fromZone,
                :route.toZone,
                :route.times,
                :route.fastestUser,
                :route.fastestTimestamp,
                :route.size,
                :route.avg,
                :route.med,
                :route.fastest
            )
            ON DUPLICATE KEY UPDATE
                times=:route.times,
                fastestUser=:route.fastestUser,
                fastestTimestamp=:route.fastestTimestamp,
                timesRun=:route.size,
                avgTime=:route.avg,
                medTime=:route.med,
                fastestTime=:route.fastest
        """
    )
    override fun storeRoute(
        @BindBean("route") route: RouteInfo
    )

    // Fetched in chunks and handed to the consumer row by row, so the whole table is never held in memory.
    // The consumer must not call the DAO: that would run on the same connection as this streaming query.
    @SqlQuery(
        """SELECT r.fromZone, r.toZone, r.timesRun, r.avgTime, r.medTime, r.fastestTime, IF(r.timesRun IS NULL, r.times, NULL) AS times
        FROM route r
        """
    )
    @FetchSize(1000)
    @RegisterRowMapper(RouteTimes.RouteTimesRowMapper::class)
    override fun forEachRouteTimes(consumer: Consumer<RouteTimes>)

    // Next chunk of routes without stats after the given key, in key order
    @SqlQuery(
        """SELECT fromZone, toZone, timesRun, avgTime, medTime, fastestTime, times
        FROM route
        WHERE timesRun IS NULL
        AND (fromZone > :fromZone OR (fromZone = :fromZone AND toZone > :toZone))
        ORDER BY fromZone, toZone
        LIMIT :limit
        """
    )
    @RegisterRowMapper(RouteTimes.RouteTimesRowMapper::class)
    override fun lookupRoutesWithoutStats(
        @Bind("fromZone") afterFromZone: Int,
        @Bind("toZone") afterToZone: Int,
        @Bind("limit") limit: Int
    ): List<RouteTimes>

    // Leaves updated unchanged, and skips routes that got stats from storeRoute in the meantime
    @SqlBatch(
        """UPDATE route
        SET timesRun=:route.stats.timesRun,
            avgTime=:route.stats.avgTime,
            medTime=:route.stats.medTime,
            fastestTime=:route.stats.fastestTime,
            updated=updated
        WHERE fromZone=:route.fromZone
        AND toZone=:route.toZone
        AND timesRun IS NULL
        """
    )
    override fun storeRouteStats(
        @BindBean("route") routes: List<RouteTimes>
    )

    @SqlQuery(
        """
            SELECT updated FROM route
            ORDER BY updated DESC
            LIMIT 1
        """
    )
    override fun getLastTimestamp(): Instant?
}
