package com.example.mindshift.screens

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * Single source of "now" and "the day boundary" for the whole app.
 *
 * Production: real system clock, device timezone.
 * Tests: freeze both via [freezeAt] — every streak, chart, range, and
 * reminder label becomes fully deterministic.
 *
 * Rule of thumb: *day* math goes through AppClock; raw instants
 * (createdAt, snoozedUntil, export timestamps) can keep using
 * System.currentTimeMillis() — zones don't affect instants.
 */
object AppClock {

    @Volatile
    var clock: Clock = Clock.systemDefaultZone()

    fun nowMillis(): Long = clock.millis()

    fun zone(): ZoneId = clock.zone

    /** Test hook: pin both "now" and the timezone. */
    fun freezeAt(instant: Instant, zone: ZoneId) {
        clock = Clock.fixed(instant, zone)
    }

    /** Test hook: return to the real clock. Always call in @After / finally. */
    fun unfreeze() {
        clock = Clock.systemDefaultZone()
    }
}
