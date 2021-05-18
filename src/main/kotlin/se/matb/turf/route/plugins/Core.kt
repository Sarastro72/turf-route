package se.matb.turf.route.plugins

import io.ktor.application.Application
import org.jdbi.v3.core.Jdbi
import se.matb.turf.route.dao.RouteDao
import se.matb.turf.route.dao.RouteDaoFacade
import se.matb.turf.route.dao.RouteMariaDbDao
import se.matb.turf.route.dao.ZoneDao
import se.matb.turf.route.dao.ZoneDaoFacade
import se.matb.turf.route.dao.ZoneMariaDbDao
import se.matb.turf.route.manager.QueryManager
import se.matb.turf.route.manager.TakeEventManager
import java.util.Properties

lateinit var zoneDao: ZoneDao
lateinit var routeDao: RouteDao
lateinit var queryManager: QueryManager

fun Application.configureCore() {
    val dbUrl = configString("ktor.dbUrl") ?: error("Need dbUrl in config")
    val properties = Properties()
    properties["user"] = configString("ktor.dbUser") ?: error("Need dbUser in config")
    properties["password"] = configString("ktor.security.dbPass") ?: error("Need dbPass in config")
    properties["sessionTimeZone"] = "UTC"
    val jdbi = Jdbi.create(dbUrl, properties).installPlugins()
    routeDao = RouteDaoFacade(jdbi.onDemand(RouteMariaDbDao::class.java))
    zoneDao = ZoneDaoFacade(jdbi.onDemand(ZoneMariaDbDao::class.java))

    queryManager = QueryManager(zoneDao, routeDao)
    TakeEventManager(routeDao, zoneDao, queryManager).start()
}

fun Application.configString(key: String) = environment.config.propertyOrNull(key)?.getString()
