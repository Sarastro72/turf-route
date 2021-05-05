package se.matb.turf.dto

import java.time.Instant

data class TakeOver(
    val type: String,
    val time: Instant,
    val zone: Zone,
    val currentOwner: User,
    val latitude: Double,
    val longitude: Double
)

data class User(
    val id: Int,
    val name: String
)

data class Zone(
    val id: Int,
    val name: String,
    val currentOwner: User,
    val previousOwner: User?,
    val latitude: Double,
    val longitude: Double,
    val pointsPerHour: Int,
    val takeoverPoints: Int,
    val totalTakeovers: Int,
    val dateLastTaken: Instant,
    val dateCreated: Instant,
    val region: Region
)

data class Region(
    val id: Int,
    val name: String,
    val country: String
)
