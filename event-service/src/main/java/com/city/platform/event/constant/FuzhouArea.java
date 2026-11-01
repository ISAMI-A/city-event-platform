package com.city.platform.event.constant;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 福州市六城区范围：事件上报只允许落在鼓楼、台江、仓山、马尾、晋安、长乐。
 * 行政区代码优先校验，逆地址解析不可用时用经纬度外包框兜底。
 */
public final class FuzhouArea {

    public static final Map<String, String> DISTRICTS = Map.of(
            "350102", "鼓楼区",
            "350103", "台江区",
            "350104", "仓山区",
            "350105", "马尾区",
            "350111", "晋安区",
            "350112", "长乐区");

    /** 六城区经纬度外包框（GCJ-02） */
    private static final BigDecimal MIN_LNG = new BigDecimal("118.35");
    private static final BigDecimal MAX_LNG = new BigDecimal("119.95");
    private static final BigDecimal MIN_LAT = new BigDecimal("25.75");
    private static final BigDecimal MAX_LAT = new BigDecimal("26.35");

    private FuzhouArea() {
    }

    public static boolean isDistrict(String areaCode) {
        return areaCode != null && DISTRICTS.containsKey(areaCode);
    }

    /**
     * 逆地址解析失败时，用地址文本里的区名兜底识别片区，避免事件缺少片区无法匹配组长。
     */
    public static String resolveByText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        for (Map.Entry<String, String> entry : DISTRICTS.entrySet()) {
            if (text.contains(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static boolean inBounds(BigDecimal longitude, BigDecimal latitude) {
        return longitude != null && latitude != null
                && longitude.compareTo(MIN_LNG) >= 0 && longitude.compareTo(MAX_LNG) <= 0
                && latitude.compareTo(MIN_LAT) >= 0 && latitude.compareTo(MAX_LAT) <= 0;
    }
}
