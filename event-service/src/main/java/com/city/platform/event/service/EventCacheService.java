package com.city.platform.event.service;

import com.city.platform.event.vo.EventDetailVO;
import com.city.platform.event.vo.EventStatVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 事件详情与统计缓存（Cache-Aside）。
 * Key 设计：
 * - city:event:detail:{id}  详情，TTL 10 分钟 + 0-60 秒随机抖动（防雪崩）
 * - city:event:null:{id}    空结果，TTL 60 秒（防穿透）
 * - city:event:stat:overview 统计，TTL 60 秒
 * 一致性：事件创建、审核、驳回、工单状态同步后主动删除详情与统计缓存，TTL 作为兜底。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventCacheService {

    private static final String DETAIL_PREFIX = "city:event:detail:";
    private static final String NULL_PREFIX = "city:event:null:";
    private static final String STAT_KEY = "city:event:stat:overview";

    private static final Duration DETAIL_TTL = Duration.ofMinutes(10);
    private static final Duration NULL_TTL = Duration.ofSeconds(60);
    private static final Duration STAT_TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public EventDetailVO getDetail(Long id) {
        try {
            String value = redisTemplate.opsForValue().get(DETAIL_PREFIX + id);
            if (value == null) {
                return null;
            }
            return objectMapper.readValue(value, EventDetailVO.class);
        } catch (Exception e) {
            log.warn("事件详情缓存读取失败 id={}: {}", id, e.getMessage());
            return null;
        }
    }

    public void putDetail(Long id, EventDetailVO detail) {
        try {
            long jitter = ThreadLocalRandom.current().nextLong(0, 61);
            redisTemplate.opsForValue().set(DETAIL_PREFIX + id,
                    objectMapper.writeValueAsString(detail), DETAIL_TTL.plusSeconds(jitter));
        } catch (Exception e) {
            log.warn("事件详情缓存写入失败 id={}: {}", id, e.getMessage());
        }
    }

    public boolean isNullCached(Long id) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(NULL_PREFIX + id));
        } catch (Exception e) {
            return false;
        }
    }

    public void putNull(Long id) {
        try {
            redisTemplate.opsForValue().set(NULL_PREFIX + id, "1", NULL_TTL);
        } catch (Exception e) {
            log.warn("空值缓存写入失败 id={}: {}", id, e.getMessage());
        }
    }

    public void evictDetail(Long id) {
        try {
            redisTemplate.delete(DETAIL_PREFIX + id);
        } catch (Exception e) {
            log.warn("事件详情缓存删除失败 id={}: {}", id, e.getMessage());
        }
    }

    public EventStatVO getStat() {
        try {
            String value = redisTemplate.opsForValue().get(STAT_KEY);
            return value == null ? null : objectMapper.readValue(value, EventStatVO.class);
        } catch (Exception e) {
            log.warn("统计缓存读取失败: {}", e.getMessage());
            return null;
        }
    }

    public void putStat(EventStatVO stat) {
        try {
            redisTemplate.opsForValue().set(STAT_KEY, objectMapper.writeValueAsString(stat), STAT_TTL);
        } catch (Exception e) {
            log.warn("统计缓存写入失败: {}", e.getMessage());
        }
    }

    public void evictStat() {
        try {
            redisTemplate.delete(STAT_KEY);
        } catch (Exception e) {
            log.warn("统计缓存删除失败: {}", e.getMessage());
        }
    }

    /**
     * 事件状态或内容变化时统一失效。
     */
    public void evictAfterChange(Long id) {
        evictDetail(id);
        evictStat();
    }
}
