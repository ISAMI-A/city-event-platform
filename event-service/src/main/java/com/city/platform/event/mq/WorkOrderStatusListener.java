package com.city.platform.event.mq;

import com.city.platform.common.api.dto.WorkOrderStatusMessage;
import com.city.platform.event.config.RabbitConfig;
import com.city.platform.event.service.EventStatusSyncService;
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
 * 消费工单状态变更消息：Redis 幂等键 + 手动 ACK，失败进入死信队列。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkOrderStatusListener {

    private static final String IDEMPOTENT_PREFIX = "city:mq:idempotent:";

    private final EventStatusSyncService syncService;
    private final StringRedisTemplate redisTemplate;

    @RabbitListener(queues = RabbitConfig.EVENT_SYNC_QUEUE)
    public void onMessage(WorkOrderStatusMessage message,
                          Channel channel,
                          @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        String key = IDEMPOTENT_PREFIX + message.getMessageId();
        Boolean first = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofHours(24));
        if (!Boolean.TRUE.equals(first)) {
            log.info("重复消息已忽略 messageId={}", message.getMessageId());
            channel.basicAck(deliveryTag, false);
            return;
        }
        try {
            syncService.sync(message);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            redisTemplate.delete(key);
            log.error("事件状态同步失败，消息进入死信队列 messageId={} traceId={}",
                    message.getMessageId(), message.getTraceId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
