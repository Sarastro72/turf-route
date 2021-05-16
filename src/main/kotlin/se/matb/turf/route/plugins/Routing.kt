package se.matb.turf.route.plugins

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.BadRequestException
import io.ktor.features.CORS
import io.ktor.features.NotFoundException
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.utils.io.core.toByteArray

fun Application.configureRouting() {

    install(CORS) {
        anyHost()
    }

    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        get("/zone/id/{zoneId}") {
            call.parameters["zoneId"]?.toInt()
                ?.let { queryManager.fetchZoneById(it) }
                ?.let { call.respond(it) }
                ?: call.respond(HttpStatusCode.NotFound)
        }

        get("/zone/name/{zoneName}") {
            call.parameters["zoneName"]
                ?.let { String(it.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8) }
                ?.let { queryManager.fetchZoneByName(it) }
                ?.let { call.respond(it) }
                ?: call.respond(HttpStatusCode.NotFound)
        }

        get("/zone/area/{swLat}/{swLong}/{neLat}/{neLong}") {
            val swLat = call.parameters["swLat"]?.toDoubleOrNull() ?: throw BadRequestException("Missing parameter")
            val swLong = call.parameters["swLong"]?.toDoubleOrNull() ?: throw BadRequestException("Missing parameter")
            val neLat = call.parameters["neLat"]?.toDoubleOrNull() ?: throw BadRequestException("Missing parameter")
            val neLong = call.parameters["neLong"]?.toDoubleOrNull() ?: throw BadRequestException("Missing parameter")

            call.respond(queryManager.fetchZonesByArea(swLat, swLong, neLat, neLong))
        }

        get("/zone/distance/{from}/{to}") {
            val from = call.parameters["from"]?.toIntOrNull() ?: throw BadRequestException("Bad or missing parameter")
            val to = call.parameters["to"]?.toIntOrNull() ?: throw BadRequestException("Bad or missing parameter")
            call.respond(mapOf("distance" to queryManager.zoneDistance(from, to)))
        }

        get("/route/fastest/{from}/{to}") {
            val from = call.parameters["from"]?.toIntOrNull() ?: throw BadRequestException("Bad or missing parameter")
            val to = call.parameters["to"]?.toIntOrNull() ?: throw BadRequestException("Bad or missing parameter")
            call.respond(queryManager.routeTo(from, to) ?: throw NotFoundException())
        }

        // Static feature. Try to access `/static/ktor_logo.svg`
        static("/static") {
            resources("static")
        }
    }
}
