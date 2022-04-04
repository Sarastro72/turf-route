package se.matb

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import se.matb.turf.route.dto.RouteToDto
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.test.Test

class ApplicationTest {
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
    fun `test deserialize`() {
        val objectMapper = ObjectMapper()
            .registerModule(JavaTimeModule())
            .registerModule(KotlinModule.Builder().build())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false)
            .configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false)

        val target = objectMapper.readValue("""{"time": "2021-05-07T12:22:43+0000"}""", InstantHolder::class.java)
        println(target)
    }

    @Test
    fun `test weight calculation`() {
        RouteToDto.calculateWeight(1, 1, 1)
        RouteToDto.calculateWeight(1, 2, 2)
        RouteToDto.calculateWeight(10, 100, 2)
        RouteToDto.calculateWeight(20, 100, 2)
        RouteToDto.calculateWeight(30, 100, 2)
        RouteToDto.calculateWeight(40, 100, 2)
        RouteToDto.calculateWeight(50, 100, 2)
        RouteToDto.calculateWeight(60, 100, 2)
        RouteToDto.calculateWeight(70, 100, 2)
        RouteToDto.calculateWeight(80, 100, 2)
        RouteToDto.calculateWeight(90, 100, 2)
        RouteToDto.calculateWeight(100, 101, 2)
        RouteToDto.calculateWeight(6, 100, 3)
        RouteToDto.calculateWeight(11, 100, 3)
        RouteToDto.calculateWeight(22, 100, 3)
        RouteToDto.calculateWeight(33, 100, 3)
        RouteToDto.calculateWeight(44, 100, 3)
        RouteToDto.calculateWeight(55, 100, 3)
        RouteToDto.calculateWeight(66, 100, 3)
        RouteToDto.calculateWeight(77, 100, 3)
        RouteToDto.calculateWeight(88, 100, 3)
        RouteToDto.calculateWeight(99, 100, 3)
    }

    data class InstantHolder(val time: Instant)
}
