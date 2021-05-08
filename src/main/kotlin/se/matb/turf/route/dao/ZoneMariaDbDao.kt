package se.matb.turf.route.dao

import org.jdbi.v3.sqlobject.config.RegisterRowMapper
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import se.matb.turf.route.model.ZoneInfo

const val ZONE_COLUMNS: String = "id, name, latitude, longitude, region, country"

@RegisterRowMapper(ZoneInfo.ZoneRowMapper::class)
interface ZoneMariaDbDao : ZoneDao {

    @SqlQuery(
        """SELECT $ZONE_COLUMNS FROM zone
            WHERE id=:id
        """
    )
    override fun lookupZone(
        @Bind("id") id: Int
    ): ZoneInfo?


    @SqlUpdate(
        """INSERT INTO zone ($ZONE_COLUMNS)
            VALUES (
                :zone.id,
                :zone.name,
                :zone.lat,
                :zone.long,
                :zone.region,
                :zone.country
            )
            """
    )
    override fun storeZone(
        @BindBean("zone") zone: ZoneInfo
    )

    @SqlQuery(
        """SELECT $ZONE_COLUMNS FROM zone
        """
    )
    override fun getAllZones(): Collection<ZoneInfo>

    @SqlQuery(
        """SELECT COUNT(*) FROM zone
        """
    )
    override fun countZones(): Int

}
