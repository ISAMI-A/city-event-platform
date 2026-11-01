package com.city.platform.event.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * 事件编号：EV + yyyyMMdd + 4 位序号，使用 Redis 原子自增。
 */
@Component
public class EventNoGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StringRedisTemplate redisTemplate;

    public EventNoGenerator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String next() {
        String date = LocalDate.now().format(DATE_FORMAT);
        String key = "city:event:seq:" + date;
        Long seq = redisTemplate.opsForValue().increment(key);
        if (seq != null && seq == 1L) {
            redisTemplate.expire(key, Duration.ofDays(2));
        }
        long value = seq == null ? System.currentTimeMillis() % 10000 : seq;
        return "EV" + date + String.format("%04d", value);
    }
}
