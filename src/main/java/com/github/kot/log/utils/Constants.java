package com.github.kot.log.utils;

import java.time.format.DateTimeFormatter;

public class Constants {

    public static final DateTimeFormatter DATE_TIME_FORMATTER_DB = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSS XXX");
    public static final DateTimeFormatter DATE_TIME_FORMATTER_LOG = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm:ss:SSS");

    private Constants() {
    }
}