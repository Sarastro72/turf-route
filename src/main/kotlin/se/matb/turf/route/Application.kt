package se.matb.turf.route

import io.ktor.application.Application
import se.matb.turf.route.plugins.configureHTTP
import se.matb.turf.route.plugins.configureRouting
import se.matb.turf.route.plugins.configureScheduling
import se.matb.turf.route.plugins.configureSerialization

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module(testing: Boolean = false) {
    configureScheduling()
    configureSerialization()
    configureHTTP()
    configureRouting()
}
