-- P4 迁移：AI 分析增加判断理由字段
USE city_ai;

ALTER TABLE `t_ai_analysis`
    ADD COLUMN `reasoning` VARCHAR(600) NULL COMMENT '模型判断理由' AFTER `suggestion`;
