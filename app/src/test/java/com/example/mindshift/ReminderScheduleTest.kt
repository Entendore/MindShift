package com.example.mindshift

import com.example.mindshift.data.ReminderItem
import com.example.mindshift.reminders.ItemReminderScheduler
import com.example.mindshift.screens.AppClock
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Calendar

class ReminderScheduleTest {

    // Wednesday, June 4 2025, 3:00 PM
    private fun wednesday3pm(): Calendar = Calendar.getInstance().apply {
        clear(); set(2025, Calendar.JUNE, 4, 15, 0, 0)
    }

    private fun rem(hour: Int, minute: Int = 0, days: String = "") =
        ReminderItem(label = "t", hour = hour, minute = minute, days = days)

    @Test fun `fires same day when the time is still ahead`() {
        val n = ItemReminderScheduler.nextFire(rem(18), wednesday3pm())
        assertEquals(Calendar.WEDNESDAY, n.get(Calendar.DAY_OF_WEEK))
        assertEquals(4, n.get(Calendar.DAY_OF_MONTH))
        assertEquals(18, n.get(Calendar.HOUR_OF_DAY))
    }

    @Test fun `fires tomorrow when today's time has passed`() {
        val n = ItemReminderScheduler.nextFire(rem(10), wednesday3pm())
        assertEquals(5, n.get(Calendar.DAY_OF_MONTH))
        assertEquals(10, n.get(Calendar.HOUR_OF_DAY))
    }

    @Test fun `skips forward to the next selected weekday`() {
        val n = ItemReminderScheduler.nextFire(rem(10, days = "2"), wednesday3pm())
        assertEquals(Calendar.MONDAY, n.get(Calendar.DAY_OF_WEEK))
        assertEquals(9, n.get(Calendar.DAY_OF_MONTH))
    }

    @Test fun `today counts when its weekday is selected and time is ahead`() {
        val n = ItemReminderScheduler.nextFire(rem(18, days = "4"), wednesday3pm())
        assertEquals(4, n.get(Calendar.DAY_OF_MONTH))
    }

    @Test fun `a pending snooze wins over the schedule`() {
        val until = System.currentTimeMillis() + 600_000
        val r = rem(18).copy(snoozedUntil = until)
        assertEquals(until, ItemReminderScheduler.effectiveNextMillis(r))
    }

    @Test fun `without a snooze the schedule wins`() {
        val r = rem(18)
        assertEquals(
            ItemReminderScheduler.nextFire(r).timeInMillis,
            ItemReminderScheduler.effectiveNextMillis(r)
        )
    }

    @Test fun `stale snooze is ignored`() {
        val r = rem(18).copy(snoozedUntil = System.currentTimeMillis() - 1000)
        assertEquals(
            ItemReminderScheduler.nextFire(r).timeInMillis,
            ItemReminderScheduler.effectiveNextMillis(r)
        )
    }

    @Test fun `time labels`() {
        assertEquals("6:05 PM", ItemReminderScheduler.timeLabel(18, 5))
        assertEquals("12:00 AM", ItemReminderScheduler.timeLabel(0, 0))
        assertEquals("12:30 PM", ItemReminderScheduler.timeLabel(12, 30))
    }

    @Test fun `day labels`() {
        assertEquals("One time", ItemReminderScheduler.daysLabel(""))
        assertEquals("Every day", ItemReminderScheduler.daysLabel("1,2,3,4,5,6,7"))
        assertEquals("Weekdays", ItemReminderScheduler.daysLabel("2,3,4,5,6"))
        assertEquals("Weekends", ItemReminderScheduler.daysLabel("1,7"))
        assertEquals("Mon, Wed", ItemReminderScheduler.daysLabel("2,4"))
    }

    @Test fun `default nextFire follows the AppClock zone - travel re-anchoring`() {
        val tokyo = ZoneId.of("Asia/Tokyo")
        val ny = ZoneId.of("America/New_York")
        AppClock.freezeAt(
            ZonedDateTime.of(2025, 6, 4, 15, 0, 0, 0, ny).toInstant(), tokyo
        )
        try {
            val n = ItemReminderScheduler.nextFire(rem(18))
            assertEquals(tokyo, n.timeZone.toZoneId())
            assertEquals(5, n.get(Calendar.DAY_OF_MONTH))
            assertEquals(18, n.get(Calendar.HOUR_OF_DAY))
        } finally { AppClock.unfreeze() }
    }

    @Test fun `nextFireLabel is deterministic under a frozen clock`() {
        val ny = ZoneId.of("America/New_York")
        AppClock.freezeAt(
            ZonedDateTime.of(2025, 6, 4, 15, 0, 0, 0, ny).toInstant(), ny
        )
        try {
            assertEquals("Today · 6:00 PM", ItemReminderScheduler.nextFireLabel(rem(18)))
            assertEquals("Mon · 6:00 PM", ItemReminderScheduler.nextFireLabel(rem(18, days = "2")))
        } finally { AppClock.unfreeze() }
    }
}
