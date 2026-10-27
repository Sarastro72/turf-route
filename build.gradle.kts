import java.util.zip.ZipFile

val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val jackson_version: String by project
val jdbi_version: String by project

plugins {
    application
    kotlin("jvm") version "2.2.10"
}

group = "se.matb"
version = "0.0.1"
application {
    mainClass.set("se.matb.turf.route.ApplicationKt")
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
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
    implementation("org.jdbi:jdbi3-core:$jdbi_version")
    implementation("org.jdbi:jdbi3-sqlobject:$jdbi_version")
    implementation("org.jdbi:jdbi3-kotlin-sqlobject:$jdbi_version")
    implementation("org.jdbi:jdbi3-kotlin:$jdbi_version")
    implementation("org.mariadb.jdbc:mariadb-java-client:3.5.5")
    implementation("org.flywaydb:flyway-core:13.10.0")
    implementation("org.flywaydb:flyway-mysql:13.10.0")
    testImplementation("org.assertj:assertj-core:3.19.0")
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")
}

// Several dependencies (e.g. flyway-core and flyway-mysql) register plugins in the same
// META-INF/services file, so they must be merged rather than overwritten in the fat jar
val mergeServiceFiles by tasks.registering {
    val runtimeJars = configurations.runtimeClasspath
    val outputDir = layout.buildDirectory.dir("merged-services")
    inputs.files(runtimeJars)
    outputs.dir(outputDir)

    doLast {
        val services = sortedMapOf<String, MutableSet<String>>()
        runtimeJars.get()
            .filter { it.name.endsWith("jar") }
            .forEach { jar ->
                ZipFile(jar).use { zip ->
                    zip.entries()
                        .asSequence()
                        .filter { !it.isDirectory && it.name.startsWith("META-INF/services/") }
                        .forEach { entry ->
                            services.getOrPut(entry.name) { linkedSetOf() } += zip.getInputStream(entry)
                                .bufferedReader()
                                .readLines()
                                .map { it.substringBefore('#').trim() }
                                .filter { it.isNotEmpty() }
                        }
                }
            }

        val dir = outputDir.get().asFile
        dir.deleteRecursively()
        services.forEach { (name, providers) ->
            dir.resolve(name).apply {
                parentFile.mkdirs()
                writeText(providers.joinToString("\n", postfix = "\n"))
            }
        }
    }
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "se.matb.turf.route.ApplicationKt"
    }

    from(
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it).matching { exclude("META-INF/services/**") } }
    )
    from(mergeServiceFiles)

    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
