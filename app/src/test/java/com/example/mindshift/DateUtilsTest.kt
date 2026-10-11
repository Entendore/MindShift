package com.example.mindshift

import com.example.mindshift.screens.*
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class DateUtilsTest {

    private val ny = ZoneId.of("America/New_York")
    private val tokyo = ZoneId.of("Asia/Tokyo")

    private fun at(
        zone: ZoneId, y: Int, mo: Int, d: Int, h: Int, min: Int, sec: Int = 0
    ): java.time.Instant = ZonedDateTime.of(y, mo, d, h, min, sec, 0, zone).toInstant()

    @Before fun freeze() {
        AppClock.freezeAt(at(ny, 2025, 6, 4, 15, 0), ny) // Wed Jun 4 2025, 3 PM NY
    }

    @After fun thaw() { AppClock.unfreeze() }

    @Test fun `unfreeze returns to the real system clock`() {
        AppClock.unfreeze()
        assertEquals(ZoneId.systemDefault(), AppClock.zone())
        assertTrue(AppClock.nowMillis() > 0)
    }

    @Test fun `dayStamp encodes yyyymmdd`() {
        assertEquals(20250604, dayStamp(at(ny, 2025, 6, 4, 14, 30).toEpochMilli()))
    }

    @Test fun `todayStamp reads the frozen clock`() {
        assertEquals(20250604, todayStamp())
    }

    @Test fun `same instant is different calendar days in different zones`() {
        val instant = at(ny, 2025, 6, 4, 23, 30)
        assertEquals(20250604, dayStamp(instant.toEpochMilli(), ny))
        assertEquals(20250605, dayStamp(instant.toEpochMilli(), tokyo))
    }

    @Test fun `stamp flips exactly at local midnight`() {
        val justBefore = ZonedDateTime.of(2025, 6, 4, 23, 59, 59, 999_999_999, ny).toInstant()
        assertEquals(20250604, dayStamp(justBefore.toEpochMilli()))
        assertEquals(20250605, dayStamp(at(ny, 2025, 6, 5, 0, 0).toEpochMilli()))
    }

    @Test fun `addDays crosses months, years, and leap days`() {
        assertEquals(20250601, addDays(20250531, 1))
        assertEquals(20250101, addDays(20241231, 1))
        assertEquals(20240229, addDays(20240228, 1))
        assertEquals(20250131, addDays(20250201, -1))
    }

    @Test fun `lastNDayStamps ends at frozen today`() {
        assertEquals(listOf(20250602, 20250603, 20250604), lastNDayStamps(3))
    }

    @Test fun `range start is calendar midnight - spring forward`() {
        AppClock.freezeAt(at(ny, 2025, 3, 9, 12, 30), ny)
        val start = startOfDaysAgoMillis(1)
        assertEquals(20250308, dayStamp(start))
        assertEquals(36.5 * 3_600_000, AppClock.nowMillis() - start.toDouble(), 1.0)
    }

    @Test fun `range start is calendar midnight - fall back`() {
        AppClock.freezeAt(at(ny, 2025, 11, 2, 12, 30), ny)
        val start = startOfDaysAgoMillis(1)
        assertEquals(20251101, dayStamp(start))
        assertEquals(37.5 * 3_600_000, AppClock.nowMillis() - start.toDouble(), 1.0)
    }

    @Test fun `startOf7DaysAgoMillis matches startOfDaysAgoMillis`() {
        assertEquals(startOfDaysAgoMillis(7), startOf7DaysAgoMillis())
    }

    @Test fun `consecutive days ending today count up`() {
        assertEquals(3, streakFromStamps(setOf(20250602, 20250603, 20250604)))
    }

    @Test fun `yesterday alone still counts - grace day`() {
        assertEquals(1, streakFromStamps(setOf(20250603)))
    }

    @Test fun `a gap breaks the streak`() {
        assertEquals(1, streakFromStamps(setOf(20250604, 20250602)))
    }

    @Test fun `stale history with nothing recent is zero`() {
        assertEquals(0, streakFromStamps(setOf(20250601, 20250531)))
    }

    @Test fun `empty history has no streak`() {
        assertEquals(0, streakFromStamps(emptySet()))
    }

    @Test fun `best streak ignores recency`() {
        assertEquals(3, bestStreakFromStamps(setOf(20250522, 20250523, 20250524, 20250604)))
    }

    @Test fun `eastward flight keeps the streak alive`() {
        val stamps = setOf(20250602, 20250603)
        AppClock.freezeAt(at(tokyo, 2025, 6, 4, 10, 0), tokyo) // NY Tue 9 PM = Tokyo Wed 10 AM
        assertEquals(2, streakFromStamps(stamps))
    }

    @Test fun `westward travel does not double-count a day`() {
        val la = ZoneId.of("America/Los_Angeles")
        AppClock.freezeAt(at(la, 2025, 6, 3, 20, 30), la)
        assertEquals(2, streakFromStamps(setOf(20250602, 20250603)))
    }

    @Test fun `a date-line jump that erases a whole day reduces to the recent run`() {
        AppClock.freezeAt(at(tokyo, 2025, 6, 7, 9, 0), tokyo)
        assertEquals(1, streakFromStamps(setOf(20250605, 20250607)))
    }

    @Test fun `streak is computed in the clock zone, not the device zone`() {
        assertEquals(1, streakFromStamps(setOf(20250603)))
    }
}
