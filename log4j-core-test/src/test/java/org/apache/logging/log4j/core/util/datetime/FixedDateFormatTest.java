/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.logging.log4j.core.util.datetime;

import static org.apache.logging.log4j.core.util.datetime.FixedDateFormat.FixedFormat.DEFAULT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.apache.logging.log4j.core.time.MutableInstant;
import org.apache.logging.log4j.core.util.datetime.FixedDateFormat.FixedFormat;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junitpioneer.jupiter.DefaultLocale;

/**
 * Tests {@link FixedDateFormat}.
 */
@ResourceLock(value = Resources.LOCALE, mode = ResourceAccessMode.READ)
class FixedDateFormatTest {

    private boolean containsNanos(final FixedFormat fixedFormat) {
        final String pattern = fixedFormat.getPattern();
        return pattern.endsWith("n") || pattern.matches(".+n+X*") || pattern.matches(".+n+Z*");
    }

    @Test
    void testConstructorDisallowsNullFormat() {
        assertThrows(NullPointerException.class, () -> new FixedDateFormat(null, TimeZone.getDefault()));
    }

    @Test
    void testConstructorDisallowsNullTimeZone() {
        assertThrows(NullPointerException.class, () -> new FixedDateFormat(FixedFormat.ABSOLUTE, null));
    }

    @Test
    void testCreateIfSupported_customTimeZoneIfOptionsArrayWithTimeZoneElement() {
        final FixedDateFormat fmt = FixedDateFormat.createIfSupported(DEFAULT.getPattern(), "GMT+08:00", "");
        assertEquals(DEFAULT.getPattern(), fmt.getFormat());
        assertEquals(TimeZone.getTimeZone("GMT+08:00"), fmt.getTimeZone());
    }

    @Test
    void testCreateIfSupported_defaultIfOptionsArrayEmpty() {
        final FixedDateFormat fmt = FixedDateFormat.createIfSupported(Strings.EMPTY_ARRAY);
        assertEquals(DEFAULT.getPattern(), fmt.getFormat());
    }

    @Test
    void testCreateIfSupported_defaultIfOptionsArrayNull() {
        final FixedDateFormat fmt = FixedDateFormat.createIfSupported((String[]) null);
        assertEquals(DEFAULT.getPattern(), fmt.getFormat());
    }

    @Test
    void testCreateIfSupported_defaultIfOptionsArrayWithSingleNullElement() {
        final FixedDateFormat fmt = FixedDateFormat.createIfSupported(new String[1]);
        assertEquals(DEFAULT.getPattern(), fmt.getFormat());
        assertEquals(TimeZone.getDefault(), fmt.getTimeZone());
    }

    @Test
    void testCreateIfSupported_defaultTimeZoneIfOptionsArrayWithSecondNullElement() {
        final FixedDateFormat fmt = FixedDateFormat.createIfSupported(DEFAULT.getPattern(), null, "");
        assertEquals(DEFAULT.getPattern(), fmt.getFormat());
        assertEquals(TimeZone.getDefault(), fmt.getTimeZone());
    }

    @Test
    void testCreateIfSupported_nonNullIfNameMatches() {
        for (final FixedDateFormat.FixedFormat format : FixedDateFormat.FixedFormat.values()) {
            final String[] options = {format.name()};
            assertNotNull(FixedDateFormat.createIfSupported(options), format.name());
        }
    }

    @Test
    void testCreateIfSupported_nonNullIfPatternMatches() {
        for (final FixedDateFormat.FixedFormat format : FixedDateFormat.FixedFormat.values()) {
            final String[] options = {format.getPattern()};
            assertNotNull(FixedDateFormat.createIfSupported(options), format.name());
        }
    }

    @Test
    void testCreateIfSupported_nullIfNameDoesNotMatch() {
        final String[] options = {"DEFAULT3"};
        assertNull(FixedDateFormat.createIfSupported(options), "DEFAULT3");
    }

    @Test
    void testCreateIfSupported_nullIfPatternDoesNotMatch() {
        final String[] options = {"y M d H m s"};
        assertNull(FixedDateFormat.createIfSupported(options), "y M d H m s");
    }

    private static final String[][] US_CENTRAL_DATA_SPRING = {
        // US/Central, UTC
        {"2017-03-11 18:00:00,000", "2017-03-12 00:00:00,000"}, //
        {"2017-03-11 19:00:00,000", "2017-03-12 01:00:00,000"}, //
        {"2017-03-11 20:00:00,000", "2017-03-12 02:00:00,000"}, //
        {"2017-03-11 21:00:00,000", "2017-03-12 03:00:00,000"}, //
        {"2017-03-11 22:00:00,000", "2017-03-12 04:00:00,000"}, //
        {"2017-03-11 23:00:00,000", "2017-03-12 05:00:00,000"}, //
        {"2017-03-12 00:00:00,000", "2017-03-12 06:00:00,000"}, //
        {"2017-03-12 01:00:00,000", "2017-03-12 07:00:00,000"}, //
        {"2017-03-12 03:00:00,000", "2017-03-12 08:00:00,000"}, //  DST jump at 2am US central time
        {"2017-03-12 04:00:00,000", "2017-03-12 09:00:00,000"}, //
        {"2017-03-12 05:00:00,000", "2017-03-12 10:00:00,000"}, //
        {"2017-03-12 06:00:00,000", "2017-03-12 11:00:00,000"}, //
        {"2017-03-12 07:00:00,000", "2017-03-12 12:00:00,000"}, //
        {"2017-03-12 08:00:00,000", "2017-03-12 13:00:00,000"}, //
        {"2017-03-12 09:00:00,000", "2017-03-12 14:00:00,000"}, //
        {"2017-03-12 10:00:00,000", "2017-03-12 15:00:00,000"}, //
        {"2017-03-12 11:00:00,000", "2017-03-12 16:00:00,000"}, //
        {"2017-03-12 12:00:00,000", "2017-03-12 17:00:00,000"}, //
        {"2017-03-12 13:00:00,000", "2017-03-12 18:00:00,000"}, //
        {"2017-03-12 14:00:00,000", "2017-03-12 19:00:00,000"}, //
        {"2017-03-12 15:00:00,000", "2017-03-12 20:00:00,000"}, //
        {"2017-03-12 16:00:00,000", "2017-03-12 21:00:00,000"}, //
        {"2017-03-12 17:00:00,000", "2017-03-12 22:00:00,000"}, //
        {"2017-03-12 18:00:00,000", "2017-03-12 23:00:00,000"}, // 24
        {"2017-03-12 19:00:00,000", "2017-03-13 00:00:00,000"}, //
        {"2017-03-12 20:00:00,000", "2017-03-13 01:00:00,000"}, //
        {"2017-03-12 21:00:00,000", "2017-03-13 02:00:00,000"}, //
        {"2017-03-12 22:00:00,000", "2017-03-13 03:00:00,000"}, //
        {"2017-03-12 23:00:00,000", "2017-03-13 04:00:00,000"}, //
        {"2017-03-13 00:00:00,000", "2017-03-13 05:00:00,000"}, //
        {"2017-03-13 01:00:00,000", "2017-03-13 06:00:00,000"}, //
        {"2017-03-13 02:00:00,000", "2017-03-13 07:00:00,000"}, //
        {"2017-03-13 03:00:00,000", "2017-03-13 08:00:00,000"}, //
        {"2017-03-13 04:00:00,000", "2017-03-13 09:00:00,000"}, //
        {"2017-03-13 05:00:00,000", "2017-03-13 10:00:00,000"}, //
        {"2017-03-13 06:00:00,000", "2017-03-13 11:00:00,000"}, //
    };

    private static final String[][] CHILE_DATA_SPRING = {
        // America/Santiago, UTC
        {"2019-09-07 23:00:00,000", "2019-09-08 03:00:00,000"}, //
        {"2019-09-08 01:00:00,000", "2019-09-08 04:00:00,000"}, // DST jump at 12am Chile time
        {"2019-09-08 02:00:00,000", "2019-09-08 05:00:00,000"}, //
        {"2019-09-08 03:00:00,000", "2019-09-08 06:00:00,000"}, //
        {"2019-09-08 04:00:00,000", "2019-09-08 07:00:00,000"}, //
        {"2019-09-08 05:00:00,000", "2019-09-08 08:00:00,000"}, //
        {"2019-09-08 06:00:00,000", "2019-09-08 09:00:00,000"}, //
        {"2019-09-08 07:00:00,000", "2019-09-08 10:00:00,000"}, //
        {"2019-09-08 08:00:00,000", "2019-09-08 11:00:00,000"}, //
        {"2019-09-08 09:00:00,000", "2019-09-08 12:00:00,000"}, //
        {"2019-09-08 10:00:00,000", "2019-09-08 13:00:00,000"}, //
        {"2019-09-08 11:00:00,000", "2019-09-08 14:00:00,000"}, //
        {"2019-09-08 12:00:00,000", "2019-09-08 15:00:00,000"}, //
        {"2019-09-08 13:00:00,000", "2019-09-08 16:00:00,000"}, //
        {"2019-09-08 14:00:00,000", "2019-09-08 17:00:00,000"}, //
        {"2019-09-08 15:00:00,000", "2019-09-08 18:00:00,000"}, //
        {"2019-09-08 16:00:00,000", "2019-09-08 19:00:00,000"}, //
        {"2019-09-08 17:00:00,000", "2019-09-08 20:00:00,000"}, //
        {"2019-09-08 18:00:00,000", "2019-09-08 21:00:00,000"}, //
        {"2019-09-08 19:00:00,000", "2019-09-08 22:00:00,000"}, //
        {"2019-09-08 20:00:00,000", "2019-09-08 23:00:00,000"}, //
        {"2019-09-08 21:00:00,000", "2019-09-09 00:00:00,000"}, //
        {"2019-09-08 22:00:00,000", "2019-09-09 01:00:00,000"}, //
        {"2019-09-08 23:00:00,000", "2019-09-09 02:00:00,000"}, //
        {"2019-09-09 00:00:00,000", "2019-09-09 03:00:00,000"}, //
        {"2019-09-09 01:00:00,000", "2019-09-09 04:00:00,000"}, //
    };

    private static final String[][] US_CENTRAL_DATA_FALL = {
        // US/Central, UTC
        {"2017-11-04 19:00:00,000", "2017-11-05 00:00:00,000"}, //
        {"2017-11-04 20:00:00,000", "2017-11-05 01:00:00,000"}, //
        {"2017-11-04 21:00:00,000", "2017-11-05 02:00:00,000"}, //
        {"2017-11-04 22:00:00,000", "2017-11-05 03:00:00,000"}, //
        {"2017-11-04 23:00:00,000", "2017-11-05 04:00:00,000"}, //
        {"2017-11-05 00:00:00,000", "2017-11-05 05:00:00,000"}, //
        {"2017-11-05 01:00:00,000", "2017-11-05 06:00:00,000"}, //  DST jump at 2am US central time
        {"2017-11-05 01:00:00,000", "2017-11-05 07:00:00,000"}, //
        {"2017-11-05 02:00:00,000", "2017-11-05 08:00:00,000"}, //
        {"2017-11-05 03:00:00,000", "2017-11-05 09:00:00,000"}, //
        {"2017-11-05 04:00:00,000", "2017-11-05 10:00:00,000"}, //
        {"2017-11-05 05:00:00,000", "2017-11-05 11:00:00,000"}, //
        {"2017-11-05 06:00:00,000", "2017-11-05 12:00:00,000"}, //
        {"2017-11-05 07:00:00,000", "2017-11-05 13:00:00,000"}, //
        {"2017-11-05 08:00:00,000", "2017-11-05 14:00:00,000"}, //
        {"2017-11-05 09:00:00,000", "2017-11-05 15:00:00,000"}, //
        {"2017-11-05 10:00:00,000", "2017-11-05 16:00:00,000"}, //
        {"2017-11-05 11:00:00,000", "2017-11-05 17:00:00,000"}, //
        {"2017-11-05 12:00:00,000", "2017-11-05 18:00:00,000"}, //
        {"2017-11-05 13:00:00,000", "2017-11-05 19:00:00,000"}, //
        {"2017-11-05 14:00:00,000", "2017-11-05 20:00:00,000"}, //
        {"2017-11-05 15:00:00,000", "2017-11-05 21:00:00,000"}, //
        {"2017-11-05 16:00:00,000", "2017-11-05 22:00:00,000"}, //
        {"2017-11-05 17:00:00,000", "2017-11-05 23:00:00,000"}, //
        {"2017-11-05 18:00:00,000", "2017-11-06 00:00:00,000"}, //
        {"2017-11-05 19:00:00,000", "2017-11-06 01:00:00,000"}, //
        {"2017-11-05 20:00:00,000", "2017-11-06 02:00:00,000"}, //
        {"2017-11-05 21:00:00,000", "2017-11-06 03:00:00,000"}, //
        {"2017-11-05 22:00:00,000", "2017-11-06 04:00:00,000"}, //
        {"2017-11-05 23:00:00,000", "2017-11-06 05:00:00,000"}, //
        {"2017-11-06 00:00:00,000", "2017-11-06 06:00:00,000"}, //
        {"2017-11-06 01:00:00,000", "2017-11-06 07:00:00,000"}, //
        {"2017-11-06 02:00:00,000", "2017-11-06 08:00:00,000"}, //
        {"2017-11-06 03:00:00,000", "2017-11-06 09:00:00,000"}, //
        {"2017-11-06 04:00:00,000", "2017-11-06 10:00:00,000"}, //
        {"2017-11-06 05:00:00,000", "2017-11-06 11:00:00,000"}, //
    };

    private static final String[][] CHILE_DATA_FALL = {
        // America/Santiago, UTC
        {"2019-04-05 23:00:00,000", "2019-04-06 02:00:00,000"}, //
        {"2019-04-06 00:00:00,000", "2019-04-06 03:00:00,000"}, //
        {"2019-04-06 01:00:00,000", "2019-04-06 04:00:00,000"}, //
        {"2019-04-06 02:00:00,000", "2019-04-06 05:00:00,000"}, //
        {"2019-04-06 03:00:00,000", "2019-04-06 06:00:00,000"}, //
        {"2019-04-06 04:00:00,000", "2019-04-06 07:00:00,000"}, //
        {"2019-04-06 05:00:00,000", "2019-04-06 08:00:00,000"}, //
        {"2019-04-06 06:00:00,000", "2019-04-06 09:00:00,000"}, //
        {"2019-04-06 07:00:00,000", "2019-04-06 10:00:00,000"}, //
        {"2019-04-06 08:00:00,000", "2019-04-06 11:00:00,000"}, //
        {"2019-04-06 09:00:00,000", "2019-04-06 12:00:00,000"}, //
        {"2019-04-06 10:00:00,000", "2019-04-06 13:00:00,000"}, //
        {"2019-04-06 11:00:00,000", "2019-04-06 14:00:00,000"}, //
        {"2019-04-06 12:00:00,000", "2019-04-06 15:00:00,000"}, //
        {"2019-04-06 13:00:00,000", "2019-04-06 16:00:00,000"}, //
        {"2019-04-06 14:00:00,000", "2019-04-06 17:00:00,000"}, //
        {"2019-04-06 15:00:00,000", "2019-04-06 18:00:00,000"}, //
        {"2019-04-06 16:00:00,000", "2019-04-06 19:00:00,000"}, //
        {"2019-04-06 17:00:00,000", "2019-04-06 20:00:00,000"}, //
        {"2019-04-06 18:00:00,000", "2019-04-06 21:00:00,000"}, //
        {"2019-04-06 19:00:00,000", "2019-04-06 22:00:00,000"}, //
        {"2019-04-06 20:00:00,000", "2019-04-06 23:00:00,000"}, //
        {"2019-04-06 21:00:00,000", "2019-04-07 00:00:00,000"}, //
        {"2019-04-06 22:00:00,000", "2019-04-07 01:00:00,000"}, //
        {"2019-04-06 23:00:00,000", "2019-04-07 02:00:00,000"}, //
        {"2019-04-06 23:00:00,000", "2019-04-07 03:00:00,000"}, // DST jump at 12 am
        {"2019-04-07 00:00:00,000", "2019-04-07 04:00:00,000"}, //
        {"2019-04-07 01:00:00,000", "2019-04-07 05:00:00,000"}, //
    };

    static Stream<Arguments> testDaylightSaving() {
        return Stream.of(
                // DST start
                Arguments.of(
                        US_CENTRAL_DATA_SPRING,
                        ZoneId.of("US/Central"),
                        LocalDateTime.of(2017, Month.MARCH, 12, 0, 0)
                                .atZone(ZoneOffset.UTC)
                                .toInstant()),
                Arguments.of(
                        CHILE_DATA_SPRING,
                        ZoneId.of("America/Santiago"),
                        LocalDateTime.of(2019, Month.SEPTEMBER, 8, 3, 0)
                                .atZone(ZoneOffset.UTC)
                                .toInstant()),
                // DST end
                Arguments.of(
                        US_CENTRAL_DATA_FALL,
                        ZoneId.of("US/Central"),
                        LocalDateTime.of(2017, Month.NOVEMBER, 5, 0, 0)
                                .atZone(ZoneOffset.UTC)
                                .toInstant()),
                Arguments.of(
                        CHILE_DATA_FALL,
                        ZoneId.of("America/Santiago"),
                        LocalDateTime.of(2019, Month.APRIL, 6, 2, 0)
                                .atZone(ZoneOffset.UTC)
                                .toInstant()));
    }

    @ParameterizedTest
    @MethodSource
    void testDaylightSaving(String[][] expectedFormattedDateTimes, ZoneId localZone, Instant startTime)
            throws Exception {
        DateTimeFormatter local = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS")
                .withLocale(Locale.ROOT)
                .withZone(localZone);
        DateTimeFormatter utc = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS")
                .withLocale(Locale.ROOT)
                .withZone(ZoneOffset.UTC);

        FixedDateFormat fixedLocal = FixedDateFormat.create(DEFAULT, TimeZone.getTimeZone(localZone));
        FixedDateFormat fixedUtc = FixedDateFormat.create(DEFAULT, TimeZone.getTimeZone(ZoneOffset.UTC));

        Instant currentTime = startTime;
        for (String[] localAndZuluDateTime : expectedFormattedDateTimes) {
            String expectedLocal = localAndZuluDateTime[0];
            String expectedUtc = localAndZuluDateTime[1];
            assertThat(local.format(currentTime))
                    .as("Local time formatted with DateTimeFormatter")
                    .isEqualTo(expectedLocal);
            assertThat(utc.format(currentTime))
                    .as("Zulu time formatted with DateTimeFormatter")
                    .isEqualTo(expectedUtc);
            assertThat(fixedLocal.format(currentTime.toEpochMilli()))
                    .as("Local time formatted with FixedDateFormat")
                    .isEqualTo(expectedLocal);
            assertThat(fixedUtc.format(currentTime.toEpochMilli()))
                    .as("Zulu time formatted with FixedDateFormat")
                    .isEqualTo(expectedUtc);
            currentTime = currentTime.plus(1, ChronoUnit.HOURS);
        }
    }

    @Test
    void testFixedFormat_getDatePatternLengthReturnsDatePatternLength() {
        assertEquals("yyyyMMdd".length(), FixedFormat.COMPACT.getDatePatternLength());
        assertEquals("yyyy-MM-dd ".length(), DEFAULT.getDatePatternLength());
    }

    @Test
    void testFixedFormat_getDatePatternLengthZeroIfNoDateInPattern() {
        assertEquals(0, FixedFormat.ABSOLUTE.getDatePatternLength());
        assertEquals(0, FixedFormat.ABSOLUTE_PERIOD.getDatePatternLength());
    }

    @Test
    void testFixedFormat_getDatePatternNullIfNoDateInPattern() {
        assertNull(FixedFormat.ABSOLUTE.getDatePattern());
        assertNull(FixedFormat.ABSOLUTE_PERIOD.getDatePattern());
    }

    @Test
    void testFixedFormat_getDatePatternReturnsDatePatternIfExists() {
        assertEquals("yyyyMMdd", FixedFormat.COMPACT.getDatePattern());
        assertEquals("yyyy-MM-dd ", DEFAULT.getDatePattern());
    }

    @Test
    void testFixedFormat_getFastDateFormatNonNullIfDateInPattern() {
        assertNotNull(FixedFormat.COMPACT.getFastDateFormat());
        assertNotNull(DEFAULT.getFastDateFormat());
        assertEquals("yyyyMMdd", FixedFormat.COMPACT.getFastDateFormat().getPattern());
        assertEquals("yyyy-MM-dd ", DEFAULT.getFastDateFormat().getPattern());
    }

    @Test
    void testFixedFormat_getFastDateFormatNullIfNoDateInPattern() {
        assertNull(FixedFormat.ABSOLUTE.getFastDateFormat());
        assertNull(FixedFormat.ABSOLUTE_PERIOD.getFastDateFormat());
    }

    @Test
    void testFormatLong() {
        final long now = System.currentTimeMillis();
        final long start = now - TimeUnit.HOURS.toMillis(25);
        final long end = now + TimeUnit.HOURS.toMillis(25);
        for (final FixedFormat format : FixedFormat.values()) {
            final String pattern = format.getPattern();
            if (containsNanos(format) || format.getFixedTimeZoneFormat() != null) {
                continue; // cannot compile precise timestamp formats with SimpleDateFormat
            }
            final SimpleDateFormat simpleDF = new SimpleDateFormat(pattern, Locale.getDefault());
            final FixedDateFormat customTF = new FixedDateFormat(format, TimeZone.getDefault());
            for (long time = start; time < end; time += 12345) {
                final String actual = customTF.format(time);
                final String expected = simpleDF.format(new Date(time));
                assertEquals(expected, actual, format + "(" + pattern + ")" + "/" + time);
            }
        }
    }

    @Test
    void testFormatLong_goingBackInTime() {
        final long now = System.currentTimeMillis();
        final long start = now - TimeUnit.HOURS.toMillis(25);
        final long end = now + TimeUnit.HOURS.toMillis(25);
        for (final FixedFormat format : FixedFormat.values()) {
            final String pattern = format.getPattern();
            if (containsNanos(format) || format.getFixedTimeZoneFormat() != null) {
                continue; // cannot compile precise timestamp formats with SimpleDateFormat
            }
            final SimpleDateFormat simpleDF = new SimpleDateFormat(pattern, Locale.getDefault());
            final FixedDateFormat customTF = new FixedDateFormat(format, TimeZone.getDefault());
            for (long time = end; time > start; time -= 12345) {
                final String actual = customTF.format(time);
                final String expected = simpleDF.format(new Date(time));
                assertEquals(expected, actual, format + "(" + pattern + ")" + "/" + time);
            }
        }
    }

    /**
     * This test case validates date pattern before and after DST
     * Base Date : 12 Mar 2017
     * Daylight Savings started on : 02:00 AM
     */
    @Test
    void testFormatLong_goingBackInTime_DST() {
        final Calendar instance = Calendar.getInstance(TimeZone.getTimeZone("EST"));
        instance.set(2017, 2, 12, 2, 0);
        final long now = instance.getTimeInMillis();
        final long start = now - TimeUnit.HOURS.toMillis(1);
        final long end = now + TimeUnit.HOURS.toMillis(1);

        for (final FixedFormat format : FixedFormat.values()) {
            final String pattern = format.getPattern();
            if (containsNanos(format) || format.getFixedTimeZoneFormat() != null) {
                continue; // cannot compile precise timestamp formats with SimpleDateFormat
            }
            final SimpleDateFormat simpleDF = new SimpleDateFormat(pattern, Locale.getDefault());
            final FixedDateFormat customTF = new FixedDateFormat(format, TimeZone.getDefault());
            for (long time = end; time > start; time -= 12345) {
                final String actual = customTF.format(time);
                final String expected = simpleDF.format(new Date(time));
                assertEquals(expected, actual, format + "(" + pattern + ")" + "/" + time);
            }
        }
    }

    @Test
    void testFormatLongCharArrayInt() {
        final long now = System.currentTimeMillis();
        final long start = now - TimeUnit.HOURS.toMillis(25);
        final long end = now + TimeUnit.HOURS.toMillis(25);
        final char[] buffer = new char[128];
        for (final FixedFormat format : FixedFormat.values()) {
            final String pattern = format.getPattern();
            if (containsNanos(format) || format.getFixedTimeZoneFormat() != null) {
                // cannot compile precise timestamp formats with SimpleDateFormat
                // This format() API not include the TZ
                continue;
            }
            final SimpleDateFormat simpleDF = new SimpleDateFormat(pattern, Locale.getDefault());
            final FixedDateFormat customTF = new FixedDateFormat(format, TimeZone.getDefault());
            for (long time = start; time < end; time += 12345) {
                final int length = customTF.format(time, buffer, 23);
                final String actual = new String(buffer, 23, length);
                final String expected = simpleDF.format(new Date(time));
                assertEquals(expected, actual, format + "(" + pattern + ")" + "/" + time);
            }
        }
    }

    @Test
    void testFormatLongCharArrayInt_goingBackInTime() {
        final long now = System.currentTimeMillis();
        final long start = now - TimeUnit.HOURS.toMillis(25);
        final long end = now + TimeUnit.HOURS.toMillis(25);
        final char[] buffer = new char[128];
        for (final FixedFormat format : FixedFormat.values()) {
            final String pattern = format.getPattern();
            if (pattern.endsWith("n")
                    || pattern.matches(".+n+X*")
                    || pattern.matches(".+n+Z*")
                    || format.getFixedTimeZoneFormat() != null) {
                continue; // cannot compile precise timestamp formats with SimpleDateFormat
            }
            final SimpleDateFormat simpleDF = new SimpleDateFormat(pattern, Locale.getDefault());
            final FixedDateFormat customTF = new FixedDateFormat(format, TimeZone.getDefault());
            for (long time = end; time > start; time -= 12345) {
                final int length = customTF.format(time, buffer, 23);
                final String actual = new String(buffer, 23, length);
                final String expected = simpleDF.format(new Date(time));
                assertEquals(expected, actual, format + "(" + pattern + ")" + "/" + time);
            }
        }
    }

    @Test
    void testGetFormatReturnsConstructorFixedFormatPattern() {
        final FixedDateFormat format = new FixedDateFormat(FixedDateFormat.FixedFormat.ABSOLUTE, TimeZone.getDefault());
        assertSame(FixedDateFormat.FixedFormat.ABSOLUTE.getPattern(), format.getFormat());
    }

    @ParameterizedTest
    @MethodSource("org.apache.logging.log4j.core.util.datetime.FixedDateFormat$FixedFormat#values")
    @DefaultLocale(language = "en")
    void testFixedFormatLength(FixedFormat format) {
        LocalDate date = LocalDate.of(2023, 4, 8);
        LocalTime time = LocalTime.of(19, 5, 14);
        ZoneId zone = ZoneId.of("Europe/Warsaw");
        long epochMillis = ZonedDateTime.of(date, time, zone).toInstant().toEpochMilli();
        MutableInstant instant = new MutableInstant();
        instant.initFromEpochMilli(epochMillis, 123_456);
        FixedDateFormat formatter = FixedDateFormat.create(format);

        String formatted = formatter.formatInstant(instant);
        assertEquals(formatter.getLength(), formatted.length(), formatted);
    }
}
