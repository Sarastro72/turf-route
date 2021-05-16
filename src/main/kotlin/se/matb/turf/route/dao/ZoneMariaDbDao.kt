package se.matb.turf.route.dao

import org.jdbi.v3.sqlobject.config.RegisterRowMapper
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.customizer.BindBean
import org.jdbi.v3.sqlobject.statement.SqlQuery
import org.jdbi.v3.sqlobject.statement.SqlUpdate
import se.matb.turf.route.dao.model.ZoneInfo

const val ZONE_COLUMNS: String = "id, name, latitude, longitude, region, country"

@RegisterRowMapper(ZoneInfo.ZoneRowMapper::class)
interface ZoneMariaDbDao : ZoneDao {

    @SqlQuery(
        """SELECT $ZONE_COLUMNS FROM zone
            WHERE id=:id
        """
    )
    override fun lookupZone(@Bind("id") id: Int): ZoneInfo?


    @SqlQuery(
        """SELECT $ZONE_COLUMNS FROM zone
            WHERE name=:name
        """
    )
    override fun lookupZone(@Bind("name") name: String): ZoneInfo?

    @SqlQuery(
        """SELECT $ZONE_COLUMNS FROM zone
            WHERE latitude BETWEEN :swLat AND :neLat
            AND longitude BETWEEN :swLong AND :neLong
        """
    )
    override fun lookupZonesInArea(
        @Bind("swLat") swLat: Double,
        @Bind("swLong") swLong: Double,
        @Bind("neLat") neLat: Double,
        @Bind("neLong") neLong: Double
    ): List<ZoneInfo>

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
    override fun storeZone(@BindBean("zone") zone: ZoneInfo)

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
