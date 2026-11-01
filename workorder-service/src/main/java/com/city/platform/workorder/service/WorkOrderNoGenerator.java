package com.city.platform.workorder.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 工单编号：WO + yyyyMMdd + 4 位序号。
 */
@Component
public class WorkOrderNoGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StringRedisTemplate redisTemplate;

    public WorkOrderNoGenerator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String next() {
        String date = LocalDate.now().format(DATE_FORMAT);
        String key = "city:wo:seq:" + date;
        Long seq = redisTemplate.opsForValue().increment(key);
        if (seq != null && seq == 1L) {
            redisTemplate.expire(key, Duration.ofDays(2));
        }
        long value = seq == null ? System.currentTimeMillis() % 10000 : seq;
        return "WO" + date + String.format("%04d", value);
    }
}
