package com.samaksh.farms.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class BusinessTimezoneConfig {

    @PostConstruct
    public void setBusinessTimezone() {
        TimeZone.setDefault(
                TimeZone.getTimeZone("Asia/Kolkata")
        );
    }
}
