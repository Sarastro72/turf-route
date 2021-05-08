package se.matb.turf.route.dao

import org.jdbi.v3.sqlobject.config.RegisterArgumentFactory
import org.jdbi.v3.sqlobject.config.RegisterRowMapper
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import se.matb.turf.route.dao.db.TimesArgumentFactory
import se.matb.turf.route.model.RouteInfo
import java.time.Instant

const val ROUTE_SELECT_COLUMNS = "fromZone, toZone, times, fastestUser, fastestTimestamp, updated"
const val ROUTE_INSERT_COLUMNS = "fromZone, toZone, times, fastestUser, fastestTimestamp"

@RegisterArgumentFactory(TimesArgumentFactory::class)
@RegisterRowMapper(RouteInfo.RouteRowMapper::class)
interface RouteMariaDbDao : RouteDao {

    @SqlQuery(
        """SELECT $ROUTE_SELECT_COLUMNS FROM route
        WHERE fromZone=:fromZone
        AND toZone=:toZone
        """
    )
    override fun lookupRoute(
        @Bind("fromZone") from: Int,
        @Bind("toZone") to: Int
    ): RouteInfo?

    @SqlUpdate(
        """INSERT INTO route ($ROUTE_INSERT_COLUMNS)
            VALUES (
                :route.fromZone,
                :route.toZone,
                :route.times,
                :route.fastestUser,
                :route.fastestTimestamp
            )
            ON DUPLICATE KEY UPDATE
                times=:route.times,
                fastestUser=:route.fastestUser,
                fastestTimestamp=:route.fastestTimestamp
        """
    )
    override fun storeRoute(
        @BindBean("route") route: RouteInfo
    )

    @SqlQuery(
        """SELECT $ROUTE_SELECT_COLUMNS FROM route
        """
    )
    override fun getAllRoutes(): Collection<RouteInfo>

    @SqlQuery(
        """SELECT count(*) FROM route
        """
    )
    override fun countRoutes(): Int

    @SqlQuery(
        """
            SELECT updated FROM route
            ORDER BY updated DESC
            LIMIT 1
        """
    )
    override fun getLastTimestamp(): Instant?
}
