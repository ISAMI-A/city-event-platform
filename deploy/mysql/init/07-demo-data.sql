SET NAMES utf8mb4;

-- P3 测试数据（仅开发/演示环境执行）
USE city_user;

-- 演示居民账号 resident_demo / Worker@123（与处理人员同密码哈希，便于联调）
INSERT IGNORE INTO `t_user` (`id`, `username`, `password_hash`, `nickname`, `status`)
VALUES (100, 'resident_demo', '$2a$10$5kOPPM5Z9jgccQoEkHlkoOLTzbT8nGorVXcKsoXA1dN0NBsa/Rxb.', '演示居民', 'ENABLED');

INSERT IGNORE INTO `t_user_role` (`user_id`, `role_code`) VALUES (100, 'RESIDENT');

-- 第二个居民，用于验证"居民不得查看他人事件"
INSERT IGNORE INTO `t_user` (`id`, `username`, `password_hash`, `nickname`, `status`)
VALUES (101, 'resident_other', '$2a$10$5kOPPM5Z9jgccQoEkHlkoOLTzbT8nGorVXcKsoXA1dN0NBsa/Rxb.', '其他居民', 'ENABLED');

INSERT IGNORE INTO `t_user_role` (`user_id`, `role_code`) VALUES (101, 'RESIDENT');
