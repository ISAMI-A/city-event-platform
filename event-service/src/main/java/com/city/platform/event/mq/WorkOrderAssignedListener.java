package com.city.platform.event.mq;

import com.city.platform.common.api.dto.WorkOrderAssignedMessage;
import com.city.platform.event.config.RabbitConfig;
import com.city.platform.event.service.NotificationService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

/**
 * 工单分配通知消费者：Redis messageId 幂等 + 业务唯一索引 + 手动 ACK，失败进入死信队列。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkOrderAssignedListener {

    private static final String IDEMPOTENT_PREFIX = "city:mq:idempotent:notify:";

    private final NotificationService notificationService;
    private final StringRedisTemplate redisTemplate;

    @RabbitListener(queues = RabbitConfig.NOTIFY_QUEUE)
    public void onMessage(WorkOrderAssignedMessage message,
                          Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        String key = IDEMPOTENT_PREFIX + message.getMessageId();
        Boolean first = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofHours(24));
        if (!Boolean.TRUE.equals(first)) {
            log.info("重复分配通知已忽略 messageId={}", message.getMessageId());
            channel.basicAck(deliveryTag, false);
            return;
        }
        try {
            notificationService.handleAssigned(message);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            redisTemplate.delete(key);
            log.error("工单分配通知处理失败，进入死信队列 messageId={} traceId={}",
                    message.getMessageId(), message.getTraceId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
