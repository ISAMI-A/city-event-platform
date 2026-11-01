package com.city.platform.map.controller;

import com.city.platform.common.api.dto.GeoResult;
import com.city.platform.common.core.model.R;
import com.city.platform.map.service.TencentMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/internal/v1/map")
@RequiredArgsConstructor
public class InternalMapController {

    private final TencentMapService tencentMapService;

    @GetMapping("/reverse-geocode")
    public R<GeoResult> reverseGeocode(@RequestParam("lng") BigDecimal lng,
                                       @RequestParam("lat") BigDecimal lat,
                                       @RequestParam(value = "address", required = false) String address) {
        return R.ok(tencentMapService.reverseGeocode(lng, lat, address));
    }
}
