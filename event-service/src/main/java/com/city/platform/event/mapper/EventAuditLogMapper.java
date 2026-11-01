package com.city.platform.event.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.city.platform.event.entity.EventAuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EventAuditLogMapper extends BaseMapper<EventAuditLog> {
}
