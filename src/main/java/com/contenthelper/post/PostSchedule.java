package com.contenthelper.post;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Picks the next publish window: Tuesday to Thursday, 10:00 in the audience timezone. The audience
 * is US first, so the window is anchored in New York and shown in UTC and the local zone.
 */
@Service
public class PostSchedule {

    static final ZoneId AUDIENCE = ZoneId.of("America/New_York");
    private static final LocalTime TIME = LocalTime.of(10, 0);
    private static final Duration MIN_LEAD = Duration.ofHours(24);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", Locale.ENGLISH);
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

    public String nextWindow(Clock clock) {
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(AUDIENCE);
        LocalDate date = now.toLocalDate();
        ZonedDateTime slot = date.atTime(TIME).atZone(AUDIENCE);
        while (!isPublishDay(slot.getDayOfWeek()) || slot.isBefore(now.plus(MIN_LEAD))) {
            date = date.plusDays(1);
            slot = date.atTime(TIME).atZone(AUDIENCE);
        }
        ZonedDateTime utc = slot.withZoneSameInstant(ZoneOffset.UTC);
        ZoneId local = ZoneId.systemDefault();
        ZonedDateTime localTime = slot.withZoneSameInstant(local);
        return DAY.format(slot) + " " + AUDIENCE.getId() + " (" + HOUR.format(utc) + " UTC, " + HOUR.format(localTime)
                + " " + local.getId() + ")";
    }

    private static boolean isPublishDay(DayOfWeek day) {
        return day == DayOfWeek.TUESDAY || day == DayOfWeek.WEDNESDAY || day == DayOfWeek.THURSDAY;
    }
}
