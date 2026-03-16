package com.example.dhan_rsi_series.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class CorrelationIdGenerator {

    public static String generate(String strategy) {

        String time = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        String random = UUID.randomUUID()
                .toString()
                .substring(0, 5);

        String id = strategy + "_" + time + "_" + random;

        return id.substring(0, Math.min(id.length(), 30));
    }
}