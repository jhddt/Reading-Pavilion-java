package com.jhddt.config.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Bean
    public DirectExchange reviewExchange(@Value("${review.mq.exchange}") String exchangeName) {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Queue reviewQueue(@Value("${review.mq.queue}") String queueName) {
        return new Queue(queueName, true);
    }

    @Bean
    public Binding reviewBinding(
            Queue reviewQueue,
            DirectExchange reviewExchange,
            @Value("${review.mq.routing-key}") String routingKey) {
        return BindingBuilder.bind(reviewQueue).to(reviewExchange).with(routingKey);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
