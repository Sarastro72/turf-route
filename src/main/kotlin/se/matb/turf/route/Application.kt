package se.matb

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import se.matb.turf.route.manager.TakeEventManager
import se.matb.turf.route.plugins.configureHTTP
import se.matb.turf.route.plugins.configureRouting
import se.matb.turf.route.plugins.configureSerialization

fun main() {

    TakeEventManager().start()

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting()
        configureHTTP()
        configureSerialization()
    }.start(wait = true)
}
