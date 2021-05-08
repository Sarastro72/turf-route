package se.matb

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import org.assertj.core.api.Assertions.assertThat
import se.matb.turf.route.model.RouteInfo
import se.matb.turf.route.plugins.configureRouting
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testRoot() {
        withTestApplication({ configureRouting() }) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(HttpStatusCode.OK, response.status())
                assertEquals("Hello World!", response.content)
            }
        }
    }

    @Test
    fun `test date formatter`() {
        val dt = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'kk:mm:ss'+0000'")
            .withZone(ZoneId.from(ZoneOffset.UTC))
            .parse("2021-05-05T14:46:55+0000")
        val formatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'kk:mm:ss'%2B0000'")
            .withZone(ZoneId.from(ZoneOffset.UTC))
        println(formatter.format(dt))
    }

    @Test
    fun `test sorted insert large after small`() {
        val ri = RouteInfo(1, 2)
        ri.addTime(79)
        ri.addTime(107)
        println("${ri.times}")
        println("${ri.times.sorted()}")
        assertThat(ri.times).containsExactlyElementsOf(ri.times.sorted())
    }

    @Test
    fun `test sorted insert small after large`() {
        val ri = RouteInfo(1, 2)
        ri.addTime(107)
        ri.addTime(79)
        println("${ri.times}")
        println("${ri.times.sorted()}")
        assertThat(ri.times).containsExactlyElementsOf(ri.times.sorted())
    }

    @Test
    fun `test sorted insert 4 different from middle`() {
        val ri = RouteInfo(1, 2)
        ri.addTime(79)
        ri.addTime(107)
        ri.addTime(88)
        ri.addTime(99)
        println("${ri.times}")
        println("${ri.times.sorted()}")
        assertThat(ri.times).containsExactlyElementsOf(ri.times.sorted())
    }

    @Test
    fun `test sorted insert 4 different from edges`() {
        val ri = RouteInfo(1, 2)
        ri.addTime(99)
        ri.addTime(88)
        ri.addTime(79)
        ri.addTime(107)
        println("${ri.times}")
        println("${ri.times.sorted()}")
        assertThat(ri.times).containsExactlyElementsOf(ri.times.sorted())
    }

    @Test
    fun `test sorted insert duplicate in middle`() {
        val ri = RouteInfo(1, 2)
        ri.addTime(99)
        ri.addTime(88)
        ri.addTime(79)
        ri.addTime(107)
        ri.addTime(88)
        println("${ri.times}")
        println("${ri.times.sorted()}")
        assertThat(ri.times).containsExactlyElementsOf(ri.times.sorted())
    }

    @Test
    fun `test sorted insert duplicate at start`() {
        val ri = RouteInfo(1, 2)
        ri.addTime(99)
        ri.addTime(88)
        ri.addTime(79)
        ri.addTime(107)
        ri.addTime(79)
        println("${ri.times}")
        println("${ri.times.sorted()}")
        assertThat(ri.times).containsExactlyElementsOf(ri.times.sorted())
    }

    @Test
    fun `test sorted insert duplicate at end`() {
        val ri = RouteInfo(1, 2)
        ri.addTime(99)
        ri.addTime(88)
        ri.addTime(107)
        ri.addTime(79)
        ri.addTime(107)
        println("${ri.times}")
        println("${ri.times.sorted()}")
        assertThat(ri.times).containsExactlyElementsOf(ri.times.sorted())
    }

    @Test
    fun `test deserialize`() {
        val objectMapper = ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerModule(KotlinModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
            .configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false)

        val target = objectMapper.readValue("""{"time": "2021-05-07T12:22:43+0000"}""", InstantHolder::class.java)
        println(target)
    }

    data class InstantHolder(val time: Instant)
}
