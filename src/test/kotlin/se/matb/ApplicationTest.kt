package se.matb

import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import se.matb.turf.route.manager.RouteInfo
import se.matb.turf.route.plugins.configureRouting
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
            .parse("2021-05-05T14:46:55+0000")
        val formatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'kk:mm:ss'%2B0000'")
            .withZone(ZoneId.from(ZoneOffset.UTC))
        println(formatter.format(dt))
    }

    @Test
    fun `test sorted insert`() {
        val ri = RouteInfo(1, 2)
        ri.addTime(17)
        println(ri.times)
        ri.addTime(15)
        println(ri.times)
        ri.addTime(20)
        println(ri.times)
        ri.addTime(17)
        println(ri.times)
        ri.addTime(16)
        println(ri.times)
        ri.addTime(19)
        println(ri.times)
        ri.addTime(22)
        println(ri.times)
        ri.addTime(12)
        println(ri.times)
        ri.addTime(22)
        println(ri.times)
        ri.addTime(12)
        println(ri.times)
    }
}
