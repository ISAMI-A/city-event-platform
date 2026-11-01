USE city_map;

CREATE TABLE IF NOT EXISTS `t_area`
(
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `area_code`   VARCHAR(32)     NOT NULL,
    `area_name`   VARCHAR(64)     NOT NULL,
    `parent_code` VARCHAR(32)     NULL,
    `level`       VARCHAR(16)     NOT NULL COMMENT 'PROVINCE/CITY/DISTRICT/STREET',
    `center_lng`  DECIMAL(10, 6)  NULL,
    `center_lat`  DECIMAL(10, 6)  NULL,
    `enabled`     TINYINT         NOT NULL DEFAULT 1,
    `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_area_code` (`area_code`),
    KEY `idx_area_parent` (`parent_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='行政区与区域字典';

CREATE TABLE IF NOT EXISTS `t_map_call_log`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `trace_id`      VARCHAR(64)     NULL,
    `call_type`     VARCHAR(32)     NOT NULL COMMENT 'REVERSE_GEOCODER/PLACE_SEARCH',
    `request_param` VARCHAR(500)    NULL,
    `success`       TINYINT         NOT NULL DEFAULT 0,
    `error_code`    VARCHAR(32)     NULL,
    `latency_ms`    INT             NULL,
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_map_create` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='地图接口调用日志';
