val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val jackson_version: String by project

plugins {
    application
    kotlin("jvm") version "1.5.0"
}

group = "se.matb"
version = "0.0.1"
application {
    mainClass.set("se.matb.turf.route.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-host-common:$ktor_version")
    implementation("io.ktor:ktor-jackson:$ktor_version")
    implementation("io.ktor:ktor-client-json:$ktor_version")
    implementation("io.ktor:ktor-client-jackson:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jackson_version")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
    implementation("com.google.guava:guava:30.1.1-jre")
    implementation("io.github.microutils:kotlin-logging:1.12.0")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
}
