package se.matb.turf.route.dao

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import org.jdbi.v3.sqlobject.customizer.Bind
import org.jdbi.v3.sqlobject.customizer.BindBean
import se.matb.turf.logging.Logging
import se.matb.turf.route.dao.model.ZoneInfo
import java.util.concurrent.TimeUnit


class ZoneDaoFacade(val zoneDao: ZoneDao) : ZoneDao {
    companion object : Logging()

    private val zoneCache: LoadingCache<Int, ZoneInfo> = CacheBuilder.newBuilder()
        .expireAfterWrite(60, TimeUnit.MINUTES)
        .build(CacheLoader.from { id -> zoneDao.lookupZone(id!!) })

    override fun lookupZone(@Bind("id") id: Int): ZoneInfo? =
        kotlin.runCatching { zoneCache.get(id) }.getOrNull()

    override fun lookupZone(@Bind("name") name: String): ZoneInfo? =
        zoneDao.lookupZone(name)

    override fun lookupZonesInArea(
        @Bind("swLat") swLat: Double,
        @Bind("swLong") swLong: Double,
        @Bind("neLat") neLat: Double,
        @Bind("neLong") neLong: Double
    ): List<ZoneInfo> =
        zoneDao.lookupZonesInArea(swLat, swLong, neLat, neLong)

    override fun storeZone(@BindBean("zone") zone: ZoneInfo) {
        zoneDao.storeZone(zone)
        zoneCache.put(zone.id, zone)
    }

    override fun getAllZones(): Collection<ZoneInfo> =
        zoneDao.getAllZones()

    override fun countZones(): Int =
        zoneDao.countZones()
}
