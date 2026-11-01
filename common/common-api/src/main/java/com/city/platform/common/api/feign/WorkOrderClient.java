package com.city.platform.common.api.feign;

import com.city.platform.common.api.dto.CreateWorkOrderCommand;
import com.city.platform.common.api.dto.EventRating;
import com.city.platform.common.api.dto.WorkOrderBrief;
import com.city.platform.common.core.model.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * workorder-service 内部契约：审核通过时创建工单、事件详情查询工单摘要。
 */
@FeignClient(name = "workorder-service", contextId = "workOrderClient", path = "/internal/v1/workorders",
        url = "${city.feign.workorder-service-url:}")
public interface WorkOrderClient {

    @PostMapping
    R<WorkOrderBrief> create(@RequestBody CreateWorkOrderCommand command);

    @GetMapping("/by-event/{eventId}")
    R<WorkOrderBrief> getByEvent(@PathVariable("eventId") Long eventId);

    /**
     * 批量查询事件对应的工单与居民评价，供事件列表展示星级。
     */
    @GetMapping("/ratings")
    R<List<EventRating>> ratings(@RequestParam("eventIds") String eventIds);
}
