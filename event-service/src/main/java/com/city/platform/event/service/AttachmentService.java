package com.city.platform.event.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.city.platform.common.core.exception.BizException;
import com.city.platform.common.core.exception.ErrorCode;
import com.city.platform.event.config.FileStorageProperties;
import com.city.platform.event.entity.Attachment;
import com.city.platform.event.mapper.AttachmentMapper;
import com.city.platform.event.vo.AttachmentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 附件：二进制落磁盘，数据库只保存元数据与访问地址。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {

    private static final Set<String> ALLOWED_BIZ_TYPES = Set.of(
            "EVENT_REPORT", "WORKORDER_RESULT", "WORKORDER_PROCESS", "USER_AVATAR");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");
    private static final DateTimeFormatter DIR_FORMAT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final AttachmentMapper attachmentMapper;
    private final FileStorageProperties properties;

    public AttachmentVO upload(MultipartFile file, String bizType, Long userId) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.FILE_TYPE_NOT_SUPPORTED, "文件为空");
        }
        if (!ALLOWED_BIZ_TYPES.contains(bizType)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "附件业务类型不合法");
        }
        if (file.getSize() > properties.getMaxSize()) {
            throw new BizException(ErrorCode.FILE_TOO_LARGE);
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BizException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }
        String extension = detectExtension(file, contentType);

        String relativeDir = LocalDate.now().format(DIR_FORMAT);
        Path directory = Paths.get(properties.getRoot(), relativeDir).toAbsolutePath();
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path target = directory.resolve(storedName);
        try {
            Files.createDirectories(directory);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            log.error("附件保存失败", e);
            throw new BizException(ErrorCode.SYSTEM_ERROR, "附件保存失败");
        }

        Attachment attachment = new Attachment();
        attachment.setBizType(bizType);
        attachment.setOwnerId(userId);
        attachment.setOriginalName(file.getOriginalFilename());
        attachment.setStoredName(storedName);
        attachment.setFilePath(relativeDir + "/" + storedName);
        attachment.setAccessUrl(properties.getAccessBaseUrl() + "/" + relativeDir + "/" + storedName);
        attachment.setContentType(contentType);
        attachment.setFileSize(file.getSize());
        attachment.setSha256(sha256(target));
        attachment.setStatus("TEMP");
        attachment.setCreateTime(java.time.LocalDateTime.now());
        attachment.setUpdateTime(java.time.LocalDateTime.now());
        attachmentMapper.insert(attachment);

        log.info("附件上传成功 attachmentId={} ownerId={} size={} type={}",
                attachment.getId(), userId, file.getSize(), contentType);
        return toVO(attachment);
    }

    /**
     * 绑定附件到业务：必须属于当前用户且仍为 TEMP，防止盗用他人附件。
     */
    public void bind(List<Long> attachmentIds, Long bizId, Long userId, String bizType) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            throw new BizException(ErrorCode.ATTACHMENT_INVALID, "至少上传 1 张照片");
        }
        List<Attachment> attachments = attachmentMapper.selectList(Wrappers.<Attachment>lambdaQuery()
                .in(Attachment::getId, attachmentIds)
                .eq(Attachment::getOwnerId, userId)
                .eq(Attachment::getBizType, bizType)
                .eq(Attachment::getStatus, "TEMP"));
        if (attachments.size() != attachmentIds.size()) {
            throw new BizException(ErrorCode.ATTACHMENT_INVALID, "附件不存在、已被使用或不属于当前用户");
        }
        for (Attachment attachment : attachments) {
            attachment.setStatus("BOUND");
            attachment.setBizId(bizId);
            attachmentMapper.updateById(attachment);
        }
    }

    /**
     * 工单结果附件绑定：允许 TEMP，或已绑定到同一工单（保证重试幂等）。
     */
    public void bindForWorkOrder(List<Long> attachmentIds, Long bizId, Long ownerId, String bizType) {
        if (attachmentIds == null || attachmentIds.isEmpty()) {
            throw new BizException(ErrorCode.ATTACHMENT_INVALID, "至少上传 1 张现场照片");
        }
        List<Attachment> attachments = attachmentMapper.selectList(Wrappers.<Attachment>lambdaQuery()
                .in(Attachment::getId, attachmentIds)
                .eq(Attachment::getOwnerId, ownerId)
                .eq(Attachment::getBizType, bizType)
                .and(w -> w.eq(Attachment::getStatus, "TEMP")
                        .or(n -> n.eq(Attachment::getStatus, "BOUND").eq(Attachment::getBizId, bizId))));
        if (attachments.size() != attachmentIds.size()) {
            throw new BizException(ErrorCode.ATTACHMENT_INVALID, "附件不存在、已被使用或不属于当前用户");
        }
        for (Attachment attachment : attachments) {
            if ("TEMP".equals(attachment.getStatus())) {
                attachment.setStatus("BOUND");
                attachment.setBizId(bizId);
                attachmentMapper.updateById(attachment);
            }
        }
    }

    public List<AttachmentVO> listByEvent(Long eventId) {
        return attachmentMapper.selectList(Wrappers.<Attachment>lambdaQuery()
                        .eq(Attachment::getBizId, eventId)
                        .eq(Attachment::getBizType, "EVENT_REPORT")
                        .eq(Attachment::getStatus, "BOUND")
                        .orderByAsc(Attachment::getId))
                .stream().map(this::toVO).toList();
    }

    /**
     * 事件已绑定照片数。
     */
    public int countByEvent(Long eventId) {
        Long count = attachmentMapper.selectCount(Wrappers.<Attachment>lambdaQuery()
                .eq(Attachment::getBizId, eventId)
                .eq(Attachment::getBizType, "EVENT_REPORT")
                .eq(Attachment::getStatus, "BOUND"));
        return count == null ? 0 : count.intValue();
    }

    public List<AttachmentVO> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return attachmentMapper.selectBatchIds(ids).stream().map(this::toVO).toList();
    }

    private String detectExtension(MultipartFile file, String contentType) {
        byte[] header = new byte[8];
        try (InputStream in = file.getInputStream()) {
            int read = in.read(header);
            if (read < 4) {
                throw new BizException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }
        boolean jpeg = (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
        boolean png = (header[0] & 0xFF) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G';
        if (jpeg && "image/jpeg".equals(contentType)) {
            return "jpg";
        }
        if (png && "image/png".equals(contentType)) {
            return "png";
        }
        throw new BizException(ErrorCode.FILE_TYPE_NOT_SUPPORTED, "文件内容与类型不匹配");
    }

    private String sha256(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            StringBuilder builder = new StringBuilder();
            for (byte b : digest.digest()) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private AttachmentVO toVO(Attachment attachment) {
        AttachmentVO vo = new AttachmentVO();
        vo.setId(attachment.getId());
        vo.setOriginalName(attachment.getOriginalName());
        vo.setAccessUrl(attachment.getAccessUrl());
        vo.setContentType(attachment.getContentType());
        vo.setFileSize(attachment.getFileSize());
        vo.setCreateTime(attachment.getCreateTime());
        return vo;
    }
}
