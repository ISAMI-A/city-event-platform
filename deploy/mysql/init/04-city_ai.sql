USE city_ai;

CREATE TABLE IF NOT EXISTS `t_ai_call_log`
(
    `id`                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `trace_id`          VARCHAR(64)     NULL,
    `biz_type`          VARCHAR(32)     NOT NULL DEFAULT 'EVENT_ANALYZE',
    `user_id`           BIGINT UNSIGNED NULL,
    `model_name`        VARCHAR(64)     NULL,
    `prompt_version`    VARCHAR(16)     NULL,
    `request_summary`   VARCHAR(500)    NULL,
    `response_summary`  VARCHAR(1000)   NULL,
    `parsed_ok`         TINYINT         NOT NULL DEFAULT 0,
    `validation_result` VARCHAR(255)    NULL,
    `error_code`        VARCHAR(32)     NULL,
    `latency_ms`        INT             NULL,
    `prompt_tokens`     INT             NULL,
    `completion_tokens` INT             NULL,
    `create_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_ai_trace` (`trace_id`),
    KEY `idx_ai_create` (`create_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='大模型调用日志';

CREATE TABLE IF NOT EXISTS `t_ai_prompt_template`
(
    `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `template_code` VARCHAR(64)     NOT NULL,
    `version`       VARCHAR(16)     NOT NULL,
    `system_prompt` TEXT            NOT NULL,
    `user_prompt`   TEXT            NOT NULL,
    `enabled`       TINYINT         NOT NULL DEFAULT 1,
    `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_prompt_code` (`template_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='提示词模板';
