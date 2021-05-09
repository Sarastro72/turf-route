package se.matb.turf.client

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import se.matb.turf.logging.Logging
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class TurfApiClient {
    companion object : Logging()

    private var client: HttpClient = HttpClient(CIO) {
        install(JsonFeature) {
            serializer = JacksonSerializer {
                //registerModule(KotlinModule())
                registerModule(JavaTimeModule())
                configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
                configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
            }
        }
    }
    private val formatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss'%2B0000'")
        .withZone(ZoneId.from(ZoneOffset.UTC))


    fun fetchEvents(from: Instant? = null): List<TakeOver> = runBlocking {
        if (from == null)
            client.get("https://api.turfgame.com/v4/feeds/takeover")
        else
            client.get("https://api.turfgame.com/v4/feeds/takeover?afterDate=${formatter.format(from)}")
    }
}
