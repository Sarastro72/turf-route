package se.matb.turf.logging

import kotlin.time.Duration.Companion.milliseconds

class Timer private constructor(val name: String, val silent: Boolean = false) {
    val start: Long = System.currentTimeMillis()
    var last: Long = start

    companion object : Logging() {
        private val timers: MutableMap<String, Timer> = HashMap()

        fun start(name: String, silent: Boolean = false): Timer {
            return Timer(name, silent).also { timers[name] = it }
        }

        fun get(name: String): Timer {
            return timers[name] ?: start(name)
        }

        fun remove(name: String) {
            timers.remove(name)
        }
    }

    fun time(flag: String) {
        val now = System.currentTimeMillis()
        val time = now - last
        val total = now - start
        last = now
        if (!silent) {
            LOG.info("$name – $flag: ${time.milliseconds}, total: ${total.milliseconds}")
        }
    }

    fun stop(flag: String = "finished") {
        time(flag)
        remove(name)
    }
}
