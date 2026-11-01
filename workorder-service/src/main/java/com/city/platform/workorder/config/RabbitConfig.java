package com.city.platform.workorder.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 与 event-service 声明同名交换机/队列（幂等），保证无论启动顺序如何消息都有队列可路由。
 */
@Configuration
public class RabbitConfig {

    public static final String WORKORDER_EXCHANGE = "city.workorder.topic";
    public static final String EVENT_SYNC_QUEUE = "city.event.sync.queue";
    public static final String NOTIFY_QUEUE = "city.notify.queue";
    public static final String WORKORDER_STATUS_KEY = "workorder.status.changed";
    public static final String WORKORDER_ASSIGNED_KEY = "workorder.assigned";

    public static final String DLX_EXCHANGE = "city.dlx.exchange";
    public static final String DLX_QUEUE = "city.dlx.queue";
    public static final String DLX_ROUTING_KEY = "dead";

    @Bean
    public TopicExchange workOrderExchange() {
        return new TopicExchange(WORKORDER_EXCHANGE, true, false);
    }

    @Bean
    public Queue eventSyncQueue() {
        return QueueBuilder.durable(EVENT_SYNC_QUEUE)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding eventSyncBinding(Queue eventSyncQueue, TopicExchange workOrderExchange) {
        return BindingBuilder.bind(eventSyncQueue).to(workOrderExchange).with(WORKORDER_STATUS_KEY);
    }

    @Bean
    public Queue notifyQueue() {
        return QueueBuilder.durable(NOTIFY_QUEUE)
                .deadLetterExchange(DLX_EXCHANGE)
                .deadLetterRoutingKey(DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding notifyBinding(Queue notifyQueue, TopicExchange workOrderExchange) {
        return BindingBuilder.bind(notifyQueue).to(workOrderExchange).with(WORKORDER_ASSIGNED_KEY);
    }

    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange(DLX_EXCHANGE, true, false);
    }

    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable(DLX_QUEUE).build();
    }

    @Bean
    public Binding dlxBinding(Queue dlxQueue, DirectExchange dlxExchange) {
        return BindingBuilder.bind(dlxQueue).to(dlxExchange).with(DLX_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
