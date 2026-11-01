SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 演示数据重置脚本（可重复执行）
-- 执行方式（容器内已挂载 deploy/mysql/init）：
--   docker exec city-mysql mysql -uroot -p1234 --default-character-set=utf8mb4 \
--     -e "source /docker-entrypoint-initdb.d/demo/reset-demo-data.sql"
-- 说明：
--   1. 清空 4 个业务库的全部业务数据（保留角色字典），重建完整演示数据；
--   2. 组织架构为「管理员 → 4 名组长 → 每名组长 3 名组员」，管理员只派单给组长；
--   3. 事件全部落在福州市六城区，含 AI 紧急度复评结果；
--   4. 工单覆盖待接单、处理中、待组长复核、待验收、已完成、居民评价等状态；
--   5. 图片由 scripts/reset-demo.ps1 复制到 data/uploads/demo/，本脚本写入附件元数据。
-- ============================================================

-- ---------- 清空 ----------
USE city_user;
TRUNCATE TABLE `t_user`;
TRUNCATE TABLE `t_user_role`;
TRUNCATE TABLE `t_worker_profile`;
TRUNCATE TABLE `t_recruitment`;
TRUNCATE TABLE `t_recruitment_apply`;

USE city_event;
TRUNCATE TABLE `t_event`;
TRUNCATE TABLE `t_attachment`;
TRUNCATE TABLE `t_event_status_log`;
TRUNCATE TABLE `t_event_audit_log`;
TRUNCATE TABLE `t_event_ai_snapshot`;
TRUNCATE TABLE `t_notification`;

USE city_workorder;
TRUNCATE TABLE `t_work_order`;
TRUNCATE TABLE `t_work_order_log`;
TRUNCATE TABLE `t_work_order_acceptance`;
TRUNCATE TABLE `t_work_order_reassign`;
TRUNCATE TABLE `t_work_rating`;
TRUNCATE TABLE `undo_log`;

USE city_ai;
TRUNCATE TABLE `t_ai_call_log`;
TRUNCATE TABLE `t_ai_analysis`;

-- ---------- 角色字典 ----------
USE city_user;
INSERT INTO `t_role` (`role_code`, `role_name`, `description`)
VALUES ('RESIDENT', '居民', '上报事件与查看进度'),
       ('WORKER', '处理人员', '接单与提交处理结果'),
       ('ADMIN', '管理员', '审核、派单、验收与用户管理')
ON DUPLICATE KEY UPDATE `role_name` = VALUES(`role_name`), `description` = VALUES(`description`);

-- ---------- 账号 ----------
-- admin / Admin@123
INSERT INTO `t_user` (`id`, `username`, `password_hash`, `nickname`, `phone`, `status`)
VALUES (1, 'admin', '$2a$10$PUNtUCz0kNwn/rHVGtYjs.JHravHWv292p9/ZvzBpS0jumIRmzxMS', '系统管理员', '13800100001', 'ENABLED');

-- 组长 / Worker@123
INSERT INTO `t_user` (`id`, `username`, `password_hash`, `nickname`, `phone`, `status`)
VALUES (2, 'worker_road_leader', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '李道路', '13800100002', 'ENABLED'),
       (3, 'worker_lighting_leader', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '王照明', '13800100003', 'ENABLED'),
       (4, 'worker_sanitation_leader', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '张环卫', '13800100004', 'ENABLED'),
       (5, 'worker_facility_leader', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '赵设施', '13800100005', 'ENABLED');

-- 组员 / Worker@123
INSERT INTO `t_user` (`id`, `username`, `password_hash`, `nickname`, `phone`, `status`)
VALUES (21, 'worker_road_01', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '陈路桥', '13800100021', 'ENABLED'),
       (22, 'worker_road_02', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '刘井盖', '13800100022', 'ENABLED'),
       (23, 'worker_road_03', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '周护栏', '13800100023', 'ENABLED'),
       (24, 'worker_lighting_01', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '吴灯杆', '13800100024', 'ENABLED'),
       (25, 'worker_lighting_02', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '郑线路', '13800100025', 'ENABLED'),
       (26, 'worker_lighting_03', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '孙巡检', '13800100026', 'OFF_DUTY'),
       (27, 'worker_sanitation_01', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '冯清运', '13800100027', 'ENABLED'),
       (28, 'worker_sanitation_02', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '褚保洁', '13800100028', 'ENABLED'),
       (29, 'worker_sanitation_03', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '卫冲洗', '13800100029', 'ENABLED'),
       (30, 'worker_facility_01', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '蒋排水', '13800100030', 'ENABLED'),
       (31, 'worker_facility_02', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '沈栈道', '13800100031', 'ENABLED'),
       (32, 'worker_facility_03', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '韩设施', '13800100032', 'ENABLED');

-- 居民 / Resident@123
INSERT INTO `t_user` (`id`, `username`, `password_hash`, `nickname`, `phone`, `status`)
VALUES (11, 'resident_fuzhou', '$2a$10$g5B4MmjqJhP3qo319D3F9uOVtW0bR.bAZDRRWyve.eXE6SlK.6hQG', '林晓榕（鼓楼）', '13800100011', 'ENABLED'),
       (12, 'resident_taijiang', '$2a$10$g5B4MmjqJhP3qo319D3F9uOVtW0bR.bAZDRRWyve.eXE6SlK.6hQG', '陈鹭（台江）', '13800100012', 'ENABLED'),
       (13, 'resident_cangshan', '$2a$10$g5B4MmjqJhP3qo319D3F9uOVtW0bR.bAZDRRWyve.eXE6SlK.6hQG', '黄刺桐（仓山）', '13800100013', 'ENABLED'),
       (14, 'resident_mawei', '$2a$10$g5B4MmjqJhP3qo319D3F9uOVtW0bR.bAZDRRWyve.eXE6SlK.6hQG', '吴水仙（马尾）', '13800100014', 'ENABLED'),
       (15, 'resident_jinan', '$2a$10$g5B4MmjqJhP3qo319D3F9uOVtW0bR.bAZDRRWyve.eXE6SlK.6hQG', '郑木兰（晋安）', '13800100015', 'ENABLED'),
       (16, 'resident_test', '$2a$10$g5B4MmjqJhP3qo319D3F9uOVtW0bR.bAZDRRWyve.eXE6SlK.6hQG', '测试居民（停用）', '13800100016', 'DISABLED');

INSERT INTO `t_user_role` (`user_id`, `role_code`)
VALUES (1, 'ADMIN'),
       (2, 'WORKER'), (3, 'WORKER'), (4, 'WORKER'), (5, 'WORKER'),
       (21, 'WORKER'), (22, 'WORKER'), (23, 'WORKER'),
       (24, 'WORKER'), (25, 'WORKER'), (26, 'WORKER'),
       (27, 'WORKER'), (28, 'WORKER'), (29, 'WORKER'),
       (30, 'WORKER'), (31, 'WORKER'), (32, 'WORKER'),
       (11, 'RESIDENT'), (12, 'RESIDENT'), (13, 'RESIDENT'), (14, 'RESIDENT'), (15, 'RESIDENT'), (16, 'RESIDENT');

-- ---------- 处理人员档案（4 组长 + 12 组员） ----------
INSERT INTO `t_worker_profile`
(`id`, `user_id`, `worker_type`, `leader_id`, `dept_code`, `dept_name`, `area_code`, `work_zone`, `zone_lng`, `zone_lat`, `skill_tags`, `status`, `create_by`)
VALUES
(1, 2, 'LEADER', NULL, 'ROAD', '市政道路班组', '350111', '晋安区中心片区', 119.331000, 26.089000, '井盖,路面,护栏', 'ON_DUTY', 1),
(2, 3, 'LEADER', NULL, 'LIGHTING', '路灯照明班组', '350102', '鼓楼区中心片区', 119.296500, 26.074500, '路灯,线路,灯杆', 'ON_DUTY', 1),
(3, 4, 'LEADER', NULL, 'SANITATION', '环卫保洁班组', '350104', '仓山区金山片区', 119.273000, 26.030000, '垃圾清运,河道保洁,冲洗', 'ON_DUTY', 1),
(4, 5, 'LEADER', NULL, 'FACILITY', '公共设施班组', '350103', '台江区滨江片区', 119.310000, 26.060000, '排水口,木栈道,公共设施', 'ON_DUTY', 1),
(5, 21, 'MEMBER', 2, 'ROAD', '市政道路班组', '350111', '晋安区鼓山片区', 119.345000, 26.070000, '井盖,路面', 'ON_DUTY', 1),
(6, 22, 'MEMBER', 2, 'ROAD', '市政道路班组', '350111', '晋安区新店片区', 119.318000, 26.120000, '井盖,排水沟', 'ON_DUTY', 1),
(7, 23, 'MEMBER', 2, 'ROAD', '市政道路班组', '350102', '鼓楼区西片区', 119.285000, 26.080000, '护栏,路面', 'ON_DUTY', 1),
(8, 24, 'MEMBER', 3, 'LIGHTING', '路灯照明班组', '350102', '鼓楼区东片区', 119.305000, 26.083000, '路灯,灯杆', 'ON_DUTY', 1),
(9, 25, 'MEMBER', 3, 'LIGHTING', '路灯照明班组', '350103', '台江区北片区', 119.306000, 26.062000, '线路,配电箱', 'ON_DUTY', 1),
(10, 26, 'MEMBER', 3, 'LIGHTING', '路灯照明班组', '350105', '马尾区片区', 119.455000, 25.990000, '路灯巡检', 'OFF_DUTY', 1),
(11, 27, 'MEMBER', 4, 'SANITATION', '环卫保洁班组', '350104', '仓山区金山片区', 119.273000, 26.030000, '垃圾清运,河道保洁', 'ON_DUTY', 1),
(12, 28, 'MEMBER', 4, 'SANITATION', '环卫保洁班组', '350103', '台江区茶亭片区', 119.306000, 26.062000, '垃圾清运,冲洗', 'ON_DUTY', 1),
(13, 29, 'MEMBER', 4, 'SANITATION', '环卫保洁班组', '350111', '晋安区岳峰片区', 119.331000, 26.089000, '河道保洁,消杀', 'ON_DUTY', 1),
(14, 30, 'MEMBER', 5, 'FACILITY', '公共设施班组', '350103', '台江区滨江片区', 119.310000, 26.060000, '排水口,管道', 'ON_DUTY', 1),
(15, 31, 'MEMBER', 5, 'FACILITY', '公共设施班组', '350112', '长乐区航城片区', 119.515000, 25.970000, '木栈道,护栏', 'ON_DUTY', 1),
(16, 32, 'MEMBER', 5, 'FACILITY', '公共设施班组', '350105', '马尾区片区', 119.448000, 25.998000, '公共设施,座椅', 'ON_DUTY', 1);

-- ---------- 招募公告 ----------
INSERT INTO `t_recruitment`
(`id`, `title`, `dept_code`, `dept_name`, `work_zone`, `headcount`, `requirement`, `description`, `status`, `deadline`, `cover_url`, `create_by`, `create_time`, `update_time`)
VALUES
(1, '环卫保洁员招聘', 'SANITATION', '环卫保洁班组', '鼓楼区中心片区', 4,
 '年龄 18-55 岁，身体健康，能适应户外作业；有保洁经验者优先。',
 '负责鼓楼区主干道及河岸步道的日常保洁、垃圾清运与冲洗作业，轮班制，提供劳保用品与岗前培训。',
 'OPEN', DATE_ADD(CURDATE(), INTERVAL 20 DAY), NULL, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2, '路灯巡检修工招聘', 'LIGHTING', '路灯照明班组', '晋安区中心片区', 2,
 '持有低压电工证，熟悉路灯线路检修，能接受夜间应急抢修。',
 '负责晋安区路灯、线路与配电箱巡检，处理跳闸、灯杆倾斜等故障，夜间应急抢修有额外补贴。',
 'OPEN', DATE_ADD(CURDATE(), INTERVAL 15 DAY), NULL, 1, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(3, '道路设施维护员招聘', 'ROAD', '市政道路班组', '仓山区金山片区', 3,
 '能承担路面修补、井盖更换、护栏加固等体力作业，有市政施工经验优先。',
 '负责仓山区金山片区道路设施巡查与小型维修，配合完成井盖缺失、路面破损等应急处置。',
 'OPEN', DATE_ADD(CURDATE(), INTERVAL 25 DAY), NULL, 1, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(4, '公共设施巡查员招聘', 'FACILITY', '公共设施班组', '台江区滨江片区', 2,
 '熟悉市政公共设施，能使用手机完成巡查上报。',
 '负责台江区滨江公园、木栈道、排水口等公共设施巡查，发现损坏及时上报并配合维修。',
 'CLOSED', DATE_SUB(CURDATE(), INTERVAL 2 DAY), NULL, 1, DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY));

INSERT INTO `t_recruitment_apply`
(`id`, `recruitment_id`, `user_id`, `note`, `status`, `create_by`, `create_time`, `update_time`)
VALUES
(1, 1, 11, '本人在鼓楼区做过两年小区保洁，熟悉垃圾分类，可随时到岗。', 'PENDING', 11, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 1, 13, '希望应聘环卫保洁岗位，能接受早班。', 'ACCEPTED', 13, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(3, 2, 14, '有低压电工证，做过路灯维护。', 'PENDING', 14, DATE_SUB(NOW(), INTERVAL 8 HOUR), DATE_SUB(NOW(), INTERVAL 8 HOUR));

-- ---------- 附件（图片由 scripts/reset-demo.ps1 复制到 data/uploads/demo/） ----------
USE city_event;
INSERT INTO `t_attachment`
(`id`, `biz_type`, `biz_id`, `owner_id`, `original_name`, `stored_name`, `file_path`, `access_url`, `content_type`, `file_size`, `sha256`, `status`, `create_time`)
VALUES
(1, 'EVENT_REPORT', 1, 11, '河岸垃圾-晋安.png', 'demo-ev01.png', 'demo/demo-ev01.png', '/uploads/demo/demo-ev01.png', 'image/png', 337991, '38b19263c346003df135d8f4bf1496684f647c72c94280a4181d8e24f93544d6', 'BOUND', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(2, 'EVENT_REPORT', 2, 12, '路灯不亮-鼓楼.png', 'demo-ev02.png', 'demo/demo-ev02.png', '/uploads/demo/demo-ev02.png', 'image/png', 828491, '8f6efe4dafcc3f343f7b1c2d5c47c5c45b1f2cc85f21c9c842175aafb99c2406', 'BOUND', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(3, 'EVENT_REPORT', 3, 13, '井盖缺失-长乐.png', 'demo-ev03.png', 'demo/demo-ev03.png', '/uploads/demo/demo-ev03.png', 'image/png', 1131040, 'a9c110438518572d2a020f15c733787b7f123d915f5952d415fd5d7e49546a80', 'BOUND', DATE_SUB(NOW(), INTERVAL 48 HOUR)),
(4, 'EVENT_REPORT', 4, 14, '垃圾未清-台江.png', 'demo-ev04.png', 'demo/demo-ev04.png', '/uploads/demo/demo-ev04.png', 'image/png', 711211, '824ed4772e45cd3dc1362902b94a9fc28accba766a5e12f4dec809429dc44feb', 'BOUND', DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(5, 'EVENT_REPORT', 5, 15, '河道漂浮物-马尾.png', 'demo-ev05.png', 'demo/demo-ev05.png', '/uploads/demo/demo-ev05.png', 'image/png', 736134, '6abcab66fa997ec9edb58322dda961c3688bdf5f470c82a642e639fe8cf35d0a', 'BOUND', DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(6, 'EVENT_REPORT', 6, 11, '河岸垃圾-仓山.png', 'demo-ev06.png', 'demo/demo-ev06.png', '/uploads/demo/demo-ev06.png', 'image/png', 887874, 'f2c7fb48c75497fde9f3db7fee9976f7a06b87a7cd4b5705fb6c7cc3c4cbad1f', 'BOUND', DATE_SUB(NOW(), INTERVAL 48 HOUR)),
(7, 'EVENT_REPORT', 7, 12, '建筑垃圾-晋安.png', 'demo-ev07.png', 'demo/demo-ev07.png', '/uploads/demo/demo-ev07.png', 'image/png', 98319, 'bde69ee33d9b203a5a8959e77205c3b5d288bf6f84014e5907fb50baf10fc489', 'BOUND', DATE_SUB(NOW(), INTERVAL 72 HOUR)),
(8, 'EVENT_REPORT', 8, 13, '井盖破损-晋安.png', 'demo-ev08.png', 'demo/demo-ev08.png', '/uploads/demo/demo-ev08.png', 'image/png', 70033, '01dcfffb24bc5e40af8aede5c1d5fdd8bfcf70532732f92adb9cc1213427a783', 'BOUND', DATE_SUB(NOW(), INTERVAL 96 HOUR)),
(9, 'EVENT_REPORT', 9, 14, '路灯不亮-鼓楼.png', 'demo-ev09.png', 'demo/demo-ev09.png', '/uploads/demo/demo-ev09.png', 'image/png', 146346, 'fb02da209af8bf550c3d88de56497826139581439222a6e4ce9a5f8621cd68d7', 'BOUND', DATE_SUB(NOW(), INTERVAL 96 HOUR)),
(10, 'EVENT_REPORT', 10, 15, '盖板缺失-马尾.png', 'demo-ev10.png', 'demo/demo-ev10.png', '/uploads/demo/demo-ev10.png', 'image/png', 64760, 'c6569549585450790eac99e85d944db62f42803b4b6cf7281f7cb60c1a4ba6f5', 'BOUND', DATE_SUB(NOW(), INTERVAL 120 HOUR)),
(11, 'EVENT_REPORT', 11, 11, '栈道护栏-长乐.png', 'demo-ev11.png', 'demo/demo-ev11.png', '/uploads/demo/demo-ev11.png', 'image/png', 1654620, '02bebce03379e90f0b71bdc4ceba1705108f8747b375c68ac73bb4f80e5708b6', 'BOUND', DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(12, 'EVENT_REPORT', 12, 12, '垃圾未清-台江.png', 'demo-ev12.png', 'demo/demo-ev12.png', '/uploads/demo/demo-ev12.png', 'image/png', 79204, '03b710757b6318098775c331d36e569623173ca05530ff298c7554a3280635db', 'BOUND', DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(13, 'EVENT_REPORT', 13, 13, '河岸垃圾-仓山.png', 'demo-ev13.png', 'demo/demo-ev13.png', '/uploads/demo/demo-ev13.png', 'image/png', 1028224, '53a33541cf346ec10a083ccdfd08290de3be35ffd01901a6e4f90464221c3a47', 'BOUND', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(14, 'WORKORDER_PROCESS', 2, 27, '处理中-晋安.png', 'demo-wo02-process.png', 'demo/demo-wo02-process.png', '/uploads/demo/demo-wo02-process.png', 'image/png', 887874, 'f2c7fb48c75497fde9f3db7fee9976f7a06b87a7cd4b5705fb6c7cc3c4cbad1f', 'BOUND', DATE_SUB(NOW(), INTERVAL 40 HOUR)),
(15, 'WORKORDER_RESULT', 3, 21, '整改后-晋安.jpg', 'demo-wo07-result.jpg', 'demo/demo-wo07-result.jpg', '/uploads/demo/demo-wo07-result.jpg', 'image/jpeg', 242009, 'fd0a020657dcab7d946071ffa2ef6386c92415f14d8069825dc568c3efe077ab', 'BOUND', DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(16, 'WORKORDER_RESULT', 4, 3, '整改后-鼓楼.jpg', 'demo-wo08-result.jpg', 'demo/demo-wo08-result.jpg', '/uploads/demo/demo-wo08-result.jpg', 'image/jpeg', 228644, 'b01c65f1be0356ad2f62c8e96e7e2f8e79636c7791a81b06d0b86e12c85f74b5', 'BOUND', DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(17, 'WORKORDER_PROCESS', 4, 3, '处理中-鼓楼.png', 'demo-wo08-process.png', 'demo/demo-wo08-process.png', '/uploads/demo/demo-wo08-process.png', 'image/png', 70033, '01dcfffb24bc5e40af8aede5c1d5fdd8bfcf70532732f92adb9cc1213427a783', 'BOUND', DATE_SUB(NOW(), INTERVAL 36 HOUR)),
(18, 'WORKORDER_RESULT', 5, 22, '整改后-马尾.jpg', 'demo-wo09-result.jpg', 'demo/demo-wo09-result.jpg', '/uploads/demo/demo-wo09-result.jpg', 'image/jpeg', 212435, '2c272483996a7f5549edbaf01ded9edc4b88331f9a162b06075b0639dd390046', 'BOUND', DATE_SUB(NOW(), INTERVAL 30 HOUR)),
(19, 'WORKORDER_PROCESS', 6, 31, '处理中-长乐.png', 'demo-wo06-process.png', 'demo/demo-wo06-process.png', '/uploads/demo/demo-wo06-process.png', 'image/png', 64760, 'c6569549585450790eac99e85d944db62f42803b4b6cf7281f7cb60c1a4ba6f5', 'BOUND', DATE_SUB(NOW(), INTERVAL 72 HOUR)),
(20, 'WORKORDER_RESULT', 6, 31, '整改后-长乐.jpg', 'demo-wo10-result.jpg', 'demo/demo-wo10-result.jpg', '/uploads/demo/demo-wo10-result.jpg', 'image/jpeg', 202246, '717af14395e3249d9fa4da7381a361e97c991a6a0a40ee532efbb0c64dc893fd', 'BOUND', DATE_SUB(NOW(), INTERVAL 48 HOUR)),
(21, 'WORKORDER_PROCESS', 7, 28, '处理中-台江.png', 'demo-wo12-process.png', 'demo/demo-wo12-process.png', '/uploads/demo/demo-wo12-process.png', 'image/png', 79204, '03b710757b6318098775c331d36e569623173ca05530ff298c7554a3280635db', 'BOUND', DATE_SUB(NOW(), INTERVAL 60 HOUR)),
(22, 'WORKORDER_RESULT', 7, 28, '整改后-台江.jpg', 'demo-wo13-result.jpg', 'demo/demo-wo13-result.jpg', '/uploads/demo/demo-wo13-result.jpg', 'image/jpeg', 242009, 'fd0a020657dcab7d946071ffa2ef6386c92415f14d8069825dc568c3efe077ab', 'BOUND', DATE_SUB(NOW(), INTERVAL 30 HOUR));

-- ---------- 事件（全部位于福州市六城区） ----------
INSERT INTO `t_event`
(`id`, `event_no`, `reporter_id`, `category_code`, `ai_category_code`, `title`, `description`, `summary`, `priority`, `ai_priority`, `ai_confidence`, `ai_suggestion`,
 `ai_review_priority`, `ai_review_reason`, `ai_review_impact`, `ai_review_response_minutes`, `ai_review_confidence`, `ai_review_time`,
 `status`, `province`, `city`, `district`, `street`, `area_code`, `address`, `longitude`, `latitude`, `photo_count`, `auditor_id`, `audit_time`, `reject_reason`, `dispatch_time`, `close_time`, `create_time`, `update_time`, `create_by`)
VALUES
(1, 'EV202610220001', 11, 'ENVIRONMENT_SANITATION', 'ENVIRONMENT_SANITATION', '晋安河步道旁成堆生活垃圾',
 '晋安区岳峰镇塔头路靠近晋安河的步道旁，发现多处生活垃圾和塑料袋堆积，部分垃圾已散落到水面，散发异味，影响周边居民散步。',
 '河岸步道旁多处生活垃圾堆积，部分散落水面。', 'MEDIUM', 'HIGH', 0.880, '安排环卫班组清运垃圾并冲洗地面',
 'HIGH', '问题位于居民日常散步的河岸步道，垃圾已散落水面并有异味，影响人数较多，建议按高等级在 8 小时内处置。', '河岸步道约 200 米范围及下游水面', 480, 0.860, DATE_SUB(NOW(), INTERVAL 2 HOUR),
 'PENDING_REVIEW', '福建省', '福州市', '晋安区', '岳峰镇', '350111', '福建省福州市晋安区岳峰镇塔头路晋安河岸边',
 119.331000, 26.089000, 1, NULL, NULL, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 3 HOUR), 11),
(2, 'EV202610220002', 12, 'PUBLIC_LIGHTING', 'PUBLIC_LIGHTING', '五四路连续多盏路灯不亮',
 '鼓楼区五四路与湖东路交叉口向北约 200 米，道路两侧连续五六盏路灯不亮，夜间行人多，靠近学校路段视线很差。',
 '五四路多盏路灯不亮，夜间通行视线差。', 'MEDIUM', 'HIGH', 0.900, '安排照明班组当晚检修线路并更换灯具',
 'HIGH', '路段靠近学校且夜间行人较多，连续多盏路灯不亮存在治安与通行风险，建议 8 小时内修复。', '五四路约 200 米路段夜间通行', 480, 0.900, DATE_SUB(NOW(), INTERVAL 1 HOUR),
 'PENDING_REVIEW', '福建省', '福州市', '鼓楼区', '温泉街道', '350102', '福建省福州市鼓楼区五四路与湖东路交叉口北侧',
 119.303000, 26.087000, 1, NULL, NULL, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR), 12),
(3, 'EV202610220003', 13, 'ROAD_FACILITY', 'ROAD_FACILITY', '朝阳路人行道井盖缺失',
 '长乐区吴航街道朝阳路人行道上一处方形井盖缺失，缺口约半米，周围没有警示标志，夜间行人容易踩空。',
 '人行道井盖缺失，无明显警示。', 'HIGH', 'URGENT', 0.940, '立即设置围挡警示并更换井盖',
 'URGENT', '井盖缺失位于人行道且无警示，存在明确人身伤害风险，建议 2 小时内设置警示并完成更换。', '朝阳路人行道行人及非机动车', 120, 0.940, DATE_SUB(NOW(), INTERVAL 40 MINUTE),
 'PENDING_REVIEW', '福建省', '福州市', '长乐区', '吴航街道', '350112', '福建省福州市长乐区吴航街道朝阳路人行道',
 119.523000, 25.963000, 1, NULL, NULL, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 48 HOUR), DATE_SUB(NOW(), INTERVAL 48 HOUR), 13),
(4, 'EV202610220004', 14, 'ENVIRONMENT_SANITATION', 'ENVIRONMENT_SANITATION', '小区外垃圾桶周边垃圾未清理',
 '台江区瀛洲街道江滨中大道某小区外侧垃圾桶周边散落大量餐盒和塑料袋，气味明显。',
 '小区外垃圾桶周边垃圾散落。', 'MEDIUM', NULL, NULL, NULL,
 'MEDIUM', '垃圾桶周边散落垃圾，影响局部环境，建议 24 小时内清运。', '小区外侧人行道约 20 米', 1440, 0.720, DATE_SUB(NOW(), INTERVAL 23 HOUR),
 'REJECTED', '福建省', '福州市', '台江区', '瀛洲街道', '350103', '福建省福州市台江区瀛洲街道江滨中大道',
 119.310000, 26.055000, 1, 1, DATE_SUB(NOW(), INTERVAL 22 HOUR),
 '照片未拍到门牌或明显参照物，无法确认具体位置，请补充带地标的照片后重新提交',
 NULL, NULL, DATE_SUB(NOW(), INTERVAL 24 HOUR), DATE_SUB(NOW(), INTERVAL 22 HOUR), 14),
(5, 'EV202610220005', 15, 'ENVIRONMENT_SANITATION', NULL, '内河水面漂浮塑料瓶',
 '马尾区罗星街道港口路内河河面漂浮塑料瓶和泡沫块，水流缓慢处堆积成片。',
 '内河河面漂浮塑料垃圾。', 'MEDIUM', NULL, NULL, NULL,
 'MEDIUM', '河面漂浮垃圾影响观感，建议 24 小时内打捞。', '港口路内河约 100 米河段', 1440, 0.700, DATE_SUB(NOW(), INTERVAL 23 HOUR),
 'CANCELLED', '福建省', '福州市', '马尾区', '罗星街道', '350105', '福建省福州市马尾区罗星街道港口路内河',
 119.455000, 25.990000, 1, NULL, NULL, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 24 HOUR), DATE_SUB(NOW(), INTERVAL 20 HOUR), 15),
(6, 'EV202610220006', 11, 'ENVIRONMENT_SANITATION', 'ENVIRONMENT_SANITATION', '浦上大道河边垃圾堆积影响通行',
 '仓山区金山街道浦上大道河边垃圾成堆，沿河步道被占，行人只能绕行机动车道。',
 '河岸垃圾堆积占道，影响通行。', 'HIGH', 'HIGH', 0.910, '当天清运并恢复步道通行',
 'HIGH', '垃圾堆积占用沿河步道，行人被迫绕行机动车道，存在交通安全风险，建议 8 小时内清运。', '浦上大道沿河步道及非机动车道', 480, 0.890, DATE_SUB(NOW(), INTERVAL 47 HOUR),
 'DISPATCHED', '福建省', '福州市', '仓山区', '金山街道', '350104', '福建省福州市仓山区金山街道浦上大道河边',
 119.273000, 26.030000, 1, 1, DATE_SUB(NOW(), INTERVAL 46 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 46 HOUR), NULL,
 DATE_SUB(NOW(), INTERVAL 48 HOUR), DATE_SUB(NOW(), INTERVAL 46 HOUR), 11),
(7, 'EV202610220007', 12, 'ENVIRONMENT_SANITATION', 'ENVIRONMENT_SANITATION', '福新东路路边建筑垃圾长期堆放',
 '晋安区鼓山镇福新东路辅道旁堆放着水泥块、废木板和编织袋等建筑垃圾，已堆放一周无人清理。',
 '辅道旁建筑垃圾长期堆放。', 'HIGH', 'HIGH', 0.870, '联系属地确认责任单位后清运',
 'HIGH', '建筑垃圾占用辅道且长期堆放，影响通行与市容，建议 8 小时内清运。', '福新东路辅道约 30 米', 480, 0.850, DATE_SUB(NOW(), INTERVAL 70 HOUR),
 'PROCESSING', '福建省', '福州市', '晋安区', '鼓山镇', '350111', '福建省福州市晋安区鼓山镇福新东路辅道',
 119.345000, 26.070000, 1, 1, DATE_SUB(NOW(), INTERVAL 70 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 70 HOUR), NULL,
 DATE_SUB(NOW(), INTERVAL 72 HOUR), DATE_SUB(NOW(), INTERVAL 44 HOUR), 12),
(8, 'EV202610220008', 13, 'ROAD_FACILITY', 'ROAD_FACILITY', '秀山路井盖破损塌陷',
 '晋安区新店镇秀山路非机动车道上井盖破损并轻微塌陷，边缘锋利，骑车经过容易摔倒。',
 '非机动车道井盖破损塌陷。', 'HIGH', 'HIGH', 0.900, '更换井盖并排查同路段井盖',
 'URGENT', '井盖破损塌陷并位于非机动车道，边缘锋利，对骑行者有明确伤害风险，建议 2 小时内设置警示并更换。', '秀山路非机动车道骑行者', 120, 0.920, DATE_SUB(NOW(), INTERVAL 94 HOUR),
 'PROCESSING', '福建省', '福州市', '晋安区', '新店镇', '350111', '福建省福州市晋安区新店镇秀山路非机动车道',
 119.318000, 26.120000, 1, 1, DATE_SUB(NOW(), INTERVAL 94 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 94 HOUR), NULL,
 DATE_SUB(NOW(), INTERVAL 96 HOUR), DATE_SUB(NOW(), INTERVAL 24 HOUR), 13),
(9, 'EV202610220009', 14, 'PUBLIC_LIGHTING', 'PUBLIC_LIGHTING', '小区门口路段路灯不亮',
 '鼓楼区温泉街道六一北路某小区门口约 100 米路段路灯全部不亮，居民夜间进出不便。',
 '小区门口路段路灯不亮。', 'HIGH', 'HIGH', 0.860, '检修线路并恢复照明',
 'MEDIUM', '路段为小区出入口，夜间人行需求有限，建议 24 小时内修复。', '小区出入口约 100 米路段', 1440, 0.780, DATE_SUB(NOW(), INTERVAL 93 HOUR),
 'PENDING_ACCEPTANCE', '福建省', '福州市', '鼓楼区', '温泉街道', '350102', '福建省福州市鼓楼区温泉街道六一北路小区门口',
 119.312000, 26.095000, 1, 1, DATE_SUB(NOW(), INTERVAL 93 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 93 HOUR), NULL,
 DATE_SUB(NOW(), INTERVAL 96 HOUR), DATE_SUB(NOW(), INTERVAL 24 HOUR), 14),
(10, 'EV202610220010', 15, 'ROAD_FACILITY', 'ROAD_FACILITY', '儒江路排水沟盖板缺失',
 '马尾区马尾镇儒江路沿街店铺门前排水沟盖板缺失两块，沟内积水，行人容易崴脚。',
 '沿街排水沟盖板缺失。', 'HIGH', 'HIGH', 0.880, '补齐盖板并清理沟内积水',
 'HIGH', '盖板缺失且沟内积水，沿街人流量较大，建议 8 小时内补齐。', '儒江路沿街人行道约 10 米', 480, 0.860, DATE_SUB(NOW(), INTERVAL 118 HOUR),
 'PENDING_ACCEPTANCE', '福建省', '福州市', '马尾区', '马尾镇', '350105', '福建省福州市马尾区马尾镇儒江路沿街店铺门前',
 119.448000, 25.998000, 1, 1, DATE_SUB(NOW(), INTERVAL 118 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 118 HOUR), NULL,
 DATE_SUB(NOW(), INTERVAL 120 HOUR), DATE_SUB(NOW(), INTERVAL 24 HOUR), 15),
(11, 'EV202610220011', 11, 'PUBLIC_FACILITY', 'PUBLIC_FACILITY', '郑和路木栈道护栏断裂',
 '长乐区航城街道郑和路河岸休闲木栈道护栏断裂，木板翘起，儿童靠近时有跌落风险。',
 '木栈道护栏断裂，存在安全隐患。', 'MEDIUM', 'HIGH', 0.820, '维修护栏并排查同段栈道',
 'HIGH', '栈道护栏断裂紧邻河岸，儿童活动较多，存在跌落风险，建议 8 小时内维修。', '郑和路河岸木栈道约 50 米', 480, 0.880, DATE_SUB(NOW(), INTERVAL 94 HOUR),
 'CLOSED', '福建省', '福州市', '长乐区', '航城街道', '350112', '福建省福州市长乐区航城街道郑和路河岸木栈道',
 119.515000, 25.970000, 1, 1, DATE_SUB(NOW(), INTERVAL 94 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 94 HOUR), DATE_SUB(NOW(), INTERVAL 24 HOUR),
 DATE_SUB(NOW(), INTERVAL 96 HOUR), DATE_SUB(NOW(), INTERVAL 24 HOUR), 11),
(12, 'EV202610220012', 12, 'ENVIRONMENT_SANITATION', 'ENVIRONMENT_SANITATION', '广达路垃圾清运不彻底',
 '台江区茶亭街道广达路垃圾收集点清运后仍有散落垃圾和污水，异味明显。',
 '垃圾收集点清运不彻底，有异味。', 'MEDIUM', 'MEDIUM', 0.780, '二次清运并冲洗地面',
 'MEDIUM', '局部垃圾收集点残留垃圾与污水，影响周边店铺与行人，建议 24 小时内处理。', '广达路垃圾收集点周边约 15 米', 1440, 0.800, DATE_SUB(NOW(), INTERVAL 94 HOUR),
 'CLOSED', '福建省', '福州市', '台江区', '茶亭街道', '350103', '福建省福州市台江区茶亭街道广达路垃圾收集点',
 119.306000, 26.062000, 1, 1, DATE_SUB(NOW(), INTERVAL 94 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 94 HOUR), DATE_SUB(NOW(), INTERVAL 24 HOUR),
 DATE_SUB(NOW(), INTERVAL 96 HOUR), DATE_SUB(NOW(), INTERVAL 24 HOUR), 12),
(13, 'EV202610220013', 13, 'ENVIRONMENT_SANITATION', 'ENVIRONMENT_SANITATION', '闽江大道河岸垃圾成堆散发异味',
 '仓山区仓前街道闽江大道河岸边垃圾成堆，含有餐厨垃圾，天气炎热后异味明显，蚊虫聚集。',
 '河岸垃圾成堆，异味明显。', 'HIGH', 'URGENT', 0.930, '立即清运并对岸边消杀',
 'URGENT', '餐厨垃圾堆放已产生明显异味并滋生蚊虫，紧邻河岸与居民活动区，建议 2 小时内清运消杀。', '闽江大道河岸约 100 米及周边居民', 120, 0.930, DATE_SUB(NOW(), INTERVAL 50 MINUTE),
 'DISPATCHED', '福建省', '福州市', '仓山区', '仓前街道', '350104', '福建省福州市仓山区仓前街道闽江大道河岸边',
 119.280000, 26.040000, 1, 1, DATE_SUB(NOW(), INTERVAL 50 MINUTE), NULL, DATE_SUB(NOW(), INTERVAL 50 MINUTE), NULL,
 DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_SUB(NOW(), INTERVAL 50 MINUTE), 13);

-- ---------- 事件状态流水 ----------
INSERT INTO `t_event_status_log`
(`event_id`, `event_no`, `from_status`, `to_status`, `action`, `operator_id`, `operator_role`, `remark`, `create_time`)
VALUES
(1, 'EV202610220001', NULL, 'PENDING_REVIEW', 'CREATE', 11, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(2, 'EV202610220002', NULL, 'PENDING_REVIEW', 'CREATE', 12, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(3, 'EV202610220003', NULL, 'PENDING_REVIEW', 'CREATE', 13, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 48 HOUR)),
(4, 'EV202610220004', NULL, 'PENDING_REVIEW', 'CREATE', 14, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(4, 'EV202610220004', 'PENDING_REVIEW', 'REJECTED', 'REJECT', 1, 'ADMIN', '照片未拍到门牌，无法确认具体位置', DATE_SUB(NOW(), INTERVAL 22 HOUR)),
(5, 'EV202610220005', NULL, 'PENDING_REVIEW', 'CREATE', 15, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(5, 'EV202610220005', 'PENDING_REVIEW', 'CANCELLED', 'CANCEL', 15, 'RESIDENT', '已由河道保洁队现场处理，居民撤销', DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(6, 'EV202610220006', NULL, 'PENDING_REVIEW', 'CREATE', 11, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 48 HOUR)),
(6, 'EV202610220006', 'PENDING_REVIEW', 'DISPATCHED', 'APPROVE', 1, 'ADMIN', '审核通过，派单给环卫组长张环卫', DATE_SUB(NOW(), INTERVAL 46 HOUR)),
(7, 'EV202610220007', NULL, 'PENDING_REVIEW', 'CREATE', 12, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 72 HOUR)),
(7, 'EV202610220007', 'PENDING_REVIEW', 'DISPATCHED', 'APPROVE', 1, 'ADMIN', '审核通过，派单给环卫组长张环卫', DATE_SUB(NOW(), INTERVAL 70 HOUR)),
(7, 'EV202610220007', 'DISPATCHED', 'PROCESSING', 'MQ_SYNC', 0, 'SYSTEM', '工单状态同步：WO202610220002', DATE_SUB(NOW(), INTERVAL 44 HOUR)),
(8, 'EV202610220008', NULL, 'PENDING_REVIEW', 'CREATE', 13, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 96 HOUR)),
(8, 'EV202610220008', 'PENDING_REVIEW', 'DISPATCHED', 'APPROVE', 1, 'ADMIN', '审核通过，派单给道路组长李道路', DATE_SUB(NOW(), INTERVAL 94 HOUR)),
(8, 'EV202610220008', 'DISPATCHED', 'PROCESSING', 'MQ_SYNC', 0, 'SYSTEM', '工单状态同步：WO202610220003', DATE_SUB(NOW(), INTERVAL 68 HOUR)),
(9, 'EV202610220009', NULL, 'PENDING_REVIEW', 'CREATE', 14, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 96 HOUR)),
(9, 'EV202610220009', 'PENDING_REVIEW', 'DISPATCHED', 'APPROVE', 1, 'ADMIN', '审核通过，派单给照明组长王照明', DATE_SUB(NOW(), INTERVAL 93 HOUR)),
(9, 'EV202610220009', 'DISPATCHED', 'PROCESSING', 'MQ_SYNC', 0, 'SYSTEM', '工单状态同步：WO202610220004', DATE_SUB(NOW(), INTERVAL 92 HOUR)),
(9, 'EV202610220009', 'PROCESSING', 'PENDING_ACCEPTANCE', 'MQ_SYNC', 0, 'SYSTEM', '组长处理完成，工单状态同步：WO202610220004', DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(10, 'EV202610220010', NULL, 'PENDING_REVIEW', 'CREATE', 15, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 120 HOUR)),
(10, 'EV202610220010', 'PENDING_REVIEW', 'DISPATCHED', 'APPROVE', 1, 'ADMIN', '审核通过，派单给道路组长李道路', DATE_SUB(NOW(), INTERVAL 118 HOUR)),
(10, 'EV202610220010', 'DISPATCHED', 'PROCESSING', 'MQ_SYNC', 0, 'SYSTEM', '工单状态同步：WO202610220005', DATE_SUB(NOW(), INTERVAL 110 HOUR)),
(10, 'EV202610220010', 'PROCESSING', 'PENDING_ACCEPTANCE', 'MQ_SYNC', 0, 'SYSTEM', '组长复核通过，工单状态同步：WO202610220005', DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(11, 'EV202610220011', NULL, 'PENDING_REVIEW', 'CREATE', 11, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 96 HOUR)),
(11, 'EV202610220011', 'PENDING_REVIEW', 'DISPATCHED', 'APPROVE', 1, 'ADMIN', '审核通过，派单给设施组长赵设施', DATE_SUB(NOW(), INTERVAL 94 HOUR)),
(11, 'EV202610220011', 'DISPATCHED', 'PROCESSING', 'MQ_SYNC', 0, 'SYSTEM', '工单状态同步：WO202610220006', DATE_SUB(NOW(), INTERVAL 90 HOUR)),
(11, 'EV202610220011', 'PROCESSING', 'PENDING_ACCEPTANCE', 'MQ_SYNC', 0, 'SYSTEM', '组长复核通过，工单状态同步：WO202610220006', DATE_SUB(NOW(), INTERVAL 48 HOUR)),
(11, 'EV202610220011', 'PENDING_ACCEPTANCE', 'CLOSED', 'MQ_SYNC', 0, 'SYSTEM', '验收通过，事件关闭', DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(12, 'EV202610220012', NULL, 'PENDING_REVIEW', 'CREATE', 12, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 96 HOUR)),
(12, 'EV202610220012', 'PENDING_REVIEW', 'DISPATCHED', 'APPROVE', 1, 'ADMIN', '审核通过，派单给环卫组长张环卫', DATE_SUB(NOW(), INTERVAL 94 HOUR)),
(12, 'EV202610220012', 'DISPATCHED', 'PROCESSING', 'MQ_SYNC', 0, 'SYSTEM', '工单状态同步：WO202610220007', DATE_SUB(NOW(), INTERVAL 90 HOUR)),
(12, 'EV202610220012', 'PROCESSING', 'PENDING_ACCEPTANCE', 'MQ_SYNC', 0, 'SYSTEM', '组长复核通过，工单状态同步：WO202610220007', DATE_SUB(NOW(), INTERVAL 48 HOUR)),
(12, 'EV202610220012', 'PENDING_ACCEPTANCE', 'CLOSED', 'MQ_SYNC', 0, 'SYSTEM', '验收通过，事件关闭', DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(13, 'EV202610220013', NULL, 'PENDING_REVIEW', 'CREATE', 13, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(13, 'EV202610220013', 'PENDING_REVIEW', 'DISPATCHED', 'APPROVE', 1, 'ADMIN', '紧急工单，审核通过并派单给环卫组长张环卫', DATE_SUB(NOW(), INTERVAL 50 MINUTE));

-- ---------- 事件审核记录 ----------
INSERT INTO `t_event_audit_log`
(`event_id`, `auditor_id`, `decision`, `before_category`, `after_category`, `before_priority`, `after_priority`, `reason`, `create_time`)
VALUES
(4, 1, 'REJECT', 'ENVIRONMENT_SANITATION', 'ENVIRONMENT_SANITATION', 'MEDIUM', 'MEDIUM', '照片未拍到门牌，无法确认具体位置', DATE_SUB(NOW(), INTERVAL 22 HOUR)),
(6, 1, 'APPROVE', 'ENVIRONMENT_SANITATION', 'ENVIRONMENT_SANITATION', 'HIGH', 'HIGH', '情况属实，安排清运', DATE_SUB(NOW(), INTERVAL 46 HOUR)),
(7, 1, 'APPROVE', 'ENVIRONMENT_SANITATION', 'ENVIRONMENT_SANITATION', 'HIGH', 'HIGH', '情况属实，安排清运', DATE_SUB(NOW(), INTERVAL 70 HOUR)),
(8, 1, 'APPROVE', 'ROAD_FACILITY', 'ROAD_FACILITY', 'HIGH', 'URGENT', '采纳 AI 复评，井盖塌陷存在人身风险，按紧急派单', DATE_SUB(NOW(), INTERVAL 94 HOUR)),
(9, 1, 'APPROVE', 'PUBLIC_LIGHTING', 'PUBLIC_LIGHTING', 'HIGH', 'MEDIUM', '采纳 AI 复评，小区出入口影响有限，按中等级处理', DATE_SUB(NOW(), INTERVAL 93 HOUR)),
(10, 1, 'APPROVE', 'ROAD_FACILITY', 'ROAD_FACILITY', 'HIGH', 'HIGH', '情况属实，派单道路班组', DATE_SUB(NOW(), INTERVAL 118 HOUR)),
(11, 1, 'APPROVE', 'PUBLIC_FACILITY', 'PUBLIC_FACILITY', 'MEDIUM', 'HIGH', '采纳 AI 复评，木栈道护栏断裂存在跌落风险', DATE_SUB(NOW(), INTERVAL 94 HOUR)),
(12, 1, 'APPROVE', 'ENVIRONMENT_SANITATION', 'ENVIRONMENT_SANITATION', 'MEDIUM', 'MEDIUM', '情况属实，安排二次清运', DATE_SUB(NOW(), INTERVAL 94 HOUR)),
(13, 1, 'APPROVE', 'ENVIRONMENT_SANITATION', 'ENVIRONMENT_SANITATION', 'HIGH', 'URGENT', '餐厨垃圾异味明显，按紧急工单派给环卫组长', DATE_SUB(NOW(), INTERVAL 50 MINUTE));

-- ---------- 事件 AI 快照 ----------
INSERT INTO `t_event_ai_snapshot`
(`event_id`, `ai_call_log_id`, `category_code`, `priority`, `summary`, `suggestion`, `confidence`, `model_name`, `accepted`, `create_time`)
VALUES
(1, 1, 'ENVIRONMENT_SANITATION', 'HIGH', '河岸步道旁多处生活垃圾堆积，部分散落水面。', '安排环卫班组清运垃圾并冲洗地面', 0.880, 'glm-4-flash', 0, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(6, 2, 'ENVIRONMENT_SANITATION', 'HIGH', '河岸垃圾堆积占道，影响通行。', '当天清运并恢复步道通行', 0.910, 'glm-4-flash', 1, DATE_SUB(NOW(), INTERVAL 48 HOUR)),
(13, 3, 'ENVIRONMENT_SANITATION', 'URGENT', '河岸垃圾成堆，异味明显。', '立即清运并对岸边消杀', 0.930, 'glm-4-flash', 1, DATE_SUB(NOW(), INTERVAL 1 HOUR));

-- ---------- 工单 ----------
USE city_workorder;
INSERT INTO `t_work_order`
(`id`, `work_order_no`, `event_id`, `event_no`, `category_code`, `priority`, `assignee_id`, `assignee_name`, `leader_id`, `leader_name`, `dept_code`, `status`,
 `creator_id`, `assign_time`, `accept_time`, `submit_time`, `leader_review_time`, `leader_review_remark`, `finish_time`, `acceptance_remark`, `return_count`, `reassign_count`,
 `ai_verify_result`, `ai_verify_time`, `create_time`, `update_time`)
VALUES
(1, 'WO202610220001', 6, 'EV202610220006', 'ENVIRONMENT_SANITATION', 'HIGH', 4, '张环卫', 4, '张环卫', 'SANITATION', 'PENDING_ACCEPT',
 1, DATE_SUB(NOW(), INTERVAL 46 HOUR), NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 46 HOUR), DATE_SUB(NOW(), INTERVAL 46 HOUR)),
(2, 'WO202610220002', 7, 'EV202610220007', 'ENVIRONMENT_SANITATION', 'HIGH', 27, '冯清运', 4, '张环卫', 'SANITATION', 'PROCESSING',
 1, DATE_SUB(NOW(), INTERVAL 70 HOUR), DATE_SUB(NOW(), INTERVAL 44 HOUR), NULL, NULL, NULL, NULL, NULL, 0, 0, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 70 HOUR), DATE_SUB(NOW(), INTERVAL 40 HOUR)),
(3, 'WO202610220003', 8, 'EV202610220008', 'ROAD_FACILITY', 'URGENT', 21, '陈路桥', 2, '李道路', 'ROAD', 'PENDING_LEADER_REVIEW',
 1, DATE_SUB(NOW(), INTERVAL 94 HOUR), DATE_SUB(NOW(), INTERVAL 68 HOUR), DATE_SUB(NOW(), INTERVAL 24 HOUR), NULL, NULL, NULL, NULL, 0, 0,
 '{"conclusion":"RECTIFIED","confidence":0.880,"reason":"整改后照片中破损井盖已更换，路面平整，与上报问题一致。","issues":[],"degraded":false,"degradeReason":null,"model":"glm-4.6v-flash","verifiedAt":"2026-10-22T08:20:00"}',
 DATE_SUB(NOW(), INTERVAL 23 HOUR), DATE_SUB(NOW(), INTERVAL 94 HOUR), DATE_SUB(NOW(), INTERVAL 23 HOUR)),
(4, 'WO202610220004', 9, 'EV202610220009', 'PUBLIC_LIGHTING', 'MEDIUM', 3, '王照明', 3, '王照明', 'LIGHTING', 'PENDING_ACCEPTANCE',
 1, DATE_SUB(NOW(), INTERVAL 93 HOUR), DATE_SUB(NOW(), INTERVAL 92 HOUR), DATE_SUB(NOW(), INTERVAL 24 HOUR), NULL, NULL, NULL, NULL, 0, 0,
 '{"conclusion":"RECTIFIED","confidence":0.900,"reason":"整改后照片中路段路灯已恢复照明，现场亮度正常。","issues":[],"degraded":false,"degradeReason":null,"model":"glm-4.6v-flash","verifiedAt":"2026-10-22T09:10:00"}',
 DATE_SUB(NOW(), INTERVAL 23 HOUR), DATE_SUB(NOW(), INTERVAL 93 HOUR), DATE_SUB(NOW(), INTERVAL 23 HOUR)),
(5, 'WO202610220005', 10, 'EV202610220010', 'ROAD_FACILITY', 'HIGH', 22, '刘井盖', 2, '李道路', 'ROAD', 'PENDING_ACCEPTANCE',
 1, DATE_SUB(NOW(), INTERVAL 118 HOUR), DATE_SUB(NOW(), INTERVAL 110 HOUR), DATE_SUB(NOW(), INTERVAL 30 HOUR), DATE_SUB(NOW(), INTERVAL 24 HOUR), '现场盖板已补齐，照片可见沟面平整，复核通过', NULL, NULL, 0, 0,
 '{"conclusion":"RECTIFIED","confidence":0.870,"reason":"整改后照片中排水沟盖板已补齐，周边无积水残留。","issues":[],"degraded":false,"degradeReason":null,"model":"glm-4.6v-flash","verifiedAt":"2026-10-22T07:40:00"}',
 DATE_SUB(NOW(), INTERVAL 29 HOUR), DATE_SUB(NOW(), INTERVAL 118 HOUR), DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(6, 'WO202610220006', 11, 'EV202610220011', 'PUBLIC_FACILITY', 'HIGH', 31, '沈栈道', 5, '赵设施', 'FACILITY', 'COMPLETED',
 1, DATE_SUB(NOW(), INTERVAL 94 HOUR), DATE_SUB(NOW(), INTERVAL 90 HOUR), DATE_SUB(NOW(), INTERVAL 48 HOUR), DATE_SUB(NOW(), INTERVAL 36 HOUR), '护栏已更换并加固，复核通过', DATE_SUB(NOW(), INTERVAL 24 HOUR), '栈道护栏已修复，现场复查无松动', 0, 0,
 '{"conclusion":"RECTIFIED","confidence":0.920,"reason":"整改后照片中木栈道护栏已更换，连接牢固。","issues":[],"degraded":false,"degradeReason":null,"model":"glm-4.6v-flash","verifiedAt":"2026-10-21T14:10:00"}',
 DATE_SUB(NOW(), INTERVAL 47 HOUR), DATE_SUB(NOW(), INTERVAL 94 HOUR), DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(7, 'WO202610220007', 12, 'EV202610220012', 'ENVIRONMENT_SANITATION', 'MEDIUM', 28, '褚保洁', 4, '张环卫', 'SANITATION', 'COMPLETED',
 1, DATE_SUB(NOW(), INTERVAL 94 HOUR), DATE_SUB(NOW(), INTERVAL 90 HOUR), DATE_SUB(NOW(), INTERVAL 30 HOUR), DATE_SUB(NOW(), INTERVAL 24 HOUR), '已二次清运并冲洗，复核通过', DATE_SUB(NOW(), INTERVAL 24 HOUR), '现场整洁，验收通过', 0, 0,
 '{"conclusion":"PARTIALLY_RECTIFIED","confidence":0.760,"reason":"整改后照片中垃圾已基本清运，地面仍有少量水渍。","issues":["地面水渍未完全干"],"degraded":false,"degradeReason":null,"model":"glm-4.6v-flash","verifiedAt":"2026-10-21T16:30:00"}',
 DATE_SUB(NOW(), INTERVAL 29 HOUR), DATE_SUB(NOW(), INTERVAL 94 HOUR), DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(8, 'WO202610220008', 13, 'EV202610220013', 'ENVIRONMENT_SANITATION', 'URGENT', 4, '张环卫', 4, '张环卫', 'SANITATION', 'PENDING_ACCEPT',
 1, DATE_SUB(NOW(), INTERVAL 50 MINUTE), NULL, NULL, NULL, NULL, NULL, NULL, 0, 0, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 50 MINUTE), DATE_SUB(NOW(), INTERVAL 50 MINUTE));

-- ---------- 工单处理记录 ----------
INSERT INTO `t_work_order_log`
(`work_order_id`, `work_order_no`, `action`, `from_status`, `to_status`, `operator_id`, `operator_role`, `content`, `attachment_ids`, `create_time`)
VALUES
(1, 'WO202610220001', 'CREATE', NULL, 'PENDING_ACCEPT', 1, 'ADMIN', '审核通过，创建工单并派单给环卫组长张环卫', NULL, DATE_SUB(NOW(), INTERVAL 46 HOUR)),
(2, 'WO202610220002', 'CREATE', NULL, 'PENDING_ACCEPT', 1, 'ADMIN', '审核通过，创建工单并派单给环卫组长张环卫', NULL, DATE_SUB(NOW(), INTERVAL 70 HOUR)),
(2, 'WO202610220002', 'ASSIGN', 'PENDING_ACCEPT', 'PENDING_ACCEPT', 4, 'WORKER', '分配给组员冯清运负责清运', NULL, DATE_SUB(NOW(), INTERVAL 68 HOUR)),
(2, 'WO202610220002', 'ACCEPT', 'PENDING_ACCEPT', 'PROCESSING', 27, 'WORKER', '组员接单', NULL, DATE_SUB(NOW(), INTERVAL 44 HOUR)),
(2, 'WO202610220002', 'PROCESS', 'PROCESSING', 'PROCESSING', 27, 'WORKER', '已完成部分建筑垃圾清运，正在联系车辆清运剩余水泥块。', '14', DATE_SUB(NOW(), INTERVAL 40 HOUR)),
(3, 'WO202610220003', 'CREATE', NULL, 'PENDING_ACCEPT', 1, 'ADMIN', '审核通过，创建工单并派单给道路组长李道路', NULL, DATE_SUB(NOW(), INTERVAL 94 HOUR)),
(3, 'WO202610220003', 'ASSIGN', 'PENDING_ACCEPT', 'PENDING_ACCEPT', 2, 'WORKER', '紧急工单，分配给组员陈路桥处理', NULL, DATE_SUB(NOW(), INTERVAL 70 HOUR)),
(3, 'WO202610220003', 'ACCEPT', 'PENDING_ACCEPT', 'PROCESSING', 21, 'WORKER', '组员接单', NULL, DATE_SUB(NOW(), INTERVAL 68 HOUR)),
(3, 'WO202610220003', 'PROCESS', 'PROCESSING', 'PROCESSING', 21, 'WORKER', '已设置警示围挡，正在更换井盖。', NULL, DATE_SUB(NOW(), INTERVAL 50 HOUR)),
(3, 'WO202610220003', 'SUBMIT', 'PROCESSING', 'PENDING_LEADER_REVIEW', 21, 'WORKER', '井盖已更换并填实，路面恢复平整，请组长复核。', '15', DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(4, 'WO202610220004', 'CREATE', NULL, 'PENDING_ACCEPT', 1, 'ADMIN', '审核通过，创建工单并派单给照明组长王照明', NULL, DATE_SUB(NOW(), INTERVAL 93 HOUR)),
(4, 'WO202610220004', 'ACCEPT', 'PENDING_ACCEPT', 'PROCESSING', 3, 'WORKER', '组长本人接单处理', NULL, DATE_SUB(NOW(), INTERVAL 92 HOUR)),
(4, 'WO202610220004', 'PROCESS', 'PROCESSING', 'PROCESSING', 3, 'WORKER', '已排查线路，更换损坏的空开与两盏灯具。', '17', DATE_SUB(NOW(), INTERVAL 36 HOUR)),
(4, 'WO202610220004', 'SUBMIT', 'PROCESSING', 'PENDING_ACCEPTANCE', 3, 'WORKER', '路段路灯已恢复照明，夜间复查亮度正常。', '16', DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(5, 'WO202610220005', 'CREATE', NULL, 'PENDING_ACCEPT', 1, 'ADMIN', '审核通过，创建工单并派单给道路组长李道路', NULL, DATE_SUB(NOW(), INTERVAL 118 HOUR)),
(5, 'WO202610220005', 'ASSIGN', 'PENDING_ACCEPT', 'PENDING_ACCEPT', 2, 'WORKER', '分配给组员刘井盖补齐盖板', NULL, DATE_SUB(NOW(), INTERVAL 112 HOUR)),
(5, 'WO202610220005', 'ACCEPT', 'PENDING_ACCEPT', 'PROCESSING', 22, 'WORKER', '组员接单', NULL, DATE_SUB(NOW(), INTERVAL 110 HOUR)),
(5, 'WO202610220005', 'PROCESS', 'PROCESSING', 'PROCESSING', 22, 'WORKER', '已订购同规格盖板，清理沟内积水。', NULL, DATE_SUB(NOW(), INTERVAL 60 HOUR)),
(5, 'WO202610220005', 'SUBMIT', 'PROCESSING', 'PENDING_LEADER_REVIEW', 22, 'WORKER', '盖板已补齐并固定，沟内积水已清理。', '18', DATE_SUB(NOW(), INTERVAL 30 HOUR)),
(5, 'WO202610220005', 'LEADER_PASS', 'PENDING_LEADER_REVIEW', 'PENDING_ACCEPTANCE', 2, 'WORKER', '现场盖板已补齐，照片可见沟面平整，复核通过', NULL, DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(6, 'WO202610220006', 'CREATE', NULL, 'PENDING_ACCEPT', 1, 'ADMIN', '审核通过，创建工单并派单给设施组长赵设施', NULL, DATE_SUB(NOW(), INTERVAL 94 HOUR)),
(6, 'WO202610220006', 'ASSIGN', 'PENDING_ACCEPT', 'PENDING_ACCEPT', 5, 'WORKER', '分配给组员沈栈道维修护栏', NULL, DATE_SUB(NOW(), INTERVAL 93 HOUR)),
(6, 'WO202610220006', 'ACCEPT', 'PENDING_ACCEPT', 'PROCESSING', 31, 'WORKER', '组员接单', NULL, DATE_SUB(NOW(), INTERVAL 90 HOUR)),
(6, 'WO202610220006', 'PROCESS', 'PROCESSING', 'PROCESSING', 31, 'WORKER', '已拆除断裂护栏，正在安装新木栏。', '19', DATE_SUB(NOW(), INTERVAL 72 HOUR)),
(6, 'WO202610220006', 'SUBMIT', 'PROCESSING', 'PENDING_LEADER_REVIEW', 31, 'WORKER', '护栏已更换并加固，请复核。', '20', DATE_SUB(NOW(), INTERVAL 48 HOUR)),
(6, 'WO202610220006', 'LEADER_PASS', 'PENDING_LEADER_REVIEW', 'PENDING_ACCEPTANCE', 5, 'WORKER', '护栏已更换并加固，复核通过', NULL, DATE_SUB(NOW(), INTERVAL 36 HOUR)),
(6, 'WO202610220006', 'ACCEPT_PASS', 'PENDING_ACCEPTANCE', 'COMPLETED', 1, 'ADMIN', '栈道护栏已修复，现场复查无松动', NULL, DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(7, 'WO202610220007', 'CREATE', NULL, 'PENDING_ACCEPT', 1, 'ADMIN', '审核通过，创建工单并派单给环卫组长张环卫', NULL, DATE_SUB(NOW(), INTERVAL 94 HOUR)),
(7, 'WO202610220007', 'ASSIGN', 'PENDING_ACCEPT', 'PENDING_ACCEPT', 4, 'WORKER', '分配给组员褚保洁二次清运', NULL, DATE_SUB(NOW(), INTERVAL 93 HOUR)),
(7, 'WO202610220007', 'ACCEPT', 'PENDING_ACCEPT', 'PROCESSING', 28, 'WORKER', '组员接单', NULL, DATE_SUB(NOW(), INTERVAL 90 HOUR)),
(7, 'WO202610220007', 'PROCESS', 'PROCESSING', 'PROCESSING', 28, 'WORKER', '已清运残留垃圾，正在冲洗地面。', '21', DATE_SUB(NOW(), INTERVAL 60 HOUR)),
(7, 'WO202610220007', 'SUBMIT', 'PROCESSING', 'PENDING_LEADER_REVIEW', 28, 'WORKER', '垃圾已二次清运，地面已冲洗，请复核。', '22', DATE_SUB(NOW(), INTERVAL 30 HOUR)),
(7, 'WO202610220007', 'LEADER_PASS', 'PENDING_LEADER_REVIEW', 'PENDING_ACCEPTANCE', 4, 'WORKER', '已二次清运并冲洗，复核通过', NULL, DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(7, 'WO202610220007', 'ACCEPT_PASS', 'PENDING_ACCEPTANCE', 'COMPLETED', 1, 'ADMIN', '现场整洁，验收通过', NULL, DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(8, 'WO202610220008', 'CREATE', NULL, 'PENDING_ACCEPT', 1, 'ADMIN', '紧急工单，创建并派单给环卫组长张环卫', NULL, DATE_SUB(NOW(), INTERVAL 50 MINUTE));

-- ---------- 验收记录 ----------
INSERT INTO `t_work_order_acceptance`
(`work_order_id`, `work_order_no`, `acceptor_id`, `result`, `remark`, `create_time`)
VALUES
(6, 'WO202610220006', 1, 'PASS', '栈道护栏已修复，现场复查无松动', DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(7, 'WO202610220007', 1, 'PASS', '现场整洁，验收通过', DATE_SUB(NOW(), INTERVAL 24 HOUR));

-- ---------- 居民评分 ----------
INSERT INTO `t_work_rating`
(`id`, `work_order_id`, `work_order_no`, `worker_id`, `reporter_id`, `rating`, `tags`, `comment`, `create_time`)
VALUES
(1, 6, 'WO202610220006', 31, 11, 5, '响应快,处理彻底', '师傅第二天就来修好了护栏，还顺带检查了旁边几块木板，很负责。', DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(2, 7, 'WO202610220007', 28, 12, 3, '态度好', '垃圾基本清运了，但地面还是有一点残留水渍和味道。', DATE_SUB(NOW(), INTERVAL 20 HOUR));

-- ---------- 站内通知 ----------
USE city_event;
INSERT INTO `t_notification` (`user_id`, `title`, `content`, `biz_type`, `biz_id`, `is_read`, `create_time`)
VALUES
(4, '新的工单已派给你', '工单 WO202610220001（浦上大道河边垃圾堆积影响通行）待接单，请分配给组员或自行处理。', 'WORK_ORDER_ASSIGNED', 1, 0, DATE_SUB(NOW(), INTERVAL 46 HOUR)),
(27, '新的工单已派给你', '工单 WO202610220002（福新东路路边建筑垃圾长期堆放）已由组长分配给你，请及时处理。', 'WORK_ORDER_ASSIGNED', 2, 1, DATE_SUB(NOW(), INTERVAL 68 HOUR)),
(21, '新的工单已派给你', '工单 WO202610220003（秀山路井盖破损塌陷）已由组长分配给你，请及时处理。', 'WORK_ORDER_ASSIGNED', 3, 1, DATE_SUB(NOW(), INTERVAL 70 HOUR)),
(3, '新的工单已派给你', '工单 WO202610220004（小区门口路段路灯不亮）待接单，请及时处理。', 'WORK_ORDER_ASSIGNED', 4, 1, DATE_SUB(NOW(), INTERVAL 93 HOUR)),
(22, '新的工单已派给你', '工单 WO202610220005（儒江路排水沟盖板缺失）已由组长分配给你，请及时处理。', 'WORK_ORDER_ASSIGNED', 5, 1, DATE_SUB(NOW(), INTERVAL 112 HOUR)),
(31, '新的工单已派给你', '工单 WO202610220006（郑和路木栈道护栏断裂）已由组长分配给你，请及时处理。', 'WORK_ORDER_ASSIGNED', 6, 1, DATE_SUB(NOW(), INTERVAL 93 HOUR)),
(28, '新的工单已派给你', '工单 WO202610220007（广达路垃圾清运不彻底）已由组长分配给你，请及时处理。', 'WORK_ORDER_ASSIGNED', 7, 1, DATE_SUB(NOW(), INTERVAL 93 HOUR)),
(4, '紧急工单已派给你', '工单 WO202610220008（闽江大道河岸垃圾成堆散发异味）为紧急工单，请立即处理或分配。', 'WORK_ORDER_ASSIGNED', 8, 0, DATE_SUB(NOW(), INTERVAL 50 MINUTE));

-- ---------- AI 调用日志 ----------
USE city_ai;
INSERT INTO `t_ai_call_log`
(`id`, `trace_id`, `biz_type`, `user_id`, `model_name`, `prompt_version`, `request_summary`, `response_summary`, `parsed_ok`, `validation_result`, `latency_ms`, `prompt_tokens`, `completion_tokens`, `create_time`)
VALUES
(1, 'demo-trace-001', 'EVENT_ANALYZE', 11, 'glm-4-flash', 'v2',
 'hash=demo-ev01,length=64,images=1',
 '{"category":"ENVIRONMENT_SANITATION","priority":"HIGH","summary":"河岸步道旁多处生活垃圾堆积，部分散落水面。"}',
 1, NULL, 820, 1260, 168, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(2, 'demo-trace-002', 'EVENT_ANALYZE', 11, 'glm-4-flash', 'v2',
 'hash=demo-ev06,length=52,images=1',
 '{"category":"ENVIRONMENT_SANITATION","priority":"HIGH","summary":"河岸垃圾堆积占道，影响通行。"}',
 1, NULL, 790, 1180, 152, DATE_SUB(NOW(), INTERVAL 48 HOUR)),
(3, 'demo-trace-003', 'EVENT_ANALYZE', 13, 'glm-4-flash', 'v2',
 'hash=demo-ev13,length=58,images=1',
 '{"category":"ENVIRONMENT_SANITATION","priority":"URGENT","summary":"河岸垃圾成堆，异味明显。"}',
 1, NULL, 810, 1210, 160, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(4, 'demo-trace-004', 'EVENT_REVIEW', 11, 'glm-4-flash', 'v1',
 'category=ENVIRONMENT_SANITATION,priority=MEDIUM,photoCount=1,textLength=64',
 '{"priority":"HIGH","reason":"问题位于居民日常散步的河岸步道，垃圾已散落水面并有异味。","impact":"河岸步道约 200 米范围及下游水面","responseMinutes":480,"confidence":0.86}',
 1, NULL, 760, 980, 130, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(5, 'demo-trace-005', 'EVENT_REVIEW', 13, 'glm-4-flash', 'v1',
 'category=ROAD_FACILITY,priority=HIGH,photoCount=1,textLength=52',
 '{"priority":"URGENT","reason":"井盖缺失位于人行道且无警示，存在明确人身伤害风险。","impact":"朝阳路人行道行人及非机动车","responseMinutes":120,"confidence":0.94}',
 1, NULL, 720, 950, 126, DATE_SUB(NOW(), INTERVAL 40 MINUTE)),
(6, 'demo-trace-006', 'WORKORDER_RECTIFY', 21, 'glm-4.6v-flash', 'v1',
 'workOrderNo=WO202610220003,images=1,originalImages=1,textLength=28',
 '{"conclusion":"RECTIFIED","confidence":0.88,"reason":"整改后照片中破损井盖已更换，路面平整。"}',
 1, NULL, 4300, NULL, NULL, DATE_SUB(NOW(), INTERVAL 23 HOUR));

INSERT INTO `t_ai_analysis`
(`call_log_id`, `category_code`, `summary`, `priority`, `suggestion`, `reasoning`, `confidence`, `degraded`, `model_name`, `expire_time`, `create_time`)
VALUES
(1, 'ENVIRONMENT_SANITATION', '河岸步道旁多处生活垃圾堆积，部分散落水面。', 'HIGH',
 '安排环卫班组清运垃圾并冲洗地面',
 '照片显示河岸步道旁有塑料袋和成堆垃圾，属于环境卫生问题，靠近居民散步区域，影响较大。',
 0.880, 0, 'glm-4-flash', DATE_ADD(NOW(), INTERVAL 30 MINUTE), DATE_SUB(NOW(), INTERVAL 3 HOUR));

SET FOREIGN_KEY_CHECKS = 1;
