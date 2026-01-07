package com.problemio.global.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class TimeUtils {
    public static final ZoneId ZONE_KST = ZoneId.of("Asia/Seoul");

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE_KST);
    }
}
