package com.example.dhan_rsi_series.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class DateTimeUtil {

    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    public static LocalDateTime toIstDateTime(Long epochSeconds) {
        if (epochSeconds == null) return null;

        return Instant.ofEpochSecond(epochSeconds)
                .atZone(IST_ZONE)
                .toLocalDateTime();
    }
}
