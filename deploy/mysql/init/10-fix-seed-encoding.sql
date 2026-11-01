-- 修复历史种子数据编码（早期初始化时客户端未声明 utf8mb4）
SET NAMES utf8mb4;

UPDATE city_user.t_user SET nickname = '系统管理员' WHERE id = 1;
UPDATE city_user.t_user SET nickname = '李道路' WHERE id = 2;
UPDATE city_user.t_user SET nickname = '王照明' WHERE id = 3;
UPDATE city_user.t_user SET nickname = '张环卫' WHERE id = 4;
UPDATE city_user.t_user SET nickname = '演示居民' WHERE id = 100;
UPDATE city_user.t_user SET nickname = '其他居民' WHERE id = 101;

UPDATE city_user.t_role SET role_name = '居民', description = '上报事件与查看进度' WHERE role_code = 'RESIDENT';
UPDATE city_user.t_role SET role_name = '处理人员', description = '接单与提交处理结果' WHERE role_code = 'WORKER';
UPDATE city_user.t_role SET role_name = '管理员', description = '审核、派单、验收与用户管理' WHERE role_code = 'ADMIN';

UPDATE city_user.t_worker_profile SET dept_name = '市政道路班组' WHERE user_id = 2;
UPDATE city_user.t_worker_profile SET dept_name = '路灯照明班组' WHERE user_id = 3;
UPDATE city_user.t_worker_profile SET dept_name = '环卫保洁班组' WHERE user_id = 4;

UPDATE city_event.t_event_category SET name = '道路设施', description = '路面破损、井盖缺失、护栏损坏' WHERE code = 'ROAD_FACILITY';
UPDATE city_event.t_event_category SET name = '公共照明', description = '路灯不亮、线路裸露、灯杆倾斜' WHERE code = 'PUBLIC_LIGHTING';
UPDATE city_event.t_event_category SET name = '环境卫生', description = '垃圾堆积、污水外溢、乱贴乱画' WHERE code = 'ENVIRONMENT_SANITATION';
UPDATE city_event.t_event_category SET name = '公共设施', description = '座椅、健身器材、公厕设施损坏' WHERE code = 'PUBLIC_FACILITY';
UPDATE city_event.t_event_category SET name = '其他', description = '无法归类，必须人工确认' WHERE code = 'OTHER';

UPDATE city_workorder.t_work_order w
JOIN city_user.t_user u ON u.id = w.assignee_id
SET w.assignee_name = u.nickname;
