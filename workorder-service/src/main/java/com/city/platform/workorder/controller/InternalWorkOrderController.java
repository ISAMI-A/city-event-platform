package com.city.platform.workorder.controller;

import com.city.platform.common.api.dto.CreateWorkOrderCommand;
import com.city.platform.common.api.dto.WorkOrderBrief;
import com.city.platform.common.core.model.R;
import com.city.platform.workorder.service.WorkOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/workorders")
@RequiredArgsConstructor
public class InternalWorkOrderController {

    private final WorkOrderService workOrderService;

    @PostMapping
    public R<WorkOrderBrief> create(@Valid @RequestBody CreateWorkOrderCommand command) {
        return R.ok(workOrderService.createInternal(command));
    }

    @GetMapping("/by-event/{eventId}")
    public R<WorkOrderBrief> byEvent(@PathVariable("eventId") Long eventId) {
        return R.ok(workOrderService.findByEvent(eventId));
    }

    @GetMapping("/ratings")
    public R<java.util.List<com.city.platform.common.api.dto.EventRating>> ratings(
            @RequestParam("eventIds") String eventIds) {
        return R.ok(workOrderService.ratings(eventIds));
    }
}
