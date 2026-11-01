-- P5 收尾：工单 AI 整改核实结果（视觉模型）
USE city_workorder;

ALTER TABLE `t_work_order`
    ADD COLUMN `ai_verify_result` TEXT NULL COMMENT 'AI整改核实结果(JSON)',
    ADD COLUMN `ai_verify_time` DATETIME NULL COMMENT 'AI核实时间';
