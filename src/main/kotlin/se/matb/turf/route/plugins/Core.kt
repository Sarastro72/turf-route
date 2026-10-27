package se.matb.turf.route.plugins

import io.ktor.application.Application
import org.flywaydb.core.Flyway
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.sqlobject.kotlin.KotlinSqlObjectPlugin
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
    val dbUser = configString("ktor.dbUser") ?: error("Need dbUser in config")
    val dbPass = configString("ktor.security.dbPass") ?: error("Need dbPass in config")
    migrateDatabase(dbUrl, dbUser, dbPass)

    val properties = Properties()
    properties["user"] = dbUser
    properties["password"] = dbPass
    properties["sessionTimeZone"] = "UTC"
    val jdbi = Jdbi.create(dbUrl, properties)
        .installPlugins()
        .installPlugin(KotlinSqlObjectPlugin())
    routeDao = RouteDaoFacade(jdbi.onDemand(RouteMariaDbDao::class.java))
    zoneDao = ZoneDaoFacade(jdbi.onDemand(ZoneMariaDbDao::class.java))

    queryManager = QueryManager(zoneDao, routeDao)
    TakeEventManager(routeDao, zoneDao, queryManager).start()
}

// Baseline at version 3: databases created before Flyway already have the V1-V3 schema
private fun migrateDatabase(dbUrl: String, dbUser: String, dbPass: String) {
    Flyway.configure()
        .dataSource(dbUrl, dbUser, dbPass)
        .locations("classpath:migrations")
        .baselineOnMigrate(true)
        .baselineVersion("3")
        .load()
        .migrate()
}

fun Application.configString(key: String) = environment.config.propertyOrNull(key)?.getString()
