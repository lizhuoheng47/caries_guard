package com.cariesguard.analysis.infrastructure.messaging;

import com.cariesguard.analysis.config.AnalysisMessagingProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "caries.analysis.messaging", name = "mode", havingValue = "rabbit")
public class AnalysisRabbitConfiguration {

    @Bean
    public TopicExchange analysisEventExchange(AnalysisMessagingProperties properties) {
        return new TopicExchange(properties.getRabbit().getExchange(), true, false);
    }

    @Bean
    public Queue analysisRequestedQueue(AnalysisMessagingProperties properties) {
        return durableQueue(properties.getRabbit().getRequestedQueue(), properties);
    }

    @Bean
    public Queue analysisCompletedQueue(AnalysisMessagingProperties properties) {
        return durableQueue(properties.getRabbit().getCompletedQueue(), properties);
    }

    @Bean
    public Queue analysisFailedQueue(AnalysisMessagingProperties properties) {
        return durableQueue(properties.getRabbit().getFailedQueue(), properties);
    }

    @Bean
    public Binding analysisRequestedBinding(TopicExchange analysisEventExchange,
                                            Queue analysisRequestedQueue,
                                            AnalysisMessagingProperties properties) {
        return BindingBuilder.bind(analysisRequestedQueue)
                .to(analysisEventExchange)
                .with(properties.getRabbit().getRequestedRoutingKey());
    }

    @Bean
    public Binding analysisCompletedBinding(TopicExchange analysisEventExchange,
                                            Queue analysisCompletedQueue,
                                            AnalysisMessagingProperties properties) {
        return BindingBuilder.bind(analysisCompletedQueue)
                .to(analysisEventExchange)
                .with(properties.getRabbit().getCompletedRoutingKey());
    }

    @Bean
    public Binding analysisFailedBinding(TopicExchange analysisEventExchange,
                                         Queue analysisFailedQueue,
                                         AnalysisMessagingProperties properties) {
        return BindingBuilder.bind(analysisFailedQueue)
                .to(analysisEventExchange)
                .with(properties.getRabbit().getFailedRoutingKey());
    }

    @Bean
    @ConditionalOnMissingBean(MessageConverter.class)
    public MessageConverter analysisRabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    private Queue durableQueue(String name, AnalysisMessagingProperties properties) {
        QueueBuilder builder = QueueBuilder.durable(name);
        if ("quorum".equalsIgnoreCase(properties.getRabbit().getQueueType())) {
            builder.quorum();
        }
        return builder.build();
    }
}
