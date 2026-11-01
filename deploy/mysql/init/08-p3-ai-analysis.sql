-- P3 迁移：AI 分析结果快照表（与调用日志分离，供事件服务通过内部接口读取）
USE city_ai;

CREATE TABLE IF NOT EXISTS `t_ai_analysis`
(
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `call_log_id`    BIGINT UNSIGNED NOT NULL COMMENT '关联 t_ai_call_log.id',
    `category_code`  VARCHAR(32)     NOT NULL COMMENT '校验后的类别',
    `summary`        VARCHAR(255)    NULL,
    `priority`       VARCHAR(16)     NOT NULL DEFAULT 'MEDIUM',
    `suggestion`     VARCHAR(600)    NULL,
    `reasoning`      VARCHAR(600)    NULL COMMENT '模型判断理由',
    `confidence`     DECIMAL(4, 3)   NOT NULL DEFAULT 0,
    `degraded`       TINYINT         NOT NULL DEFAULT 0,
    `degrade_reason` VARCHAR(64)     NULL,
    `model_name`     VARCHAR(64)     NULL,
    `expire_time`    DATETIME        NOT NULL COMMENT '调用结果有效期',
    `create_time`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ai_analysis_call` (`call_log_id`),
    KEY `idx_ai_analysis_expire` (`expire_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='AI 分析结果快照';
