SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 增量演示数据：工人 12 名、事件 23 条、工单 13 个及配套流水
-- 可重复执行（按 id 段先删后插，不影响 reset-demo-data.sql 的原始数据）
-- 执行：mysql --host=127.0.0.1 --port=3307 --user=root --password=1234 \
--        --default-character-set=utf8mb4 -e "source seed-more-demo-data.sql"
-- ============================================================

-- ---------- 清理增量段 ----------
USE city_user;
DELETE FROM `t_user_role` WHERE `user_id` BETWEEN 33 AND 44;
DELETE FROM `t_worker_profile` WHERE `id` BETWEEN 17 AND 28;
DELETE FROM `t_user` WHERE `id` BETWEEN 33 AND 44;

USE city_event;
DELETE FROM `t_notification` WHERE `biz_type` = 'WORK_ORDER_ASSIGNED' AND `biz_id` BETWEEN 9 AND 21;
DELETE FROM `t_attachment` WHERE `id` BETWEEN 23 AND 200;
DELETE FROM `t_event_status_log` WHERE `event_id` BETWEEN 14 AND 36;
DELETE FROM `t_event_audit_log` WHERE `event_id` BETWEEN 14 AND 36;
DELETE FROM `t_event_ai_snapshot` WHERE `event_id` BETWEEN 14 AND 36;
DELETE FROM `t_event` WHERE `id` BETWEEN 14 AND 36;

USE city_workorder;
DELETE FROM `t_work_order_log` WHERE `work_order_id` BETWEEN 9 AND 21;
DELETE FROM `t_work_order_acceptance` WHERE `work_order_id` BETWEEN 9 AND 21;
DELETE FROM `t_work_rating` WHERE `id` BETWEEN 3 AND 20;
DELETE FROM `t_work_order` WHERE `id` BETWEEN 9 AND 21;

USE city_ai;
DELETE FROM `t_ai_call_log` WHERE `id` BETWEEN 7 AND 30;

-- ---------- 新增处理人员（12 名组员，4 个班组各 3 名） ----------
USE city_user;
INSERT INTO `t_user` (`id`, `username`, `password_hash`, `nickname`, `phone`, `status`)
VALUES
(33, 'worker_road_04', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '王路面', '13800100033', 'ENABLED'),
(34, 'worker_road_05', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '李检修', '13800100034', 'ENABLED'),
(35, 'worker_road_06', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '张坑槽', '13800100035', 'ENABLED'),
(36, 'worker_lighting_04', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '林灯控', '13800100036', 'ENABLED'),
(37, 'worker_lighting_05', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '黄配电', '13800100037', 'ENABLED'),
(38, 'worker_lighting_06', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '陈抢修', '13800100038', 'ENABLED'),
(39, 'worker_sanitation_04', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '许清运', '13800100039', 'ENABLED'),
(40, 'worker_sanitation_05', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '何保洁', '13800100040', 'ENABLED'),
(41, 'worker_sanitation_06', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '邓冲洗', '13800100041', 'ENABLED'),
(42, 'worker_facility_04', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '郑排水', '13800100042', 'ENABLED'),
(43, 'worker_facility_05', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '谢栈道', '13800100043', 'ENABLED'),
(44, 'worker_facility_06', '$2a$10$R90uQ24mMXirzkvsJcbDhO9T7qT3D0dWAEoHEeUywXbHaiz60bq8a', '曾座椅', '13800100044', 'ENABLED');

INSERT INTO `t_user_role` (`user_id`, `role_code`)
VALUES (33, 'WORKER'), (34, 'WORKER'), (35, 'WORKER'), (36, 'WORKER'), (37, 'WORKER'), (38, 'WORKER'), (39, 'WORKER'), (40, 'WORKER'), (41, 'WORKER'), (42, 'WORKER'), (43, 'WORKER'), (44, 'WORKER');

INSERT INTO `t_worker_profile`
(`id`, `user_id`, `worker_type`, `leader_id`, `dept_code`, `dept_name`, `area_code`, `work_zone`, `zone_lng`, `zone_lat`, `skill_tags`, `status`, `create_by`)
VALUES
(17, 33, 'MEMBER', 2, 'ROAD', '市政道路班组', '350111', '晋安区岳峰片区', 119.334000, 26.090000, '路面,坑槽', 'ON_DUTY', 1),
(18, 34, 'MEMBER', 2, 'ROAD', '市政道路班组', '350102', '鼓楼区南片区', 119.288000, 26.070000, '路面,护栏', 'ON_DUTY', 1),
(19, 35, 'MEMBER', 2, 'ROAD', '市政道路班组', '350104', '仓山区上渡片区', 119.282000, 26.036000, '井盖,路面', 'ON_DUTY', 1),
(20, 36, 'MEMBER', 3, 'LIGHTING', '路灯照明班组', '350102', '鼓楼区西片区', 119.290000, 26.080000, '路灯,控制箱', 'ON_DUTY', 1),
(21, 37, 'MEMBER', 3, 'LIGHTING', '路灯照明班组', '350103', '台江区南片区', 119.302000, 26.050000, '配电箱,线路', 'ON_DUTY', 1),
(22, 38, 'MEMBER', 3, 'LIGHTING', '路灯照明班组', '350111', '晋安区东片区', 119.350000, 26.070000, '抢修,灯具', 'ON_DUTY', 1),
(23, 39, 'MEMBER', 4, 'SANITATION', '环卫保洁班组', '350104', '仓山区下渡片区', 119.276000, 26.028000, '垃圾清运,消杀', 'ON_DUTY', 1),
(24, 40, 'MEMBER', 4, 'SANITATION', '环卫保洁班组', '350103', '台江区瀛洲片区', 119.312000, 26.056000, '保洁,冲洗', 'ON_DUTY', 1),
(25, 41, 'MEMBER', 4, 'SANITATION', '环卫保洁班组', '350111', '晋安区茶园片区', 119.320000, 26.100000, '冲洗,河道保洁', 'ON_DUTY', 1),
(26, 42, 'MEMBER', 5, 'FACILITY', '公共设施班组', '350103', '台江区宁化片区', 119.300000, 26.052000, '排水口,管道', 'ON_DUTY', 1),
(27, 43, 'MEMBER', 5, 'FACILITY', '公共设施班组', '350112', '长乐区吴航片区', 119.520000, 25.966000, '木栈道,护栏', 'ON_DUTY', 1),
(28, 44, 'MEMBER', 5, 'FACILITY', '公共设施班组', '350105', '马尾区罗星片区', 119.452000, 25.992000, '座椅,公共设施', 'ON_DUTY', 1);

-- ---------- 新增事件（23 条，覆盖六城区与 7 种状态） ----------
USE city_event;
INSERT INTO `t_event`
(`id`, `event_no`, `reporter_id`, `category_code`, `ai_category_code`, `title`, `description`, `summary`, `priority`, `ai_priority`, `ai_confidence`, `ai_suggestion`,
 `ai_review_priority`, `ai_review_reason`, `ai_review_impact`, `ai_review_response_minutes`, `ai_review_confidence`, `ai_review_time`,
 `status`, `province`, `city`, `district`, `street`, `area_code`, `address`, `longitude`, `latitude`, `photo_count`, `auditor_id`, `audit_time`, `reject_reason`, `dispatch_time`, `close_time`, `create_time`, `update_time`, `create_by`)
VALUES
(14, 'EV202610250024', 15, 'ENVIRONMENT_SANITATION', 'ENVIRONMENT_SANITATION', '茶园路人行道果皮箱满溢',
 '晋安区茶园街道茶园路人行道果皮箱已满溢，果皮和纸盒散落至人行道，气味刺鼻。',
 '人行道果皮箱满溢，垃圾散落。', 'MEDIUM', 'HIGH', 0.860, '当日清运并冲洗果皮箱周边地面',
 'HIGH', '果皮箱满溢位于人行道且紧邻公交站，夏季异味扩散快，建议 8 小时内清运。', '茶园路人行道约 30 米及公交站候车区', 480, 0.840, DATE_SUB(NOW(), INTERVAL 1 HOUR),
 'PENDING_REVIEW', '福建省', '福州市', '晋安区', '茶园街道', '350111', '福建省福州市晋安区茶园街道茶园路人行道',
 119.320000, 26.100000, 1, NULL, NULL, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 2 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR), 15),
(15, 'EV202610250025', 11, 'PUBLIC_LIGHTING', 'PUBLIC_LIGHTING', '东街口地铁站出口路灯闪烁',
 '鼓楼区东街街道东街口地铁站 C 出口外三盏路灯持续闪烁，夜间照明不稳定。',
 '地铁口三盏路灯闪烁，照明不稳。', 'MEDIUM', 'MEDIUM', 0.830, '检查镇流器与线路接头，必要时更换灯具',
 'MEDIUM', '路灯闪烁影响夜间通行，但未完全熄灭，建议 24 小时内检修。', '东街口地铁站 C 出口外约 50 米', 1440, 0.800, DATE_SUB(NOW(), INTERVAL 2 HOUR),
 'PENDING_REVIEW', '福建省', '福州市', '鼓楼区', '东街街道', '350102', '福建省福州市鼓楼区东街街道东街口地铁站 C 出口',
 119.303000, 26.083000, 1, NULL, NULL, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 3 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR), 11),
(16, 'EV202610250026', 13, 'ROAD_FACILITY', 'ROAD_FACILITY', '金山大道非机动车道路面凹陷',
 '仓山区金山街道金山大道非机动车道出现约一米长路面凹陷，雨后积水，骑行易打滑。',
 '非机动车道路面凹陷积水。', 'HIGH', 'HIGH', 0.880, '切割修补凹陷路面并恢复排水坡度',
 'HIGH', '凹陷处雨后积水，非机动车骑行易打滑，建议 8 小时内修补。', '金山大道非机动车道约 1 米范围', 480, 0.870, DATE_SUB(NOW(), INTERVAL 4 HOUR),
 'PENDING_REVIEW', '福建省', '福州市', '仓山区', '金山街道', '350104', '福建省福州市仓山区金山街道金山大道非机动车道',
 119.273000, 26.030000, 1, NULL, NULL, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_SUB(NOW(), INTERVAL 4 HOUR), 13),
(17, 'EV202610250027', 12, 'PUBLIC_FACILITY', 'PUBLIC_FACILITY', '滨江公园休闲座椅木板断裂',
 '台江区宁化街道滨江公园内一张休闲座椅木板断裂翘起，坐面有铁钉外露。',
 '公园座椅木板断裂，铁钉外露。', 'MEDIUM', 'HIGH', 0.900, '拆除破损座椅并更换木板，排查同区域座椅',
 'HIGH', '座椅铁钉外露且位于公园活动区，存在划伤风险，建议 8 小时内处理。', '滨江公园亲水平台周边 3 张座椅', 480, 0.890, DATE_SUB(NOW(), INTERVAL 5 HOUR),
 'PENDING_REVIEW', '福建省', '福州市', '台江区', '宁化街道', '350103', '福建省福州市台江区宁化街道滨江公园亲水平台',
 119.300000, 26.052000, 1, NULL, NULL, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 5 HOUR), 12),
(18, 'EV202610250028', 13, 'ROAD_FACILITY', 'ROAD_FACILITY', '吴航街道人行道地砖松动',
 '长乐区吴航街道郑和路与爱心路交叉口人行道地砖大面积松动，雨天踩踏溅水。',
 '人行道地砖松动，雨天溅水。', 'MEDIUM', 'MEDIUM', 0.790, '重新铺设松动地砖并夯实基础',
 'MEDIUM', '地砖松动影响步行体验，无直接伤害风险，建议 24 小时内修复。', '郑和路与爱心路交叉口人行道约 20 平方米', 1440, 0.760, DATE_SUB(NOW(), INTERVAL 7 HOUR),
 'PENDING_REVIEW', '福建省', '福州市', '长乐区', '吴航街道', '350112', '福建省福州市长乐区长乐区吴航街道郑和路与爱心路交叉口',
 119.523000, 25.963000, 1, NULL, NULL, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 8 HOUR), DATE_SUB(NOW(), INTERVAL 7 HOUR), 13),
(19, 'EV202610250029', 14, 'ENVIRONMENT_SANITATION', 'ENVIRONMENT_SANITATION', '罗星塔公园外侧垃圾房污水外流',
 '马尾区罗星街道罗星塔公园外侧垃圾房污水沿路面外流，异味明显，行人绕行。',
 '垃圾房污水外流，异味明显。', 'HIGH', 'HIGH', 0.870, '抽排污水并冲洗路面，检查垃圾房排水口',
 'HIGH', '污水外流至人行道并散发异味，影响行人通行，建议 8 小时内处理。', '罗星塔公园外侧人行道约 15 米', 480, 0.850, DATE_SUB(NOW(), INTERVAL 8 HOUR),
 'PENDING_REVIEW', '福建省', '福州市', '马尾区', '罗星街道', '350105', '福建省福州市马尾区罗星街道罗星塔公园外侧',
 119.455000, 25.990000, 1, NULL, NULL, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 9 HOUR), DATE_SUB(NOW(), INTERVAL 8 HOUR), 14),
(20, 'EV202610250030', 11, 'ENVIRONMENT_SANITATION', 'ENVIRONMENT_SANITATION', '某小区垃圾桶旁堆放旧家具',
 '鼓楼区洪山镇某小区垃圾桶旁堆放旧沙发和床垫，已放置多日无人清运。',
 '垃圾桶旁堆放旧家具。', 'MEDIUM', 'MEDIUM', 0.740, '联系大件垃圾清运车集中收运',
 'MEDIUM', '大件垃圾占用人行通道，无即时安全风险，建议 24 小时内清运。', '小区外侧人行通道约 8 米', 1440, 0.720, DATE_SUB(NOW(), INTERVAL 27 HOUR),
 'REJECTED', '福建省', '福州市', '鼓楼区', '洪山镇', '350102', '福建省福州市鼓楼区洪山镇梅峰路某小区外侧',
 119.270000, 26.080000, 1, 1, DATE_SUB(NOW(), INTERVAL 27 HOUR), '照片未拍到小区名称与门牌，无法确认归属，请补充带地标的照片后重新提交', NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 28 HOUR), DATE_SUB(NOW(), INTERVAL 27 HOUR), 11),
(21, 'EV202610250031', 15, 'PUBLIC_FACILITY', 'PUBLIC_FACILITY', '晋安河公园护栏油漆脱落',
 '晋安区岳峰镇晋安河公园沿河护栏油漆大面积脱落，露出锈蚀钢面。',
 '沿河护栏油漆脱落锈蚀。', 'LOW', 'LOW', 0.700, '除锈并重新刷防锈漆',
 'LOW', '护栏结构完好，仅外观锈蚀，建议纳入月度维护计划。', '晋安河公园沿河护栏约 40 米', 4320, 0.680, DATE_SUB(NOW(), INTERVAL 29 HOUR),
 'REJECTED', '福建省', '福州市', '晋安区', '岳峰镇', '350111', '福建省福州市晋安区岳峰镇晋安河公园沿河步道',
 119.331000, 26.089000, 1, 1, DATE_SUB(NOW(), INTERVAL 29 HOUR), '该问题属于日常养护范围，已转交公园管理单位按计划处理，本次不作事件派单', NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 30 HOUR), DATE_SUB(NOW(), INTERVAL 29 HOUR), 15),
(22, 'EV202610250032', 12, 'PUBLIC_LIGHTING', 'PUBLIC_LIGHTING', '茶亭公园路灯白天常亮',
 '台江区茶亭街道茶亭公园内两盏路灯白天持续点亮，疑似光控开关故障。',
 '公园路灯白天常亮，疑光控故障。', 'LOW', 'LOW', 0.760, '检查光控开关与定时器设置',
 'LOW', '白天亮灯造成电能浪费，无安全影响，建议 3 日内检修。', '茶亭公园内 2 盏路灯', 4320, 0.740, DATE_SUB(NOW(), INTERVAL 19 HOUR),
 'CANCELLED', '福建省', '福州市', '台江区', '茶亭街道', '350103', '福建省福州市台江区茶亭街道茶亭公园内',
 119.306000, 26.062000, 1, NULL, NULL, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 20 HOUR), DATE_SUB(NOW(), INTERVAL 19 HOUR), 12),
(23, 'EV202610250033', 13, 'ROAD_FACILITY', 'ROAD_FACILITY', '上渡路井盖异响',
 '仓山区上渡街道上渡路车辆碾过井盖时发出明显异响，井盖有轻微跳动。',
 '井盖异响并轻微跳动。', 'MEDIUM', 'MEDIUM', 0.780, '紧固井盖并加装减震胶圈',
 'MEDIUM', '井盖异响提示松动，存在移位风险，建议 24 小时内紧固。', '上渡路机动车道 1 处井盖', 1440, 0.770, DATE_SUB(NOW(), INTERVAL 17 HOUR),
 'CANCELLED', '福建省', '福州市', '仓山区', '上渡街道', '350104', '福建省福州市仓山区上渡街道上渡路机动车道',
 119.282000, 26.036000, 1, NULL, NULL, NULL, NULL, NULL,
 DATE_SUB(NOW(), INTERVAL 18 HOUR), DATE_SUB(NOW(), INTERVAL 17 HOUR), 13),
(24, 'EV202610250034', 11, 'ENVIRONMENT_SANITATION', 'ENVIRONMENT_SANITATION', '五四北路口绿化带内垃圾堆积',
 '鼓楼区温泉街道五四北路中央绿化带内堆积塑料袋、饮料瓶和落叶，影响市容。',
 '绿化带内垃圾堆积。', 'MEDIUM', 'MEDIUM', 0.810, '清理绿化带垃圾并加强巡回保洁',
 'MEDIUM', '绿化带垃圾影响市容，无安全风险，建议 24 小时内清理。', '五四北路中央绿化带约 60 米', 1440, 0.790, DATE_SUB(NOW(), INTERVAL 5 HOUR),
 'DISPATCHED', '福建省', '福州市', '鼓楼区', '温泉街道', '350102', '福建省福州市鼓楼区温泉街道五四北路中央绿化带',
 119.312000, 26.095000, 1, 1, DATE_SUB(NOW(), INTERVAL 5 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 5 HOUR), NULL,
 DATE_SUB(NOW(), INTERVAL 6 HOUR), DATE_SUB(NOW(), INTERVAL 5 HOUR), 11),
(25, 'EV202610250035', 15, 'ROAD_FACILITY', 'ROAD_FACILITY', '福马路人行道盲道砖破损',
 '晋安区鼓山镇福马路人行道盲道砖多处破损缺失，视障人士通行存在障碍。',
 '盲道砖破损缺失。', 'HIGH', 'HIGH', 0.890, '更换破损盲道砖并保持走向连续',
 'HIGH', '盲道破损直接影响视障人士通行安全，建议 8 小时内更换。', '福马路人行道盲道约 15 米', 480, 0.880, DATE_SUB(NOW(), INTERVAL 9 HOUR),
 'DISPATCHED', '福建省', '福州市', '晋安区', '鼓山镇', '350111', '福建省福州市晋安区鼓山镇福马路人行道',
 119.345000, 26.070000, 1, 1, DATE_SUB(NOW(), INTERVAL 9 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 9 HOUR), NULL,
 DATE_SUB(NOW(), INTERVAL 10 HOUR), DATE_SUB(NOW(), INTERVAL 9 HOUR), 15),
(26, 'EV202610250036', 14, 'PUBLIC_FACILITY', 'PUBLIC_FACILITY', '儒江路人行道隔离墩倾斜',
 '马尾区马尾镇儒江路人行道一处石质隔离墩倾斜，底部与地面脱开，容易倾倒。',
 '人行道隔离墩倾斜松动。', 'MEDIUM', 'HIGH', 0.840, '重新固定隔离墩或更换为防撞柱',
 'HIGH', '隔离墩松动可能倾倒砸伤行人，建议 8 小时内加固。', '儒江路人行道约 5 米范围', 480, 0.830, DATE_SUB(NOW(), INTERVAL 11 HOUR),
 'DISPATCHED', '福建省', '福州市', '马尾区', '马尾镇', '350105', '福建省福州市马尾区马尾镇儒江路人行道',
 119.448000, 25.998000, 1, 1, DATE_SUB(NOW(), INTERVAL 11 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 11 HOUR), NULL,
 DATE_SUB(NOW(), INTERVAL 12 HOUR), DATE_SUB(NOW(), INTERVAL 11 HOUR), 14),
(27, 'EV202610250037', 13, 'ENVIRONMENT_SANITATION', 'ENVIRONMENT_SANITATION', '金山工业园区垃圾集中点异味',
 '仓山区金山街道金山工业园区垃圾集中点未及时清运，餐厨垃圾发酵产生强烈异味。',
 '园区垃圾集中点异味强烈。', 'HIGH', 'URGENT', 0.910, '立即清运并冲洗消杀，调整清运频次',
 'URGENT', '餐厨垃圾发酵异味扩散至周边厂区，影响范围大，建议 2 小时内清运。', '金山工业园区 3 栋厂房及周边道路', 120, 0.900, DATE_SUB(NOW(), INTERVAL 29 HOUR),
 'PROCESSING', '福建省', '福州市', '仓山区', '金山街道', '350104', '福建省福州市仓山区金山街道金山工业园区垃圾集中点',
 119.276000, 26.028000, 1, 1, DATE_SUB(NOW(), INTERVAL 29 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 29 HOUR), NULL,
 DATE_SUB(NOW(), INTERVAL 30 HOUR), DATE_SUB(NOW(), INTERVAL 29 HOUR), 13),
(28, 'EV202610250038', 12, 'ROAD_FACILITY', 'ROAD_FACILITY', '宁化支路人行道护栏缺失',
 '台江区宁化街道宁化支路人行道外侧护栏缺失一段约两米，缺口紧邻机动车道。',
 '人行道护栏缺失约两米。', 'HIGH', 'URGENT', 0.900, '立即设置临时围挡并补装护栏',
 'URGENT', '护栏缺口紧邻机动车道，行人尤其是儿童有跌落车道风险，建议 2 小时内围挡。', '宁化支路人行道约 2 米缺口', 120, 0.910, DATE_SUB(NOW(), INTERVAL 25 HOUR),
 'PROCESSING', '福建省', '福州市', '台江区', '宁化街道', '350103', '福建省福州市台江区宁化街道宁化支路人行道',
 119.300000, 26.052000, 1, 1, DATE_SUB(NOW(), INTERVAL 25 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 25 HOUR), NULL,
 DATE_SUB(NOW(), INTERVAL 26 HOUR), DATE_SUB(NOW(), INTERVAL 25 HOUR), 12),
(29, 'EV202610250039', 11, 'PUBLIC_LIGHTING', 'PUBLIC_LIGHTING', '航城路一段路灯整排不亮',
 '长乐区航城街道航城路东段约 300 米路灯整排不亮，夜间道路一片漆黑。',
 '航城路东段整排路灯不亮。', 'HIGH', 'HIGH', 0.880, '排查箱变与主线路，恢复供电',
 'HIGH', '整排路灯不亮导致路段夜间无照明，存在治安与交通安全风险，建议 8 小时内恢复。', '航城路东段约 300 米道路', 480, 0.870, DATE_SUB(NOW(), INTERVAL 33 HOUR),
 'PROCESSING', '福建省', '福州市', '长乐区', '航城街道', '350112', '福建省福州市长乐区航城街道航城路东段',
 119.515000, 25.970000, 1, 1, DATE_SUB(NOW(), INTERVAL 33 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 33 HOUR), NULL,
 DATE_SUB(NOW(), INTERVAL 34 HOUR), DATE_SUB(NOW(), INTERVAL 33 HOUR), 11),
(30, 'EV202610250040', 15, 'ENVIRONMENT_SANITATION', 'ENVIRONMENT_SANITATION', '晋安河支流河面漂浮油污',
 '晋安区茶园街道晋安河支流河面出现带状油污，随水流向下游扩散，岸边有异味。',
 '河面出现带状油污。', 'HIGH', 'URGENT', 0.920, '设置围油栏并溯源排查排口，组织打捞',
 'URGENT', '油污随水流扩散且可能来自偷排，影响河道水质，建议 2 小时内处置并溯源。', '晋安河支流约 300 米河段及下游', 120, 0.910, DATE_SUB(NOW(), INTERVAL 51 HOUR),
 'PENDING_ACCEPTANCE', '福建省', '福州市', '晋安区', '茶园街道', '350111', '福建省福州市晋安区茶园街道晋安河支流河段',
 119.320000, 26.100000, 1, 1, DATE_SUB(NOW(), INTERVAL 51 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 51 HOUR), NULL,
 DATE_SUB(NOW(), INTERVAL 52 HOUR), DATE_SUB(NOW(), INTERVAL 51 HOUR), 15),
(31, 'EV202610250041', 11, 'ROAD_FACILITY', 'ROAD_FACILITY', '梅峰路人行道树池篦子翘起',
 '鼓楼区洪山镇梅峰路人行道树池篦子翘起约十厘米，行人易被绊倒。',
 '树池篦子翘起易绊人。', 'MEDIUM', 'HIGH', 0.850, '复位并固定树池篦子，检查周边同类问题',
 'HIGH', '篦子翘起形成绊倒隐患，人行道行人密集，建议 8 小时内固定。', '梅峰路人行道 1 处树池', 480, 0.840, DATE_SUB(NOW(), INTERVAL 47 HOUR),
 'PENDING_ACCEPTANCE', '福建省', '福州市', '鼓楼区', '洪山镇', '350102', '福建省福州市鼓楼区洪山镇梅峰路人行道',
 119.270000, 26.080000, 1, 1, DATE_SUB(NOW(), INTERVAL 47 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 47 HOUR), NULL,
 DATE_SUB(NOW(), INTERVAL 48 HOUR), DATE_SUB(NOW(), INTERVAL 47 HOUR), 11),
(32, 'EV202610250042', 13, 'PUBLIC_FACILITY', 'PUBLIC_FACILITY', '上渡路人行天桥栏杆松动',
 '仓山区上渡街道上渡路人行天桥一段栏杆固定螺栓松动，推拉时有明显晃动。',
 '天桥栏杆螺栓松动晃动。', 'HIGH', 'URGENT', 0.930, '立即封闭该段并紧固更换螺栓',
 'URGENT', '天桥栏杆松动存在坠落风险，人流量大，建议 2 小时内封闭加固。', '上渡路人行天桥东侧一段栏杆', 120, 0.920, DATE_SUB(NOW(), INTERVAL 43 HOUR),
 'PENDING_ACCEPTANCE', '福建省', '福州市', '仓山区', '上渡街道', '350104', '福建省福州市仓山区上渡街道上渡路人行天桥',
 119.282000, 26.036000, 1, 1, DATE_SUB(NOW(), INTERVAL 43 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 43 HOUR), NULL,
 DATE_SUB(NOW(), INTERVAL 44 HOUR), DATE_SUB(NOW(), INTERVAL 43 HOUR), 13),
(33, 'EV202610250043', 12, 'ENVIRONMENT_SANITATION', 'ENVIRONMENT_SANITATION', '瀛洲路夜市收摊后地面油污',
 '台江区瀛洲街道瀛洲路夜市收摊后地面残留大量油污和竹签，行人踩踏打滑。',
 '夜市收摊后地面油污。', 'MEDIUM', 'MEDIUM', 0.800, '高压冲洗地面并督促摊主铺设防油垫',
 'MEDIUM', '地面油污影响行人通行，建议 24 小时内冲洗。', '瀛洲路夜市段人行道约 80 米', 1440, 0.790, DATE_SUB(NOW(), INTERVAL 95 HOUR),
 'CLOSED', '福建省', '福州市', '台江区', '瀛洲街道', '350103', '福建省福州市台江区瀛洲街道瀛洲路夜市段',
 119.312000, 26.056000, 1, 1, DATE_SUB(NOW(), INTERVAL 95 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 95 HOUR), DATE_SUB(NOW(), INTERVAL 72 HOUR),
 DATE_SUB(NOW(), INTERVAL 96 HOUR), DATE_SUB(NOW(), INTERVAL 95 HOUR), 12),
(34, 'EV202610250044', 14, 'ROAD_FACILITY', 'ROAD_FACILITY', '罗星路路面坑槽影响行车',
 '马尾区罗星街道罗星路机动车道出现两处坑槽，深度约十厘米，车辆避让频繁。',
 '机动车道两处坑槽较深。', 'HIGH', 'HIGH', 0.880, '冷补料临时修补并安排沥青摊铺',
 'HIGH', '坑槽较深且位于机动车道，易造成轮胎损伤，建议 8 小时内修补。', '罗星路机动车道两处坑槽', 480, 0.870, DATE_SUB(NOW(), INTERVAL 119 HOUR),
 'CLOSED', '福建省', '福州市', '马尾区', '罗星街道', '350105', '福建省福州市马尾区罗星街道罗星路机动车道',
 119.452000, 25.992000, 1, 1, DATE_SUB(NOW(), INTERVAL 119 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 119 HOUR), DATE_SUB(NOW(), INTERVAL 96 HOUR),
 DATE_SUB(NOW(), INTERVAL 120 HOUR), DATE_SUB(NOW(), INTERVAL 119 HOUR), 14),
(35, 'EV202610250045', 15, 'PUBLIC_LIGHTING', 'PUBLIC_LIGHTING', '岳峰路人行道灯长时间不亮',
 '晋安区岳峰镇岳峰路人行道庭院灯连续一周不亮，夜间居民散步视线差。',
 '人行道庭院灯连续一周不亮。', 'MEDIUM', 'HIGH', 0.860, '更换庭院灯电源模块并排查线路',
 'HIGH', '连续一周不亮且位于居民散步路段，建议 8 小时内修复。', '岳峰路人行道约 200 米', 480, 0.850, DATE_SUB(NOW(), INTERVAL 143 HOUR),
 'CLOSED', '福建省', '福州市', '晋安区', '岳峰镇', '350111', '福建省福州市晋安区岳峰镇岳峰路人行道',
 119.334000, 26.090000, 1, 1, DATE_SUB(NOW(), INTERVAL 143 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 143 HOUR), DATE_SUB(NOW(), INTERVAL 120 HOUR),
 DATE_SUB(NOW(), INTERVAL 144 HOUR), DATE_SUB(NOW(), INTERVAL 143 HOUR), 15),
(36, 'EV202610250046', 11, 'PUBLIC_FACILITY', 'PUBLIC_FACILITY', '温泉公园公厕无障碍扶手松动',
 '鼓楼区温泉街道温泉公园公厕无障碍扶手固定件松动，扶手可左右晃动。',
 '公厕无障碍扶手松动。', 'MEDIUM', 'HIGH', 0.900, '更换膨胀螺栓重新固定扶手',
 'HIGH', '无障碍扶手松动无法借力，存在跌倒风险，建议 8 小时内修复。', '温泉公园公厕无障碍间 1 处', 480, 0.890, DATE_SUB(NOW(), INTERVAL 159 HOUR),
 'CLOSED', '福建省', '福州市', '鼓楼区', '温泉街道', '350102', '福建省福州市鼓楼区温泉街道温泉公园公厕',
 119.312000, 26.090000, 1, 1, DATE_SUB(NOW(), INTERVAL 159 HOUR), NULL, DATE_SUB(NOW(), INTERVAL 159 HOUR), DATE_SUB(NOW(), INTERVAL 136 HOUR),
 DATE_SUB(NOW(), INTERVAL 160 HOUR), DATE_SUB(NOW(), INTERVAL 159 HOUR), 11);

-- ---------- 事件上报照片（复制 demo 图片为唯一文件名） ----------
INSERT INTO `t_attachment`
(`id`, `biz_type`, `biz_id`, `owner_id`, `original_name`, `stored_name`, `file_path`, `access_url`, `content_type`, `file_size`, `sha256`, `status`, `create_time`)
VALUES
(23, 'EVENT_REPORT', 14, 15, '茶园路人行道果皮箱满溢.png', 'seed-event-report-23.png', 'demo/seed-event-report-23.png', '/uploads/demo/seed-event-report-23.png', 'image/png', 337991, '38b19263c346003df135d8f4bf1496684f647c72c94280a4181d8e24f93544d6', 'BOUND', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(24, 'EVENT_REPORT', 15, 11, '东街口地铁站出口路灯闪烁.png', 'seed-event-report-24.png', 'demo/seed-event-report-24.png', '/uploads/demo/seed-event-report-24.png', 'image/png', 828491, '8f6efe4dafcc3f343f7b1c2d5c47c5c45b1f2cc85f21c9c842175aafb99c2406', 'BOUND', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(25, 'EVENT_REPORT', 16, 13, '金山大道非机动车道路面凹陷.png', 'seed-event-report-25.png', 'demo/seed-event-report-25.png', '/uploads/demo/seed-event-report-25.png', 'image/png', 1131040, 'a9c110438518572d2a020f15c733787b7f123d915f5952d415fd5d7e49546a80', 'BOUND', DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(26, 'EVENT_REPORT', 17, 12, '滨江公园休闲座椅木板断裂.png', 'seed-event-report-26.png', 'demo/seed-event-report-26.png', '/uploads/demo/seed-event-report-26.png', 'image/png', 711211, '824ed4772e45cd3dc1362902b94a9fc28accba766a5e12f4dec809429dc44feb', 'BOUND', DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(27, 'EVENT_REPORT', 18, 13, '吴航街道人行道地砖松动.png', 'seed-event-report-27.png', 'demo/seed-event-report-27.png', '/uploads/demo/seed-event-report-27.png', 'image/png', 736134, '6abcab66fa997ec9edb58322dda961c3688bdf5f470c82a642e639fe8cf35d0a', 'BOUND', DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(28, 'EVENT_REPORT', 19, 14, '罗星塔公园外侧垃圾房污水外流.png', 'seed-event-report-28.png', 'demo/seed-event-report-28.png', '/uploads/demo/seed-event-report-28.png', 'image/png', 887874, 'f2c7fb48c75497fde9f3db7fee9976f7a06b87a7cd4b5705fb6c7cc3c4cbad1f', 'BOUND', DATE_SUB(NOW(), INTERVAL 9 HOUR)),
(29, 'EVENT_REPORT', 20, 11, '某小区垃圾桶旁堆放旧家具.png', 'seed-event-report-29.png', 'demo/seed-event-report-29.png', '/uploads/demo/seed-event-report-29.png', 'image/png', 98319, 'bde69ee33d9b203a5a8959e77205c3b5d288bf6f84014e5907fb50baf10fc489', 'BOUND', DATE_SUB(NOW(), INTERVAL 28 HOUR)),
(30, 'EVENT_REPORT', 21, 15, '晋安河公园护栏油漆脱落.png', 'seed-event-report-30.png', 'demo/seed-event-report-30.png', '/uploads/demo/seed-event-report-30.png', 'image/png', 70033, '01dcfffb24bc5e40af8aede5c1d5fdd8bfcf70532732f92adb9cc1213427a783', 'BOUND', DATE_SUB(NOW(), INTERVAL 30 HOUR)),
(31, 'EVENT_REPORT', 22, 12, '茶亭公园路灯白天常亮.png', 'seed-event-report-31.png', 'demo/seed-event-report-31.png', '/uploads/demo/seed-event-report-31.png', 'image/png', 146346, 'fb02da209af8bf550c3d88de56497826139581439222a6e4ce9a5f8621cd68d7', 'BOUND', DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(32, 'EVENT_REPORT', 23, 13, '上渡路井盖异响.png', 'seed-event-report-32.png', 'demo/seed-event-report-32.png', '/uploads/demo/seed-event-report-32.png', 'image/png', 64760, 'c6569549585450790eac99e85d944db62f42803b4b6cf7281f7cb60c1a4ba6f5', 'BOUND', DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(33, 'EVENT_REPORT', 24, 11, '五四北路口绿化带内垃圾堆积.png', 'seed-event-report-33.png', 'demo/seed-event-report-33.png', '/uploads/demo/seed-event-report-33.png', 'image/png', 1654620, '02bebce03379e90f0b71bdc4ceba1705108f8747b375c68ac73bb4f80e5708b6', 'BOUND', DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(34, 'EVENT_REPORT', 25, 15, '福马路人行道盲道砖破损.png', 'seed-event-report-34.png', 'demo/seed-event-report-34.png', '/uploads/demo/seed-event-report-34.png', 'image/png', 79204, '03b710757b6318098775c331d36e569623173ca05530ff298c7554a3280635db', 'BOUND', DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(35, 'EVENT_REPORT', 26, 14, '儒江路人行道隔离墩倾斜.png', 'seed-event-report-35.png', 'demo/seed-event-report-35.png', '/uploads/demo/seed-event-report-35.png', 'image/png', 1028224, '53a33541cf346ec10a083ccdfd08290de3be35ffd01901a6e4f90464221c3a47', 'BOUND', DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(36, 'EVENT_REPORT', 27, 13, '金山工业园区垃圾集中点异味.png', 'seed-event-report-36.png', 'demo/seed-event-report-36.png', '/uploads/demo/seed-event-report-36.png', 'image/png', 337991, '38b19263c346003df135d8f4bf1496684f647c72c94280a4181d8e24f93544d6', 'BOUND', DATE_SUB(NOW(), INTERVAL 30 HOUR)),
(37, 'EVENT_REPORT', 28, 12, '宁化支路人行道护栏缺失.png', 'seed-event-report-37.png', 'demo/seed-event-report-37.png', '/uploads/demo/seed-event-report-37.png', 'image/png', 828491, '8f6efe4dafcc3f343f7b1c2d5c47c5c45b1f2cc85f21c9c842175aafb99c2406', 'BOUND', DATE_SUB(NOW(), INTERVAL 26 HOUR)),
(38, 'EVENT_REPORT', 29, 11, '航城路一段路灯整排不亮.png', 'seed-event-report-38.png', 'demo/seed-event-report-38.png', '/uploads/demo/seed-event-report-38.png', 'image/png', 1131040, 'a9c110438518572d2a020f15c733787b7f123d915f5952d415fd5d7e49546a80', 'BOUND', DATE_SUB(NOW(), INTERVAL 34 HOUR)),
(39, 'EVENT_REPORT', 30, 15, '晋安河支流河面漂浮油污.png', 'seed-event-report-39.png', 'demo/seed-event-report-39.png', '/uploads/demo/seed-event-report-39.png', 'image/png', 711211, '824ed4772e45cd3dc1362902b94a9fc28accba766a5e12f4dec809429dc44feb', 'BOUND', DATE_SUB(NOW(), INTERVAL 52 HOUR)),
(40, 'EVENT_REPORT', 31, 11, '梅峰路人行道树池篦子翘起.png', 'seed-event-report-40.png', 'demo/seed-event-report-40.png', '/uploads/demo/seed-event-report-40.png', 'image/png', 736134, '6abcab66fa997ec9edb58322dda961c3688bdf5f470c82a642e639fe8cf35d0a', 'BOUND', DATE_SUB(NOW(), INTERVAL 48 HOUR)),
(41, 'EVENT_REPORT', 32, 13, '上渡路人行天桥栏杆松动.png', 'seed-event-report-41.png', 'demo/seed-event-report-41.png', '/uploads/demo/seed-event-report-41.png', 'image/png', 887874, 'f2c7fb48c75497fde9f3db7fee9976f7a06b87a7cd4b5705fb6c7cc3c4cbad1f', 'BOUND', DATE_SUB(NOW(), INTERVAL 44 HOUR)),
(42, 'EVENT_REPORT', 33, 12, '瀛洲路夜市收摊后地面油污.png', 'seed-event-report-42.png', 'demo/seed-event-report-42.png', '/uploads/demo/seed-event-report-42.png', 'image/png', 98319, 'bde69ee33d9b203a5a8959e77205c3b5d288bf6f84014e5907fb50baf10fc489', 'BOUND', DATE_SUB(NOW(), INTERVAL 96 HOUR)),
(43, 'EVENT_REPORT', 34, 14, '罗星路路面坑槽影响行车.png', 'seed-event-report-43.png', 'demo/seed-event-report-43.png', '/uploads/demo/seed-event-report-43.png', 'image/png', 70033, '01dcfffb24bc5e40af8aede5c1d5fdd8bfcf70532732f92adb9cc1213427a783', 'BOUND', DATE_SUB(NOW(), INTERVAL 120 HOUR)),
(44, 'EVENT_REPORT', 35, 15, '岳峰路人行道灯长时间不亮.png', 'seed-event-report-44.png', 'demo/seed-event-report-44.png', '/uploads/demo/seed-event-report-44.png', 'image/png', 146346, 'fb02da209af8bf550c3d88de56497826139581439222a6e4ce9a5f8621cd68d7', 'BOUND', DATE_SUB(NOW(), INTERVAL 144 HOUR)),
(45, 'EVENT_REPORT', 36, 11, '温泉公园公厕无障碍扶手松动.png', 'seed-event-report-45.png', 'demo/seed-event-report-45.png', '/uploads/demo/seed-event-report-45.png', 'image/png', 64760, 'c6569549585450790eac99e85d944db62f42803b4b6cf7281f7cb60c1a4ba6f5', 'BOUND', DATE_SUB(NOW(), INTERVAL 160 HOUR)),
(46, 'WORKORDER_PROCESS', 12, 39, '处理中-仓山区.png', 'seed-workorder-process-46.png', 'demo/seed-workorder-process-46.png', '/uploads/demo/seed-workorder-process-46.png', 'image/png', 887874, 'f2c7fb48c75497fde9f3db7fee9976f7a06b87a7cd4b5705fb6c7cc3c4cbad1f', 'BOUND', DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(47, 'WORKORDER_PROCESS', 13, 34, '处理中-台江区.png', 'seed-workorder-process-47.png', 'demo/seed-workorder-process-47.png', '/uploads/demo/seed-workorder-process-47.png', 'image/png', 70033, '01dcfffb24bc5e40af8aede5c1d5fdd8bfcf70532732f92adb9cc1213427a783', 'BOUND', DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(48, 'WORKORDER_PROCESS', 14, 38, '处理中-长乐区.png', 'seed-workorder-process-48.png', 'demo/seed-workorder-process-48.png', '/uploads/demo/seed-workorder-process-48.png', 'image/png', 64760, 'c6569549585450790eac99e85d944db62f42803b4b6cf7281f7cb60c1a4ba6f5', 'BOUND', DATE_SUB(NOW(), INTERVAL 28 HOUR)),
(49, 'WORKORDER_PROCESS', 15, 41, '处理中-晋安区.png', 'seed-workorder-process-49.png', 'demo/seed-workorder-process-49.png', '/uploads/demo/seed-workorder-process-49.png', 'image/png', 79204, '03b710757b6318098775c331d36e569623173ca05530ff298c7554a3280635db', 'BOUND', DATE_SUB(NOW(), INTERVAL 46 HOUR)),
(50, 'WORKORDER_RESULT', 15, 41, '整改后-晋安区.jpg', 'seed-workorder-result-50.jpg', 'demo/seed-workorder-result-50.jpg', '/uploads/demo/seed-workorder-result-50.jpg', 'image/jpeg', 242009, 'fd0a020657dcab7d946071ffa2ef6386c92415f14d8069825dc568c3efe077ab', 'BOUND', DATE_SUB(NOW(), INTERVAL 42 HOUR)),
(51, 'WORKORDER_PROCESS', 16, 33, '处理中-鼓楼区.png', 'seed-workorder-process-51.png', 'demo/seed-workorder-process-51.png', '/uploads/demo/seed-workorder-process-51.png', 'image/png', 887874, 'f2c7fb48c75497fde9f3db7fee9976f7a06b87a7cd4b5705fb6c7cc3c4cbad1f', 'BOUND', DATE_SUB(NOW(), INTERVAL 42 HOUR)),
(52, 'WORKORDER_RESULT', 16, 33, '整改后-鼓楼区.jpg', 'seed-workorder-result-52.jpg', 'demo/seed-workorder-result-52.jpg', '/uploads/demo/seed-workorder-result-52.jpg', 'image/jpeg', 228644, 'b01c65f1be0356ad2f62c8e96e7e2f8e79636c7791a81b06d0b86e12c85f74b5', 'BOUND', DATE_SUB(NOW(), INTERVAL 38 HOUR)),
(53, 'WORKORDER_PROCESS', 17, 42, '处理中-仓山区.png', 'seed-workorder-process-53.png', 'demo/seed-workorder-process-53.png', '/uploads/demo/seed-workorder-process-53.png', 'image/png', 70033, '01dcfffb24bc5e40af8aede5c1d5fdd8bfcf70532732f92adb9cc1213427a783', 'BOUND', DATE_SUB(NOW(), INTERVAL 38 HOUR)),
(54, 'WORKORDER_RESULT', 17, 42, '整改后-仓山区.jpg', 'seed-workorder-result-54.jpg', 'demo/seed-workorder-result-54.jpg', '/uploads/demo/seed-workorder-result-54.jpg', 'image/jpeg', 212435, '2c272483996a7f5549edbaf01ded9edc4b88331f9a162b06075b0639dd390046', 'BOUND', DATE_SUB(NOW(), INTERVAL 34 HOUR)),
(55, 'WORKORDER_PROCESS', 18, 40, '处理中-台江区.png', 'seed-workorder-process-55.png', 'demo/seed-workorder-process-55.png', '/uploads/demo/seed-workorder-process-55.png', 'image/png', 64760, 'c6569549585450790eac99e85d944db62f42803b4b6cf7281f7cb60c1a4ba6f5', 'BOUND', DATE_SUB(NOW(), INTERVAL 90 HOUR)),
(56, 'WORKORDER_RESULT', 18, 40, '整改后-台江区.jpg', 'seed-workorder-result-56.jpg', 'demo/seed-workorder-result-56.jpg', '/uploads/demo/seed-workorder-result-56.jpg', 'image/jpeg', 202246, '717af14395e3249d9fa4da7381a361e97c991a6a0a40ee532efbb0c64dc893fd', 'BOUND', DATE_SUB(NOW(), INTERVAL 86 HOUR)),
(57, 'WORKORDER_PROCESS', 19, 2, '处理中-马尾区.png', 'seed-workorder-process-57.png', 'demo/seed-workorder-process-57.png', '/uploads/demo/seed-workorder-process-57.png', 'image/png', 79204, '03b710757b6318098775c331d36e569623173ca05530ff298c7554a3280635db', 'BOUND', DATE_SUB(NOW(), INTERVAL 114 HOUR)),
(58, 'WORKORDER_RESULT', 19, 2, '整改后-马尾区.jpg', 'seed-workorder-result-58.jpg', 'demo/seed-workorder-result-58.jpg', '/uploads/demo/seed-workorder-result-58.jpg', 'image/jpeg', 242009, 'fd0a020657dcab7d946071ffa2ef6386c92415f14d8069825dc568c3efe077ab', 'BOUND', DATE_SUB(NOW(), INTERVAL 110 HOUR)),
(59, 'WORKORDER_PROCESS', 20, 37, '处理中-晋安区.png', 'seed-workorder-process-59.png', 'demo/seed-workorder-process-59.png', '/uploads/demo/seed-workorder-process-59.png', 'image/png', 887874, 'f2c7fb48c75497fde9f3db7fee9976f7a06b87a7cd4b5705fb6c7cc3c4cbad1f', 'BOUND', DATE_SUB(NOW(), INTERVAL 138 HOUR)),
(60, 'WORKORDER_RESULT', 20, 37, '整改后-晋安区.jpg', 'seed-workorder-result-60.jpg', 'demo/seed-workorder-result-60.jpg', '/uploads/demo/seed-workorder-result-60.jpg', 'image/jpeg', 242009, 'fd0a020657dcab7d946071ffa2ef6386c92415f14d8069825dc568c3efe077ab', 'BOUND', DATE_SUB(NOW(), INTERVAL 134 HOUR)),
(61, 'WORKORDER_PROCESS', 21, 32, '处理中-鼓楼区.png', 'seed-workorder-process-61.png', 'demo/seed-workorder-process-61.png', '/uploads/demo/seed-workorder-process-61.png', 'image/png', 70033, '01dcfffb24bc5e40af8aede5c1d5fdd8bfcf70532732f92adb9cc1213427a783', 'BOUND', DATE_SUB(NOW(), INTERVAL 154 HOUR)),
(62, 'WORKORDER_RESULT', 21, 32, '整改后-鼓楼区.jpg', 'seed-workorder-result-62.jpg', 'demo/seed-workorder-result-62.jpg', '/uploads/demo/seed-workorder-result-62.jpg', 'image/jpeg', 228644, 'b01c65f1be0356ad2f62c8e96e7e2f8e79636c7791a81b06d0b86e12c85f74b5', 'BOUND', DATE_SUB(NOW(), INTERVAL 150 HOUR));

-- ---------- 事件状态流水 ----------
INSERT INTO `t_event_status_log`
(`event_id`, `event_no`, `from_status`, `to_status`, `action`, `operator_id`, `operator_role`, `remark`, `create_time`)
VALUES
(14, 'EV202610250024', NULL, 'PENDING_REVIEW', 'CREATE', 15, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(15, 'EV202610250025', NULL, 'PENDING_REVIEW', 'CREATE', 11, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(16, 'EV202610250026', NULL, 'PENDING_REVIEW', 'CREATE', 13, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(17, 'EV202610250027', NULL, 'PENDING_REVIEW', 'CREATE', 12, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(18, 'EV202610250028', NULL, 'PENDING_REVIEW', 'CREATE', 13, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(19, 'EV202610250029', NULL, 'PENDING_REVIEW', 'CREATE', 14, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 9 HOUR)),
(20, 'EV202610250030', NULL, 'PENDING_REVIEW', 'CREATE', 11, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 28 HOUR)),
(20, 'EV202610250030', 'PENDING_REVIEW', 'REJECTED', 'REJECT', 1, 'ADMIN', '照片未拍到小区名称与门牌，无法确认归属，请补充带地标的照片后重新提交', DATE_SUB(NOW(), INTERVAL 27 HOUR)),
(21, 'EV202610250031', NULL, 'PENDING_REVIEW', 'CREATE', 15, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 30 HOUR)),
(21, 'EV202610250031', 'PENDING_REVIEW', 'REJECTED', 'REJECT', 1, 'ADMIN', '该问题属于日常养护范围，已转交公园管理单位按计划处理，本次不作事件派单', DATE_SUB(NOW(), INTERVAL 29 HOUR)),
(22, 'EV202610250032', NULL, 'PENDING_REVIEW', 'CREATE', 12, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(22, 'EV202610250032', 'PENDING_REVIEW', 'CANCELLED', 'CANCEL', 12, 'RESIDENT', '公园管理方已自行处理，居民撤销上报', DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(23, 'EV202610250033', NULL, 'PENDING_REVIEW', 'CREATE', 13, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(23, 'EV202610250033', 'PENDING_REVIEW', 'CANCELLED', 'CANCEL', 13, 'RESIDENT', '道路养护单位巡查时已同步处置，居民撤销上报', DATE_SUB(NOW(), INTERVAL 16 HOUR)),
(24, 'EV202610250034', NULL, 'PENDING_REVIEW', 'CREATE', 11, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(24, 'EV202610250034', 'PENDING_REVIEW', 'DISPATCHED', 'APPROVE', 1, 'ADMIN', '审核通过，派单给组长张环卫', DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(25, 'EV202610250035', NULL, 'PENDING_REVIEW', 'CREATE', 15, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(25, 'EV202610250035', 'PENDING_REVIEW', 'DISPATCHED', 'APPROVE', 1, 'ADMIN', '审核通过，派单给组长李道路', DATE_SUB(NOW(), INTERVAL 9 HOUR)),
(26, 'EV202610250036', NULL, 'PENDING_REVIEW', 'CREATE', 14, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 12 HOUR)),
(26, 'EV202610250036', 'PENDING_REVIEW', 'DISPATCHED', 'APPROVE', 1, 'ADMIN', '审核通过，派单给组长赵设施', DATE_SUB(NOW(), INTERVAL 11 HOUR)),
(27, 'EV202610250037', NULL, 'PENDING_REVIEW', 'CREATE', 13, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 30 HOUR)),
(27, 'EV202610250037', 'PENDING_REVIEW', 'DISPATCHED', 'APPROVE', 1, 'ADMIN', '审核通过，派单给组长张环卫', DATE_SUB(NOW(), INTERVAL 29 HOUR)),
(27, 'EV202610250037', 'DISPATCHED', 'PROCESSING', 'MQ_SYNC', 0, 'SYSTEM', '工单状态同步：WO202610250017', DATE_SUB(NOW(), INTERVAL 22 HOUR)),
(28, 'EV202610250038', NULL, 'PENDING_REVIEW', 'CREATE', 12, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 26 HOUR)),
(28, 'EV202610250038', 'PENDING_REVIEW', 'DISPATCHED', 'APPROVE', 1, 'ADMIN', '审核通过，派单给组长李道路', DATE_SUB(NOW(), INTERVAL 25 HOUR)),
(28, 'EV202610250038', 'DISPATCHED', 'PROCESSING', 'MQ_SYNC', 0, 'SYSTEM', '工单状态同步：WO202610250018', DATE_SUB(NOW(), INTERVAL 18 HOUR)),
(29, 'EV202610250039', NULL, 'PENDING_REVIEW', 'CREATE', 11, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 34 HOUR)),
(29, 'EV202610250039', 'PENDING_REVIEW', 'DISPATCHED', 'APPROVE', 1, 'ADMIN', '审核通过，派单给组长王照明', DATE_SUB(NOW(), INTERVAL 33 HOUR)),
(29, 'EV202610250039', 'DISPATCHED', 'PROCESSING', 'MQ_SYNC', 0, 'SYSTEM', '工单状态同步：WO202610250019', DATE_SUB(NOW(), INTERVAL 26 HOUR)),
(30, 'EV202610250040', NULL, 'PENDING_REVIEW', 'CREATE', 15, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 52 HOUR)),
(30, 'EV202610250040', 'PENDING_REVIEW', 'DISPATCHED', 'APPROVE', 1, 'ADMIN', '审核通过，派单给组长张环卫', DATE_SUB(NOW(), INTERVAL 51 HOUR)),
(30, 'EV202610250040', 'DISPATCHED', 'PROCESSING', 'MQ_SYNC', 0, 'SYSTEM', '工单状态同步：WO202610250020', DATE_SUB(NOW(), INTERVAL 44 HOUR)),
(30, 'EV202610250040', 'PROCESSING', 'PENDING_ACCEPTANCE', 'MQ_SYNC', 0, 'SYSTEM', '组长复核通过，工单状态同步：WO202610250020', DATE_SUB(NOW(), INTERVAL 32 HOUR)),
(31, 'EV202610250041', NULL, 'PENDING_REVIEW', 'CREATE', 11, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 48 HOUR)),
(31, 'EV202610250041', 'PENDING_REVIEW', 'DISPATCHED', 'APPROVE', 1, 'ADMIN', '审核通过，派单给组长李道路', DATE_SUB(NOW(), INTERVAL 47 HOUR)),
(31, 'EV202610250041', 'DISPATCHED', 'PROCESSING', 'MQ_SYNC', 0, 'SYSTEM', '工单状态同步：WO202610250021', DATE_SUB(NOW(), INTERVAL 40 HOUR)),
(31, 'EV202610250041', 'PROCESSING', 'PENDING_ACCEPTANCE', 'MQ_SYNC', 0, 'SYSTEM', '组长复核通过，工单状态同步：WO202610250021', DATE_SUB(NOW(), INTERVAL 28 HOUR)),
(32, 'EV202610250042', NULL, 'PENDING_REVIEW', 'CREATE', 13, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 44 HOUR)),
(32, 'EV202610250042', 'PENDING_REVIEW', 'DISPATCHED', 'APPROVE', 1, 'ADMIN', '审核通过，派单给组长赵设施', DATE_SUB(NOW(), INTERVAL 43 HOUR)),
(32, 'EV202610250042', 'DISPATCHED', 'PROCESSING', 'MQ_SYNC', 0, 'SYSTEM', '工单状态同步：WO202610250022', DATE_SUB(NOW(), INTERVAL 36 HOUR)),
(32, 'EV202610250042', 'PROCESSING', 'PENDING_ACCEPTANCE', 'MQ_SYNC', 0, 'SYSTEM', '组长复核通过，工单状态同步：WO202610250022', DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(33, 'EV202610250043', NULL, 'PENDING_REVIEW', 'CREATE', 12, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 96 HOUR)),
(33, 'EV202610250043', 'PENDING_REVIEW', 'DISPATCHED', 'APPROVE', 1, 'ADMIN', '审核通过，派单给组长张环卫', DATE_SUB(NOW(), INTERVAL 95 HOUR)),
(33, 'EV202610250043', 'DISPATCHED', 'PROCESSING', 'MQ_SYNC', 0, 'SYSTEM', '工单状态同步：WO202610250023', DATE_SUB(NOW(), INTERVAL 88 HOUR)),
(33, 'EV202610250043', 'PROCESSING', 'PENDING_ACCEPTANCE', 'MQ_SYNC', 0, 'SYSTEM', '组长复核通过，工单状态同步：WO202610250023', DATE_SUB(NOW(), INTERVAL 76 HOUR)),
(33, 'EV202610250043', 'PENDING_ACCEPTANCE', 'CLOSED', 'MQ_SYNC', 0, 'SYSTEM', '验收通过，事件关闭', DATE_SUB(NOW(), INTERVAL 72 HOUR)),
(34, 'EV202610250044', NULL, 'PENDING_REVIEW', 'CREATE', 14, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 120 HOUR)),
(34, 'EV202610250044', 'PENDING_REVIEW', 'DISPATCHED', 'APPROVE', 1, 'ADMIN', '审核通过，派单给组长李道路', DATE_SUB(NOW(), INTERVAL 119 HOUR)),
(34, 'EV202610250044', 'DISPATCHED', 'PROCESSING', 'MQ_SYNC', 0, 'SYSTEM', '工单状态同步：WO202610250024', DATE_SUB(NOW(), INTERVAL 112 HOUR)),
(34, 'EV202610250044', 'PROCESSING', 'PENDING_ACCEPTANCE', 'MQ_SYNC', 0, 'SYSTEM', '组长复核通过，工单状态同步：WO202610250024', DATE_SUB(NOW(), INTERVAL 100 HOUR)),
(34, 'EV202610250044', 'PENDING_ACCEPTANCE', 'CLOSED', 'MQ_SYNC', 0, 'SYSTEM', '验收通过，事件关闭', DATE_SUB(NOW(), INTERVAL 96 HOUR)),
(35, 'EV202610250045', NULL, 'PENDING_REVIEW', 'CREATE', 15, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 144 HOUR)),
(35, 'EV202610250045', 'PENDING_REVIEW', 'DISPATCHED', 'APPROVE', 1, 'ADMIN', '审核通过，派单给组长王照明', DATE_SUB(NOW(), INTERVAL 143 HOUR)),
(35, 'EV202610250045', 'DISPATCHED', 'PROCESSING', 'MQ_SYNC', 0, 'SYSTEM', '工单状态同步：WO202610250025', DATE_SUB(NOW(), INTERVAL 136 HOUR)),
(35, 'EV202610250045', 'PROCESSING', 'PENDING_ACCEPTANCE', 'MQ_SYNC', 0, 'SYSTEM', '组长复核通过，工单状态同步：WO202610250025', DATE_SUB(NOW(), INTERVAL 124 HOUR)),
(35, 'EV202610250045', 'PENDING_ACCEPTANCE', 'CLOSED', 'MQ_SYNC', 0, 'SYSTEM', '验收通过，事件关闭', DATE_SUB(NOW(), INTERVAL 120 HOUR)),
(36, 'EV202610250046', NULL, 'PENDING_REVIEW', 'CREATE', 11, 'RESIDENT', '居民上报事件', DATE_SUB(NOW(), INTERVAL 160 HOUR)),
(36, 'EV202610250046', 'PENDING_REVIEW', 'DISPATCHED', 'APPROVE', 1, 'ADMIN', '审核通过，派单给组长赵设施', DATE_SUB(NOW(), INTERVAL 159 HOUR)),
(36, 'EV202610250046', 'DISPATCHED', 'PROCESSING', 'MQ_SYNC', 0, 'SYSTEM', '工单状态同步：WO202610250026', DATE_SUB(NOW(), INTERVAL 152 HOUR)),
(36, 'EV202610250046', 'PROCESSING', 'PENDING_ACCEPTANCE', 'MQ_SYNC', 0, 'SYSTEM', '组长复核通过，工单状态同步：WO202610250026', DATE_SUB(NOW(), INTERVAL 140 HOUR)),
(36, 'EV202610250046', 'PENDING_ACCEPTANCE', 'CLOSED', 'MQ_SYNC', 0, 'SYSTEM', '验收通过，事件关闭', DATE_SUB(NOW(), INTERVAL 136 HOUR));

-- ---------- 事件审核记录 ----------
INSERT INTO `t_event_audit_log`
(`event_id`, `auditor_id`, `decision`, `before_category`, `after_category`, `before_priority`, `after_priority`, `reason`, `create_time`)
VALUES
(20, 1, 'REJECT', 'ENVIRONMENT_SANITATION', 'ENVIRONMENT_SANITATION', 'MEDIUM', 'MEDIUM', '照片未拍到小区名称与门牌，无法确认归属，请补充带地标的照片后重新提交', DATE_SUB(NOW(), INTERVAL 27 HOUR)),
(21, 1, 'REJECT', 'PUBLIC_FACILITY', 'PUBLIC_FACILITY', 'LOW', 'LOW', '该问题属于日常养护范围，已转交公园管理单位按计划处理，本次不作事件派单', DATE_SUB(NOW(), INTERVAL 29 HOUR)),
(24, 1, 'APPROVE', 'ENVIRONMENT_SANITATION', 'ENVIRONMENT_SANITATION', 'MEDIUM', 'MEDIUM', '情况属实，按 AI 复评等级派单', DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(25, 1, 'APPROVE', 'ROAD_FACILITY', 'ROAD_FACILITY', 'HIGH', 'HIGH', '情况属实，按 AI 复评等级派单', DATE_SUB(NOW(), INTERVAL 9 HOUR)),
(26, 1, 'APPROVE', 'PUBLIC_FACILITY', 'PUBLIC_FACILITY', 'MEDIUM', 'HIGH', '情况属实，按 AI 复评等级派单', DATE_SUB(NOW(), INTERVAL 11 HOUR)),
(27, 1, 'APPROVE', 'ENVIRONMENT_SANITATION', 'ENVIRONMENT_SANITATION', 'HIGH', 'URGENT', '情况属实，按 AI 复评等级派单', DATE_SUB(NOW(), INTERVAL 29 HOUR)),
(28, 1, 'APPROVE', 'ROAD_FACILITY', 'ROAD_FACILITY', 'HIGH', 'URGENT', '情况属实，按 AI 复评等级派单', DATE_SUB(NOW(), INTERVAL 25 HOUR)),
(29, 1, 'APPROVE', 'PUBLIC_LIGHTING', 'PUBLIC_LIGHTING', 'HIGH', 'HIGH', '情况属实，按 AI 复评等级派单', DATE_SUB(NOW(), INTERVAL 33 HOUR)),
(30, 1, 'APPROVE', 'ENVIRONMENT_SANITATION', 'ENVIRONMENT_SANITATION', 'HIGH', 'URGENT', '情况属实，按 AI 复评等级派单', DATE_SUB(NOW(), INTERVAL 51 HOUR)),
(31, 1, 'APPROVE', 'ROAD_FACILITY', 'ROAD_FACILITY', 'MEDIUM', 'HIGH', '情况属实，按 AI 复评等级派单', DATE_SUB(NOW(), INTERVAL 47 HOUR)),
(32, 1, 'APPROVE', 'PUBLIC_FACILITY', 'PUBLIC_FACILITY', 'HIGH', 'URGENT', '情况属实，按 AI 复评等级派单', DATE_SUB(NOW(), INTERVAL 43 HOUR)),
(33, 1, 'APPROVE', 'ENVIRONMENT_SANITATION', 'ENVIRONMENT_SANITATION', 'MEDIUM', 'MEDIUM', '情况属实，按 AI 复评等级派单', DATE_SUB(NOW(), INTERVAL 95 HOUR)),
(34, 1, 'APPROVE', 'ROAD_FACILITY', 'ROAD_FACILITY', 'HIGH', 'HIGH', '情况属实，按 AI 复评等级派单', DATE_SUB(NOW(), INTERVAL 119 HOUR)),
(35, 1, 'APPROVE', 'PUBLIC_LIGHTING', 'PUBLIC_LIGHTING', 'MEDIUM', 'HIGH', '情况属实，按 AI 复评等级派单', DATE_SUB(NOW(), INTERVAL 143 HOUR)),
(36, 1, 'APPROVE', 'PUBLIC_FACILITY', 'PUBLIC_FACILITY', 'MEDIUM', 'HIGH', '情况属实，按 AI 复评等级派单', DATE_SUB(NOW(), INTERVAL 159 HOUR));

-- ---------- 事件 AI 快照 ----------
INSERT INTO `t_event_ai_snapshot`
(`event_id`, `ai_call_log_id`, `category_code`, `priority`, `summary`, `suggestion`, `confidence`, `model_name`, `accepted`, `create_time`)
VALUES
(14, 7, 'ENVIRONMENT_SANITATION', 'HIGH', '人行道果皮箱满溢，垃圾散落。', '当日清运并冲洗果皮箱周边地面', 0.860, 'glm-4-flash', 0, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(15, 8, 'PUBLIC_LIGHTING', 'MEDIUM', '地铁口三盏路灯闪烁，照明不稳。', '检查镇流器与线路接头，必要时更换灯具', 0.830, 'glm-4-flash', 0, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(16, 9, 'ROAD_FACILITY', 'HIGH', '非机动车道路面凹陷积水。', '切割修补凹陷路面并恢复排水坡度', 0.880, 'glm-4-flash', 0, DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(17, 10, 'PUBLIC_FACILITY', 'HIGH', '公园座椅木板断裂，铁钉外露。', '拆除破损座椅并更换木板，排查同区域座椅', 0.900, 'glm-4-flash', 0, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(18, 11, 'ROAD_FACILITY', 'MEDIUM', '人行道地砖松动，雨天溅水。', '重新铺设松动地砖并夯实基础', 0.790, 'glm-4-flash', 0, DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(19, 12, 'ENVIRONMENT_SANITATION', 'HIGH', '垃圾房污水外流，异味明显。', '抽排污水并冲洗路面，检查垃圾房排水口', 0.870, 'glm-4-flash', 0, DATE_SUB(NOW(), INTERVAL 9 HOUR)),
(27, 13, 'ENVIRONMENT_SANITATION', 'URGENT', '园区垃圾集中点异味强烈。', '立即清运并冲洗消杀，调整清运频次', 0.910, 'glm-4-flash', 0, DATE_SUB(NOW(), INTERVAL 30 HOUR)),
(30, 14, 'ENVIRONMENT_SANITATION', 'URGENT', '河面出现带状油污。', '设置围油栏并溯源排查排口，组织打捞', 0.920, 'glm-4-flash', 0, DATE_SUB(NOW(), INTERVAL 52 HOUR));

-- ---------- 工单（待接单 3 / 处理中 3 / 待验收 3 / 已完成 4） ----------
USE city_workorder;
INSERT INTO `t_work_order`
(`id`, `work_order_no`, `event_id`, `event_no`, `category_code`, `priority`, `assignee_id`, `assignee_name`, `leader_id`, `leader_name`, `dept_code`, `status`,
 `creator_id`, `assign_time`, `accept_time`, `submit_time`, `leader_review_time`, `leader_review_remark`, `finish_time`, `acceptance_remark`, `return_count`, `reassign_count`,
 `ai_verify_result`, `ai_verify_time`, `create_time`, `update_time`)
VALUES
(9, 'WO202610250014', 24, 'EV202610250034', 'ENVIRONMENT_SANITATION', 'MEDIUM', 4, '张环卫', 4, '张环卫', 'SANITATION', 'PENDING_ACCEPT',
 1, DATE_SUB(NOW(), INTERVAL 5 HOUR), NULL, NULL, NULL, NULL, NULL, NULL, 0, 0,
 NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(10, 'WO202610250015', 25, 'EV202610250035', 'ROAD_FACILITY', 'HIGH', 2, '李道路', 2, '李道路', 'ROAD', 'PENDING_ACCEPT',
 1, DATE_SUB(NOW(), INTERVAL 9 HOUR), NULL, NULL, NULL, NULL, NULL, NULL, 0, 0,
 NULL, NULL, DATE_SUB(NOW(), INTERVAL 9 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(11, 'WO202610250016', 26, 'EV202610250036', 'PUBLIC_FACILITY', 'HIGH', 5, '赵设施', 5, '赵设施', 'FACILITY', 'PENDING_ACCEPT',
 1, DATE_SUB(NOW(), INTERVAL 11 HOUR), NULL, NULL, NULL, NULL, NULL, NULL, 0, 0,
 NULL, NULL, DATE_SUB(NOW(), INTERVAL 11 HOUR), DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(12, 'WO202610250017', 27, 'EV202610250037', 'ENVIRONMENT_SANITATION', 'URGENT', 39, '许清运', 4, '张环卫', 'SANITATION', 'PROCESSING',
 1, DATE_SUB(NOW(), INTERVAL 29 HOUR), DATE_SUB(NOW(), INTERVAL 26 HOUR), NULL, NULL, NULL, NULL, NULL, 0, 0,
 NULL, NULL, DATE_SUB(NOW(), INTERVAL 29 HOUR), DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(13, 'WO202610250018', 28, 'EV202610250038', 'ROAD_FACILITY', 'URGENT', 34, '李检修', 2, '李道路', 'ROAD', 'PROCESSING',
 1, DATE_SUB(NOW(), INTERVAL 25 HOUR), DATE_SUB(NOW(), INTERVAL 22 HOUR), NULL, NULL, NULL, NULL, NULL, 0, 0,
 NULL, NULL, DATE_SUB(NOW(), INTERVAL 25 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(14, 'WO202610250019', 29, 'EV202610250039', 'PUBLIC_LIGHTING', 'HIGH', 38, '陈抢修', 3, '王照明', 'LIGHTING', 'PROCESSING',
 1, DATE_SUB(NOW(), INTERVAL 33 HOUR), DATE_SUB(NOW(), INTERVAL 30 HOUR), NULL, NULL, NULL, NULL, NULL, 0, 0,
 NULL, NULL, DATE_SUB(NOW(), INTERVAL 33 HOUR), DATE_SUB(NOW(), INTERVAL 10 HOUR)),
(15, 'WO202610250020', 30, 'EV202610250040', 'ENVIRONMENT_SANITATION', 'URGENT', 41, '邓冲洗', 4, '张环卫', 'SANITATION', 'PENDING_ACCEPTANCE',
 1, DATE_SUB(NOW(), INTERVAL 51 HOUR), DATE_SUB(NOW(), INTERVAL 48 HOUR), DATE_SUB(NOW(), INTERVAL 42 HOUR), DATE_SUB(NOW(), INTERVAL 37 HOUR), '围油栏已布设，油污完成打捞并冲洗岸坡，复核通过', NULL, NULL, 0, 0,
 '{"conclusion":"RECTIFIED","confidence":0.890,"reason":"整改后照片与上报点位一致，问题已消除。","issues":[],"degraded":false,"degradeReason":null,"model":"glm-4.6v-flash"}', DATE_SUB(NOW(), INTERVAL 40 HOUR), DATE_SUB(NOW(), INTERVAL 51 HOUR), DATE_SUB(NOW(), INTERVAL 28 HOUR)),
(16, 'WO202610250021', 31, 'EV202610250041', 'ROAD_FACILITY', 'HIGH', 33, '王路面', 2, '李道路', 'ROAD', 'PENDING_ACCEPTANCE',
 1, DATE_SUB(NOW(), INTERVAL 47 HOUR), DATE_SUB(NOW(), INTERVAL 44 HOUR), DATE_SUB(NOW(), INTERVAL 38 HOUR), DATE_SUB(NOW(), INTERVAL 33 HOUR), '篦子已复位并用膨胀螺栓固定，复核通过', NULL, NULL, 0, 0,
 '{"conclusion":"RECTIFIED","confidence":0.890,"reason":"整改后照片与上报点位一致，问题已消除。","issues":[],"degraded":false,"degradeReason":null,"model":"glm-4.6v-flash"}', DATE_SUB(NOW(), INTERVAL 36 HOUR), DATE_SUB(NOW(), INTERVAL 47 HOUR), DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(17, 'WO202610250022', 32, 'EV202610250042', 'PUBLIC_FACILITY', 'URGENT', 42, '郑排水', 5, '赵设施', 'FACILITY', 'PENDING_ACCEPTANCE',
 1, DATE_SUB(NOW(), INTERVAL 43 HOUR), DATE_SUB(NOW(), INTERVAL 40 HOUR), DATE_SUB(NOW(), INTERVAL 34 HOUR), DATE_SUB(NOW(), INTERVAL 29 HOUR), '栏杆螺栓已全部更换，现场推拉无晃动，复核通过', NULL, NULL, 0, 0,
 '{"conclusion":"RECTIFIED","confidence":0.890,"reason":"整改后照片与上报点位一致，问题已消除。","issues":[],"degraded":false,"degradeReason":null,"model":"glm-4.6v-flash"}', DATE_SUB(NOW(), INTERVAL 32 HOUR), DATE_SUB(NOW(), INTERVAL 43 HOUR), DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(18, 'WO202610250023', 33, 'EV202610250043', 'ENVIRONMENT_SANITATION', 'MEDIUM', 40, '何保洁', 4, '张环卫', 'SANITATION', 'COMPLETED',
 1, DATE_SUB(NOW(), INTERVAL 95 HOUR), DATE_SUB(NOW(), INTERVAL 92 HOUR), DATE_SUB(NOW(), INTERVAL 86 HOUR), DATE_SUB(NOW(), INTERVAL 81 HOUR), '地面已完成高压冲洗，复核通过', DATE_SUB(NOW(), INTERVAL 72 HOUR), '现场地面无油污残留，验收通过', 0, 0,
 '{"conclusion":"RECTIFIED","confidence":0.890,"reason":"整改后照片与上报点位一致，问题已消除。","issues":[],"degraded":false,"degradeReason":null,"model":"glm-4.6v-flash"}', DATE_SUB(NOW(), INTERVAL 84 HOUR), DATE_SUB(NOW(), INTERVAL 95 HOUR), DATE_SUB(NOW(), INTERVAL 72 HOUR)),
(19, 'WO202610250024', 34, 'EV202610250044', 'ROAD_FACILITY', 'HIGH', 2, '李道路', 2, '李道路', 'ROAD', 'COMPLETED',
 1, DATE_SUB(NOW(), INTERVAL 119 HOUR), DATE_SUB(NOW(), INTERVAL 116 HOUR), DATE_SUB(NOW(), INTERVAL 110 HOUR), NULL, NULL, DATE_SUB(NOW(), INTERVAL 96 HOUR), '坑槽修补平整，行车无颠簸，验收通过', 0, 0,
 '{"conclusion":"RECTIFIED","confidence":0.890,"reason":"整改后照片与上报点位一致，问题已消除。","issues":[],"degraded":false,"degradeReason":null,"model":"glm-4.6v-flash"}', DATE_SUB(NOW(), INTERVAL 108 HOUR), DATE_SUB(NOW(), INTERVAL 119 HOUR), DATE_SUB(NOW(), INTERVAL 96 HOUR)),
(20, 'WO202610250025', 35, 'EV202610250045', 'PUBLIC_LIGHTING', 'HIGH', 37, '黄配电', 3, '王照明', 'LIGHTING', 'COMPLETED',
 1, DATE_SUB(NOW(), INTERVAL 143 HOUR), DATE_SUB(NOW(), INTERVAL 140 HOUR), DATE_SUB(NOW(), INTERVAL 134 HOUR), DATE_SUB(NOW(), INTERVAL 129 HOUR), '庭院灯电源模块更换，整段恢复照明，复核通过', DATE_SUB(NOW(), INTERVAL 120 HOUR), '夜间复查整段照明正常，验收通过', 0, 0,
 '{"conclusion":"RECTIFIED","confidence":0.890,"reason":"整改后照片与上报点位一致，问题已消除。","issues":[],"degraded":false,"degradeReason":null,"model":"glm-4.6v-flash"}', DATE_SUB(NOW(), INTERVAL 132 HOUR), DATE_SUB(NOW(), INTERVAL 143 HOUR), DATE_SUB(NOW(), INTERVAL 120 HOUR)),
(21, 'WO202610250026', 36, 'EV202610250046', 'PUBLIC_FACILITY', 'HIGH', 32, '韩设施', 5, '赵设施', 'FACILITY', 'COMPLETED',
 1, DATE_SUB(NOW(), INTERVAL 159 HOUR), DATE_SUB(NOW(), INTERVAL 156 HOUR), DATE_SUB(NOW(), INTERVAL 150 HOUR), DATE_SUB(NOW(), INTERVAL 145 HOUR), '扶手已重新固定，借力测试稳固，复核通过', DATE_SUB(NOW(), INTERVAL 136 HOUR), '无障碍扶手牢固，验收通过', 0, 0,
 '{"conclusion":"RECTIFIED","confidence":0.890,"reason":"整改后照片与上报点位一致，问题已消除。","issues":[],"degraded":false,"degradeReason":null,"model":"glm-4.6v-flash"}', DATE_SUB(NOW(), INTERVAL 148 HOUR), DATE_SUB(NOW(), INTERVAL 159 HOUR), DATE_SUB(NOW(), INTERVAL 136 HOUR));

-- ---------- 工单处理记录 ----------
INSERT INTO `t_work_order_log`
(`work_order_id`, `work_order_no`, `action`, `from_status`, `to_status`, `operator_id`, `operator_role`, `content`, `attachment_ids`, `create_time`)
VALUES
(9, 'WO202610250014', 'CREATE', NULL, 'PENDING_ACCEPT', 1, 'ADMIN', '审核通过，创建工单并派单给组长张环卫', NULL, DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(10, 'WO202610250015', 'CREATE', NULL, 'PENDING_ACCEPT', 1, 'ADMIN', '审核通过，创建工单并派单给组长李道路', NULL, DATE_SUB(NOW(), INTERVAL 9 HOUR)),
(11, 'WO202610250016', 'CREATE', NULL, 'PENDING_ACCEPT', 1, 'ADMIN', '审核通过，创建工单并派单给组长赵设施', NULL, DATE_SUB(NOW(), INTERVAL 11 HOUR)),
(12, 'WO202610250017', 'CREATE', NULL, 'PENDING_ACCEPT', 1, 'ADMIN', '审核通过，创建工单并派单给组长张环卫', NULL, DATE_SUB(NOW(), INTERVAL 29 HOUR)),
(12, 'WO202610250017', 'ASSIGN', 'PENDING_ACCEPT', 'PENDING_ACCEPT', 4, 'WORKER', '分配给组员许清运处理', NULL, DATE_SUB(NOW(), INTERVAL 28 HOUR)),
(12, 'WO202610250017', 'ACCEPT', 'PENDING_ACCEPT', 'PROCESSING', 39, 'WORKER', '已接单，准备进场处理', NULL, DATE_SUB(NOW(), INTERVAL 26 HOUR)),
(12, 'WO202610250017', 'PROCESS', 'PROCESSING', 'PROCESSING', 39, 'WORKER', '已到达现场并开始处置，设置临时警示', NULL, DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(13, 'WO202610250018', 'CREATE', NULL, 'PENDING_ACCEPT', 1, 'ADMIN', '审核通过，创建工单并派单给组长李道路', NULL, DATE_SUB(NOW(), INTERVAL 25 HOUR)),
(13, 'WO202610250018', 'ASSIGN', 'PENDING_ACCEPT', 'PENDING_ACCEPT', 2, 'WORKER', '分配给组员李检修处理', NULL, DATE_SUB(NOW(), INTERVAL 24 HOUR)),
(13, 'WO202610250018', 'ACCEPT', 'PENDING_ACCEPT', 'PROCESSING', 34, 'WORKER', '已接单，准备进场处理', NULL, DATE_SUB(NOW(), INTERVAL 22 HOUR)),
(13, 'WO202610250018', 'PROCESS', 'PROCESSING', 'PROCESSING', 34, 'WORKER', '已到达现场并开始处置，设置临时警示', NULL, DATE_SUB(NOW(), INTERVAL 20 HOUR)),
(14, 'WO202610250019', 'CREATE', NULL, 'PENDING_ACCEPT', 1, 'ADMIN', '审核通过，创建工单并派单给组长王照明', NULL, DATE_SUB(NOW(), INTERVAL 33 HOUR)),
(14, 'WO202610250019', 'ASSIGN', 'PENDING_ACCEPT', 'PENDING_ACCEPT', 3, 'WORKER', '分配给组员陈抢修处理', NULL, DATE_SUB(NOW(), INTERVAL 32 HOUR)),
(14, 'WO202610250019', 'ACCEPT', 'PENDING_ACCEPT', 'PROCESSING', 38, 'WORKER', '已接单，准备进场处理', NULL, DATE_SUB(NOW(), INTERVAL 30 HOUR)),
(14, 'WO202610250019', 'PROCESS', 'PROCESSING', 'PROCESSING', 38, 'WORKER', '已到达现场并开始处置，设置临时警示', NULL, DATE_SUB(NOW(), INTERVAL 28 HOUR)),
(15, 'WO202610250020', 'CREATE', NULL, 'PENDING_ACCEPT', 1, 'ADMIN', '审核通过，创建工单并派单给组长张环卫', NULL, DATE_SUB(NOW(), INTERVAL 51 HOUR)),
(15, 'WO202610250020', 'ASSIGN', 'PENDING_ACCEPT', 'PENDING_ACCEPT', 4, 'WORKER', '分配给组员邓冲洗处理', NULL, DATE_SUB(NOW(), INTERVAL 50 HOUR)),
(15, 'WO202610250020', 'ACCEPT', 'PENDING_ACCEPT', 'PROCESSING', 41, 'WORKER', '已接单，准备进场处理', NULL, DATE_SUB(NOW(), INTERVAL 48 HOUR)),
(15, 'WO202610250020', 'PROCESS', 'PROCESSING', 'PROCESSING', 41, 'WORKER', '已到达现场并开始处置，设置临时警示', NULL, DATE_SUB(NOW(), INTERVAL 46 HOUR)),
(15, 'WO202610250020', 'SUBMIT', 'PROCESSING', 'PENDING_LEADER_REVIEW', 41, 'WORKER', '问题已处理完毕，上传整改后照片，请复核', NULL, DATE_SUB(NOW(), INTERVAL 42 HOUR)),
(15, 'WO202610250020', 'LEADER_PASS', 'PENDING_LEADER_REVIEW', 'PENDING_ACCEPTANCE', 4, 'WORKER', '围油栏已布设，油污完成打捞并冲洗岸坡，复核通过', NULL, DATE_SUB(NOW(), INTERVAL 37 HOUR)),
(16, 'WO202610250021', 'CREATE', NULL, 'PENDING_ACCEPT', 1, 'ADMIN', '审核通过，创建工单并派单给组长李道路', NULL, DATE_SUB(NOW(), INTERVAL 47 HOUR)),
(16, 'WO202610250021', 'ASSIGN', 'PENDING_ACCEPT', 'PENDING_ACCEPT', 2, 'WORKER', '分配给组员王路面处理', NULL, DATE_SUB(NOW(), INTERVAL 46 HOUR)),
(16, 'WO202610250021', 'ACCEPT', 'PENDING_ACCEPT', 'PROCESSING', 33, 'WORKER', '已接单，准备进场处理', NULL, DATE_SUB(NOW(), INTERVAL 44 HOUR)),
(16, 'WO202610250021', 'PROCESS', 'PROCESSING', 'PROCESSING', 33, 'WORKER', '已到达现场并开始处置，设置临时警示', NULL, DATE_SUB(NOW(), INTERVAL 42 HOUR)),
(16, 'WO202610250021', 'SUBMIT', 'PROCESSING', 'PENDING_LEADER_REVIEW', 33, 'WORKER', '问题已处理完毕，上传整改后照片，请复核', NULL, DATE_SUB(NOW(), INTERVAL 38 HOUR)),
(16, 'WO202610250021', 'LEADER_PASS', 'PENDING_LEADER_REVIEW', 'PENDING_ACCEPTANCE', 2, 'WORKER', '篦子已复位并用膨胀螺栓固定，复核通过', NULL, DATE_SUB(NOW(), INTERVAL 33 HOUR)),
(17, 'WO202610250022', 'CREATE', NULL, 'PENDING_ACCEPT', 1, 'ADMIN', '审核通过，创建工单并派单给组长赵设施', NULL, DATE_SUB(NOW(), INTERVAL 43 HOUR)),
(17, 'WO202610250022', 'ASSIGN', 'PENDING_ACCEPT', 'PENDING_ACCEPT', 5, 'WORKER', '分配给组员郑排水处理', NULL, DATE_SUB(NOW(), INTERVAL 42 HOUR)),
(17, 'WO202610250022', 'ACCEPT', 'PENDING_ACCEPT', 'PROCESSING', 42, 'WORKER', '已接单，准备进场处理', NULL, DATE_SUB(NOW(), INTERVAL 40 HOUR)),
(17, 'WO202610250022', 'PROCESS', 'PROCESSING', 'PROCESSING', 42, 'WORKER', '已到达现场并开始处置，设置临时警示', NULL, DATE_SUB(NOW(), INTERVAL 38 HOUR)),
(17, 'WO202610250022', 'SUBMIT', 'PROCESSING', 'PENDING_LEADER_REVIEW', 42, 'WORKER', '问题已处理完毕，上传整改后照片，请复核', NULL, DATE_SUB(NOW(), INTERVAL 34 HOUR)),
(17, 'WO202610250022', 'LEADER_PASS', 'PENDING_LEADER_REVIEW', 'PENDING_ACCEPTANCE', 5, 'WORKER', '栏杆螺栓已全部更换，现场推拉无晃动，复核通过', NULL, DATE_SUB(NOW(), INTERVAL 29 HOUR)),
(18, 'WO202610250023', 'CREATE', NULL, 'PENDING_ACCEPT', 1, 'ADMIN', '审核通过，创建工单并派单给组长张环卫', NULL, DATE_SUB(NOW(), INTERVAL 95 HOUR)),
(18, 'WO202610250023', 'ASSIGN', 'PENDING_ACCEPT', 'PENDING_ACCEPT', 4, 'WORKER', '分配给组员何保洁处理', NULL, DATE_SUB(NOW(), INTERVAL 94 HOUR)),
(18, 'WO202610250023', 'ACCEPT', 'PENDING_ACCEPT', 'PROCESSING', 40, 'WORKER', '已接单，准备进场处理', NULL, DATE_SUB(NOW(), INTERVAL 92 HOUR)),
(18, 'WO202610250023', 'PROCESS', 'PROCESSING', 'PROCESSING', 40, 'WORKER', '已到达现场并开始处置，设置临时警示', NULL, DATE_SUB(NOW(), INTERVAL 90 HOUR)),
(18, 'WO202610250023', 'SUBMIT', 'PROCESSING', 'PENDING_LEADER_REVIEW', 40, 'WORKER', '问题已处理完毕，上传整改后照片，请复核', NULL, DATE_SUB(NOW(), INTERVAL 86 HOUR)),
(18, 'WO202610250023', 'LEADER_PASS', 'PENDING_LEADER_REVIEW', 'PENDING_ACCEPTANCE', 4, 'WORKER', '地面已完成高压冲洗，复核通过', NULL, DATE_SUB(NOW(), INTERVAL 81 HOUR)),
(18, 'WO202610250023', 'ACCEPT_PASS', 'PENDING_ACCEPTANCE', 'COMPLETED', 1, 'ADMIN', '现场地面无油污残留，验收通过', NULL, DATE_SUB(NOW(), INTERVAL 72 HOUR)),
(19, 'WO202610250024', 'CREATE', NULL, 'PENDING_ACCEPT', 1, 'ADMIN', '审核通过，创建工单并派单给组长李道路', NULL, DATE_SUB(NOW(), INTERVAL 119 HOUR)),
(19, 'WO202610250024', 'ACCEPT', 'PENDING_ACCEPT', 'PROCESSING', 2, 'WORKER', '已接单，准备进场处理', NULL, DATE_SUB(NOW(), INTERVAL 116 HOUR)),
(19, 'WO202610250024', 'PROCESS', 'PROCESSING', 'PROCESSING', 2, 'WORKER', '已到达现场并开始处置，设置临时警示', NULL, DATE_SUB(NOW(), INTERVAL 114 HOUR)),
(19, 'WO202610250024', 'SUBMIT', 'PROCESSING', 'PENDING_LEADER_REVIEW', 2, 'WORKER', '问题已处理完毕，上传整改后照片，请复核', NULL, DATE_SUB(NOW(), INTERVAL 110 HOUR)),
(19, 'WO202610250024', 'ACCEPT_PASS', 'PENDING_ACCEPTANCE', 'COMPLETED', 1, 'ADMIN', '坑槽修补平整，行车无颠簸，验收通过', NULL, DATE_SUB(NOW(), INTERVAL 96 HOUR)),
(20, 'WO202610250025', 'CREATE', NULL, 'PENDING_ACCEPT', 1, 'ADMIN', '审核通过，创建工单并派单给组长王照明', NULL, DATE_SUB(NOW(), INTERVAL 143 HOUR)),
(20, 'WO202610250025', 'ASSIGN', 'PENDING_ACCEPT', 'PENDING_ACCEPT', 3, 'WORKER', '分配给组员黄配电处理', NULL, DATE_SUB(NOW(), INTERVAL 142 HOUR)),
(20, 'WO202610250025', 'ACCEPT', 'PENDING_ACCEPT', 'PROCESSING', 37, 'WORKER', '已接单，准备进场处理', NULL, DATE_SUB(NOW(), INTERVAL 140 HOUR)),
(20, 'WO202610250025', 'PROCESS', 'PROCESSING', 'PROCESSING', 37, 'WORKER', '已到达现场并开始处置，设置临时警示', NULL, DATE_SUB(NOW(), INTERVAL 138 HOUR)),
(20, 'WO202610250025', 'SUBMIT', 'PROCESSING', 'PENDING_LEADER_REVIEW', 37, 'WORKER', '问题已处理完毕，上传整改后照片，请复核', NULL, DATE_SUB(NOW(), INTERVAL 134 HOUR)),
(20, 'WO202610250025', 'LEADER_PASS', 'PENDING_LEADER_REVIEW', 'PENDING_ACCEPTANCE', 3, 'WORKER', '庭院灯电源模块更换，整段恢复照明，复核通过', NULL, DATE_SUB(NOW(), INTERVAL 129 HOUR)),
(20, 'WO202610250025', 'ACCEPT_PASS', 'PENDING_ACCEPTANCE', 'COMPLETED', 1, 'ADMIN', '夜间复查整段照明正常，验收通过', NULL, DATE_SUB(NOW(), INTERVAL 120 HOUR)),
(21, 'WO202610250026', 'CREATE', NULL, 'PENDING_ACCEPT', 1, 'ADMIN', '审核通过，创建工单并派单给组长赵设施', NULL, DATE_SUB(NOW(), INTERVAL 159 HOUR)),
(21, 'WO202610250026', 'ASSIGN', 'PENDING_ACCEPT', 'PENDING_ACCEPT', 5, 'WORKER', '分配给组员韩设施处理', NULL, DATE_SUB(NOW(), INTERVAL 158 HOUR)),
(21, 'WO202610250026', 'ACCEPT', 'PENDING_ACCEPT', 'PROCESSING', 32, 'WORKER', '已接单，准备进场处理', NULL, DATE_SUB(NOW(), INTERVAL 156 HOUR)),
(21, 'WO202610250026', 'PROCESS', 'PROCESSING', 'PROCESSING', 32, 'WORKER', '已到达现场并开始处置，设置临时警示', NULL, DATE_SUB(NOW(), INTERVAL 154 HOUR)),
(21, 'WO202610250026', 'SUBMIT', 'PROCESSING', 'PENDING_LEADER_REVIEW', 32, 'WORKER', '问题已处理完毕，上传整改后照片，请复核', NULL, DATE_SUB(NOW(), INTERVAL 150 HOUR)),
(21, 'WO202610250026', 'LEADER_PASS', 'PENDING_LEADER_REVIEW', 'PENDING_ACCEPTANCE', 5, 'WORKER', '扶手已重新固定，借力测试稳固，复核通过', NULL, DATE_SUB(NOW(), INTERVAL 145 HOUR)),
(21, 'WO202610250026', 'ACCEPT_PASS', 'PENDING_ACCEPTANCE', 'COMPLETED', 1, 'ADMIN', '无障碍扶手牢固，验收通过', NULL, DATE_SUB(NOW(), INTERVAL 136 HOUR));

-- ---------- 验收记录 ----------
INSERT INTO `t_work_order_acceptance`
(`work_order_id`, `work_order_no`, `acceptor_id`, `result`, `remark`, `create_time`)
VALUES
(18, 'WO202610250023', 1, 'PASS', '现场地面无油污残留，验收通过', DATE_SUB(NOW(), INTERVAL 72 HOUR)),
(19, 'WO202610250024', 1, 'PASS', '坑槽修补平整，行车无颠簸，验收通过', DATE_SUB(NOW(), INTERVAL 96 HOUR)),
(20, 'WO202610250025', 1, 'PASS', '夜间复查整段照明正常，验收通过', DATE_SUB(NOW(), INTERVAL 120 HOUR)),
(21, 'WO202610250026', 1, 'PASS', '无障碍扶手牢固，验收通过', DATE_SUB(NOW(), INTERVAL 136 HOUR));

-- ---------- 居民评分 ----------
INSERT INTO `t_work_rating`
(`id`, `work_order_id`, `work_order_no`, `worker_id`, `reporter_id`, `rating`, `tags`, `comment`, `create_time`)
VALUES
(3, 18, 'WO202610250023', 40, 12, 4, '处理及时', '冲洗得很干净，第二天早上就看不到油污了。', DATE_SUB(NOW(), INTERVAL 76 HOUR)),
(4, 19, 'WO202610250024', 2, 14, 5, '响应快,处理彻底', '坑槽补得很平，开车过去没有颠簸。', DATE_SUB(NOW(), INTERVAL 100 HOUR)),
(5, 20, 'WO202610250025', 37, 15, 4, '态度好,处理彻底', '灯修好了，晚上散步亮堂多了。', DATE_SUB(NOW(), INTERVAL 124 HOUR)),
(6, 21, 'WO202610250026', 32, 11, 5, '响应快', '扶手固定得很牢，老人用着放心。', DATE_SUB(NOW(), INTERVAL 140 HOUR));

-- ---------- 站内通知 ----------
USE city_event;
INSERT INTO `t_notification` (`user_id`, `title`, `content`, `biz_type`, `biz_id`, `is_read`, `create_time`)
VALUES
(4, '新的工单已派给你', '工单 WO202610250014（五四北路口绿化带内垃圾堆积）待接单，请分配给组员或自行处理。', 'WORK_ORDER_ASSIGNED', 9, 0, DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(2, '新的工单已派给你', '工单 WO202610250015（福马路人行道盲道砖破损）待接单，请分配给组员或自行处理。', 'WORK_ORDER_ASSIGNED', 10, 0, DATE_SUB(NOW(), INTERVAL 9 HOUR)),
(5, '新的工单已派给你', '工单 WO202610250016（儒江路人行道隔离墩倾斜）待接单，请分配给组员或自行处理。', 'WORK_ORDER_ASSIGNED', 11, 0, DATE_SUB(NOW(), INTERVAL 11 HOUR)),
(39, '新的工单已派给你', '工单 WO202610250017（金山工业园区垃圾集中点异味）已分配给你，请及时处理。', 'WORK_ORDER_ASSIGNED', 12, 0, DATE_SUB(NOW(), INTERVAL 29 HOUR)),
(34, '新的工单已派给你', '工单 WO202610250018（宁化支路人行道护栏缺失）已分配给你，请及时处理。', 'WORK_ORDER_ASSIGNED', 13, 0, DATE_SUB(NOW(), INTERVAL 25 HOUR)),
(38, '新的工单已派给你', '工单 WO202610250019（航城路一段路灯整排不亮）已分配给你，请及时处理。', 'WORK_ORDER_ASSIGNED', 14, 0, DATE_SUB(NOW(), INTERVAL 33 HOUR)),
(41, '新的工单已派给你', '工单 WO202610250020（晋安河支流河面漂浮油污）已分配给你，请及时处理。', 'WORK_ORDER_ASSIGNED', 15, 0, DATE_SUB(NOW(), INTERVAL 51 HOUR)),
(33, '新的工单已派给你', '工单 WO202610250021（梅峰路人行道树池篦子翘起）已分配给你，请及时处理。', 'WORK_ORDER_ASSIGNED', 16, 0, DATE_SUB(NOW(), INTERVAL 47 HOUR)),
(42, '新的工单已派给你', '工单 WO202610250022（上渡路人行天桥栏杆松动）已分配给你，请及时处理。', 'WORK_ORDER_ASSIGNED', 17, 0, DATE_SUB(NOW(), INTERVAL 43 HOUR)),
(40, '新的工单已派给你', '工单 WO202610250023（瀛洲路夜市收摊后地面油污）已分配给你，请及时处理。', 'WORK_ORDER_ASSIGNED', 18, 0, DATE_SUB(NOW(), INTERVAL 95 HOUR)),
(2, '新的工单已派给你', '工单 WO202610250024（罗星路路面坑槽影响行车）已分配给你，请及时处理。', 'WORK_ORDER_ASSIGNED', 19, 0, DATE_SUB(NOW(), INTERVAL 119 HOUR)),
(37, '新的工单已派给你', '工单 WO202610250025（岳峰路人行道灯长时间不亮）已分配给你，请及时处理。', 'WORK_ORDER_ASSIGNED', 20, 0, DATE_SUB(NOW(), INTERVAL 143 HOUR)),
(32, '新的工单已派给你', '工单 WO202610250026（温泉公园公厕无障碍扶手松动）已分配给你，请及时处理。', 'WORK_ORDER_ASSIGNED', 21, 0, DATE_SUB(NOW(), INTERVAL 159 HOUR));

-- ---------- AI 调用日志 ----------
USE city_ai;
INSERT INTO `t_ai_call_log`
(`id`, `trace_id`, `biz_type`, `user_id`, `model_name`, `prompt_version`, `request_summary`, `response_summary`, `parsed_ok`, `validation_result`, `latency_ms`, `prompt_tokens`, `completion_tokens`, `create_time`)
VALUES
(7, 'seed-trace-14', 'EVENT_ANALYZE', 15, 'glm-4-flash', 'v2',
 'title=茶园路人行道果皮箱满溢,images=1',
 '{"category":"ENVIRONMENT_SANITATION","priority":"HIGH","summary":"人行道果皮箱满溢，垃圾散落。"}',
 1, NULL, 800, 1200, 155, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(8, 'seed-trace-15', 'EVENT_ANALYZE', 11, 'glm-4-flash', 'v2',
 'title=东街口地铁站出口路灯闪烁,images=1',
 '{"category":"PUBLIC_LIGHTING","priority":"MEDIUM","summary":"地铁口三盏路灯闪烁，照明不稳。"}',
 1, NULL, 800, 1200, 155, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(9, 'seed-trace-16', 'EVENT_ANALYZE', 13, 'glm-4-flash', 'v2',
 'title=金山大道非机动车道路面凹,images=1',
 '{"category":"ROAD_FACILITY","priority":"HIGH","summary":"非机动车道路面凹陷积水。"}',
 1, NULL, 800, 1200, 155, DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(10, 'seed-trace-17', 'EVENT_ANALYZE', 12, 'glm-4-flash', 'v2',
 'title=滨江公园休闲座椅木板断裂,images=1',
 '{"category":"PUBLIC_FACILITY","priority":"HIGH","summary":"公园座椅木板断裂，铁钉外露。"}',
 1, NULL, 800, 1200, 155, DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(11, 'seed-trace-18', 'EVENT_ANALYZE', 13, 'glm-4-flash', 'v2',
 'title=吴航街道人行道地砖松动,images=1',
 '{"category":"ROAD_FACILITY","priority":"MEDIUM","summary":"人行道地砖松动，雨天溅水。"}',
 1, NULL, 800, 1200, 155, DATE_SUB(NOW(), INTERVAL 8 HOUR)),
(12, 'seed-trace-19', 'EVENT_ANALYZE', 14, 'glm-4-flash', 'v2',
 'title=罗星塔公园外侧垃圾房污水,images=1',
 '{"category":"ENVIRONMENT_SANITATION","priority":"HIGH","summary":"垃圾房污水外流，异味明显。"}',
 1, NULL, 800, 1200, 155, DATE_SUB(NOW(), INTERVAL 9 HOUR)),
(13, 'seed-trace-27', 'EVENT_ANALYZE', 13, 'glm-4-flash', 'v2',
 'title=金山工业园区垃圾集中点异,images=1',
 '{"category":"ENVIRONMENT_SANITATION","priority":"URGENT","summary":"园区垃圾集中点异味强烈。"}',
 1, NULL, 800, 1200, 155, DATE_SUB(NOW(), INTERVAL 30 HOUR)),
(14, 'seed-trace-30', 'EVENT_ANALYZE', 15, 'glm-4-flash', 'v2',
 'title=晋安河支流河面漂浮油污,images=1',
 '{"category":"ENVIRONMENT_SANITATION","priority":"URGENT","summary":"河面出现带状油污。"}',
 1, NULL, 800, 1200, 155, DATE_SUB(NOW(), INTERVAL 52 HOUR));

SET FOREIGN_KEY_CHECKS = 1;