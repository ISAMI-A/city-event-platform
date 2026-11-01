package com.city.platform.map.controller;

import com.city.platform.common.api.dto.GeoResult;
import com.city.platform.common.core.model.R;
import com.city.platform.map.config.MapProperties;
import com.city.platform.map.service.TencentMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/map")
@RequiredArgsConstructor
public class MapController {

    private final TencentMapService tencentMapService;
    private final MapProperties mapProperties;

    @GetMapping("/reverse-geocode")
    public R<GeoResult> reverseGeocode(@RequestParam("lng") BigDecimal lng,
                                       @RequestParam("lat") BigDecimal lat,
                                       @RequestParam(value = "address", required = false) String address) {
        return R.ok(tencentMapService.reverseGeocode(lng, lat, address));
    }

    /**
     * 前端地图初始化配置：只返回 Web 端受限 Key，不返回服务端 WebService Key。
     */
    @GetMapping("/config")
    public R<Map<String, Object>> config() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("provider", "tencent");
        config.put("coordinateSystem", "gcj02");
        config.put("jsApiKey", mapProperties.getJsApiKey());
        config.put("jsApiEnabled", !mapProperties.getJsApiKey().isBlank());
        return R.ok(config);
    }
}
