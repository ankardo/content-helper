package com.contenthelper.post;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PostScheduleTest {

    private final PostSchedule schedule = new PostSchedule();

    private static Clock clock(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
    }

    @Test
    void skipsTodayWhenThereIsNoTimeToEdit() {
        String window = schedule.nextWindow(clock("2026-10-21T12:00:00Z"));
        assertThat(window).startsWith("Thu 22 Oct 2026, 10:00 America/New_York (14:00 UTC");
    }

    @Test
    void skipsToTheNextWeekAfterThursdayMorning() {
        String window = schedule.nextWindow(clock("2026-10-22T15:00:00Z"));
        assertThat(window).startsWith("Tue 27 Oct 2026, 10:00 America/New_York (14:00 UTC");
    }

    @Test
    void skipsTheWeekend() {
        String window = schedule.nextWindow(clock("2026-10-23T15:00:00Z"));
        assertThat(window).startsWith("Tue 27 Oct 2026");
    }

    @Test
    void mondayMorningPicksTuesday() {
        String window = schedule.nextWindow(clock("2026-10-26T12:00:00Z"));
        assertThat(window).startsWith("Tue 27 Oct 2026");
    }

    @Test
    void reportsTheLocalZone() {
        String window = schedule.nextWindow(clock("2026-10-21T12:00:00Z"));
        assertThat(window).contains("UTC");
        assertThat(window).contains(java.time.ZoneId.systemDefault().getId());
    }
}
