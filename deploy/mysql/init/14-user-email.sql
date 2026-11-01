SET NAMES utf8mb4;

-- ============================================================
-- 账号体系补全：邮箱绑定 + 存量微信账号补登录账号
-- 仅需执行一次（新环境由 init 顺序自动执行）
-- ============================================================

USE city_user;

ALTER TABLE `t_user`
    ADD COLUMN `email` VARCHAR(128) NULL COMMENT '绑定邮箱' AFTER `phone`,
    ADD COLUMN `email_verified` TINYINT NOT NULL DEFAULT 0 COMMENT '邮箱是否已验证' AFTER `email`,
    ADD UNIQUE KEY `uk_user_email` (`email`);

-- 历史微信注册账号没有 username，个人页无法展示登录账号；按 id 补齐唯一账号
UPDATE `t_user`
SET `username` = CONCAT('wx_', LPAD(`id`, 6, '0'))
WHERE `username` IS NULL
  AND `open_id` IS NOT NULL;
