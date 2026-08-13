package com.samaksh.farms.common.time;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class BusinessTime {

    public static final ZoneId ZONE_ID =
            ZoneId.of("Asia/Kolkata");

    private BusinessTime() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE_ID);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE_ID);
    }
}
