package com.city.platform.common.api.feign;

import com.city.platform.common.api.dto.GeoResult;
import com.city.platform.common.core.model.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/**
 * map-service 内部逆地址解析契约。
 */
@FeignClient(name = "map-service", contextId = "mapClient", path = "/internal/v1/map",
        url = "${city.feign.map-service-url:}")
public interface MapClient {

    @GetMapping("/reverse-geocode")
    R<GeoResult> reverseGeocode(@RequestParam("lng") BigDecimal lng,
                                @RequestParam("lat") BigDecimal lat,
                                @RequestParam(value = "address", required = false) String address);
}
