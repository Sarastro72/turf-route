package se.matb.turf.route.plugins

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing

fun Application.configureRouting() {

    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        get("/json/jackson") {
            call.respond(Payload("high"))
        }


        // Static feature. Try to access `/static/ktor_logo.svg`
        static("/static") {
            resources("static")
        }
    }
}
