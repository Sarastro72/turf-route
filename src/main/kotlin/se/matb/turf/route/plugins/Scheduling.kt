package se.matb.turf.route.plugins

import io.ktor.application.Application
import io.ktor.application.ApplicationEnvironment
import org.jdbi.v3.core.Jdbi
import se.matb.turf.route.dao.RouteDao
import se.matb.turf.route.dao.RouteMariaDbDao
import se.matb.turf.route.dao.ZoneDao
import se.matb.turf.route.dao.ZoneMariaDbDao
import se.matb.turf.route.manager.TakeEventManager
import java.util.Properties

fun Application.configureScheduling() {
    val dbUrl = environment.config.propertyOrNull("ktor.dbUrl")?.getString() ?: error("Need dbUrl in config")
    val properties = Properties()
    properties["user"] = environment.config.propertyOrNull("ktor.dbUser")?.getString() ?: error("Need dbUser in config")
    properties["password"] =
        environment.config.propertyOrNull("ktor.security.dbPass")?.getString() ?: error("Need dbPass in config")
    val jdbi = Jdbi.create(dbUrl, properties).installPlugins()
    val routeDao: RouteDao = jdbi.onDemand(RouteMariaDbDao::class.java)
    val zoneDao: ZoneDao = jdbi.onDemand(ZoneMariaDbDao::class.java)

    TakeEventManager(routeDao, zoneDao).start()
}

private fun dbUrl(environment: ApplicationEnvironment): String {
    val dbPass = environment.config.propertyOrNull("ktor.security.dbPass")?.getString() ?: ""
    return environment.config.propertyOrNull("ktor.dbUrl")?.getString()?.replace("####", dbPass) ?: ""
}
