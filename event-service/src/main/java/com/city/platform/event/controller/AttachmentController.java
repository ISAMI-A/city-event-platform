package com.city.platform.event.controller;

import com.city.platform.common.core.model.R;
import com.city.platform.common.security.context.UserContext;
import com.city.platform.event.service.AttachmentService;
import com.city.platform.event.vo.AttachmentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping
    public R<AttachmentVO> upload(@RequestPart("file") MultipartFile file,
                                  @RequestParam("bizType") String bizType) {
        return R.ok(attachmentService.upload(file, bizType, UserContext.requireUserId()));
    }

    @GetMapping("/batch")
    public R<List<AttachmentVO>> batch(@RequestParam("ids") List<Long> ids) {
        return R.ok(attachmentService.listByIds(ids));
    }
}
