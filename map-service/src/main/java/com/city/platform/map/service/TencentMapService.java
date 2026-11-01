package com.city.platform.map.service;

import com.city.platform.common.api.dto.GeoResult;
import com.city.platform.common.core.web.TraceIdUtils;
import com.city.platform.map.config.MapProperties;
import com.city.platform.map.entity.MapCallLog;
import com.city.platform.map.mapper.MapCallLogMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 腾讯位置服务逆地址解析：缓存 + 超时 + 失败降级，不阻断事件上报。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TencentMapService {

    private static final String CACHE_PREFIX = "city:map:geo:";
    private static final Duration CACHE_TTL = Duration.ofDays(7);

    private final RestClient mapRestClient;
    private final MapProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final MapCallLogMapper mapCallLogMapper;
    private final ObjectMapper objectMapper;

    public GeoResult reverseGeocode(BigDecimal lng, BigDecimal lat, String fallbackAddress) {
        if (lng == null || lat == null) {
            return pending(fallbackAddress, "INVALID_COORDINATE");
        }
        if (lng.doubleValue() < -180 || lng.doubleValue() > 180
                || lat.doubleValue() < -90 || lat.doubleValue() > 90) {
            log.warn("无效坐标 lng={} lat={}", lng, lat);
            return pending(fallbackAddress, "INVALID_COORDINATE");
        }
        String cacheKey = CACHE_PREFIX + lng.setScale(4, RoundingMode.HALF_UP).toPlainString()
                + "," + lat.setScale(4, RoundingMode.HALF_UP).toPlainString();
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (StringUtils.hasText(cached)) {
                return objectMapper.readValue(cached, GeoResult.class);
            }
        } catch (Exception e) {
            log.warn("地图缓存读取失败，继续调用外部接口: {}", e.getMessage());
        }

        long start = System.currentTimeMillis();
        String errorCode = null;
        try {
            if (!StringUtils.hasText(properties.getKey())) {
                return pending(fallbackAddress, "MAP_KEY_MISSING");
            }
            String location = lat.toPlainString() + "," + lng.toPlainString();
            String sig = sign("/ws/geocoder/v1/", location);
            JsonNode response = mapRestClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/ws/geocoder/v1/")
                                .queryParam("location", location)
                                .queryParam("key", properties.getKey())
                                .queryParam("get_poi", 0);
                        if (sig != null) {
                            uriBuilder.queryParam("sig", sig);
                        }
                        return uriBuilder.build();
                    })
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                return pending(fallbackAddress, "EMPTY_RESPONSE");
            }
            int status = response.path("status").asInt(-1);
            if (status != 0) {
                errorCode = "STATUS_" + status;
                log.warn("腾讯地图返回异常 status={} message={}", status, response.path("message").asText());
                return pending(fallbackAddress, errorCode);
            }
            JsonNode result = response.path("result");
            JsonNode component = result.path("address_component");
            GeoResult geo = new GeoResult(
                    component.path("province").asText(null),
                    component.path("city").asText(null),
                    component.path("district").asText(null),
                    component.path("street").asText(null),
                    result.path("ad_info").path("adcode").asText(null),
                    result.path("address").asText(null),
                    result.path("formatted_addresses").path("recommend").asText(null),
                    false);
            cache(cacheKey, geo);
            return geo;
        } catch (Exception e) {
            log.warn("腾讯地图调用失败: {}", e.getMessage());
            return pending(fallbackAddress, "CALL_FAILED");
        } finally {
            saveLog(lng, lat, errorCode == null, errorCode,
                    (int) (System.currentTimeMillis() - start));
        }
    }

    /**
     * 腾讯 WebServiceAPI 签名：请求路径 + "?" + 参数名升序拼接的原始参数 + SK，取 MD5 小写。
     */
    private String sign(String path, String location) {
        if (!StringUtils.hasText(properties.getSk())) {
            return null;
        }
        Map<String, String> params = new LinkedHashMap<>();
        params.put("key", properties.getKey());
        params.put("location", location);
        params.put("get_poi", "0");
        String query = params.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        String raw = path + "?" + query + properties.getSk();
        return DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    private void cache(String key, GeoResult geo) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(geo), CACHE_TTL);
        } catch (Exception e) {
            log.warn("地图结果缓存写入失败: {}", e.getMessage());
        }
    }

    private GeoResult pending(String fallbackAddress, String reason) {
        GeoResult geo = new GeoResult();
        geo.setAddress(fallbackAddress);
        geo.setPending(true);
        log.info("地图解析降级 reason={} address={}", reason, fallbackAddress);
        return geo;
    }

    private void saveLog(BigDecimal lng, BigDecimal lat, boolean success, String errorCode, int latency) {
        try {
            MapCallLog logRecord = new MapCallLog();
            logRecord.setTraceId(TraceIdUtils.getTraceId());
            logRecord.setCallType("REVERSE_GEOCODER");
            logRecord.setRequestParam("lng=" + lng + ",lat=" + lat);
            logRecord.setSuccess(success ? 1 : 0);
            logRecord.setErrorCode(errorCode);
            logRecord.setLatencyMs(latency);
            mapCallLogMapper.insert(logRecord);
        } catch (Exception e) {
            log.warn("地图调用日志写入失败: {}", e.getMessage());
        }
    }
}
