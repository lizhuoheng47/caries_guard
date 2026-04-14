package com.cariesguard.analysis.infrastructure.messaging;

import com.cariesguard.analysis.config.AnalysisMessagingProperties;
import com.cariesguard.analysis.domain.model.AnalysisCompletedEvent;
import com.cariesguard.analysis.domain.model.AnalysisFailedEvent;
import com.cariesguard.analysis.domain.model.AnalysisRequestedEvent;
import com.cariesguard.analysis.domain.service.AnalysisTaskEventPublisher;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(prefix = "caries.analysis.messaging", name = "mode", havingValue = "rabbit")
public class RabbitAnalysisTaskEventPublisher implements AnalysisTaskEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RabbitAnalysisTaskEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final AnalysisMessagingProperties properties;

    public RabbitAnalysisTaskEventPublisher(RabbitTemplate rabbitTemplate, AnalysisMessagingProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public void publishRequested(AnalysisRequestedEvent event) {
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setHeader("eventType", "analysis.requested");
        messageProperties.setHeader("taskId", event.taskId());
        messageProperties.setHeader("taskNo", event.taskNo());
        messageProperties.setHeader("taskStatusCode", event.taskStatusCode());
        Message message = new Message(resolveRequestedPayload(event).getBytes(StandardCharsets.UTF_8), messageProperties);
        rabbitTemplate.send(properties.getRabbit().getExchange(), properties.getRabbit().getRequestedRoutingKey(), message);
        log.info("analysis.requested published exchange={} routingKey={} taskId={} taskNo={}",
                properties.getRabbit().getExchange(),
                properties.getRabbit().getRequestedRoutingKey(),
                event.taskId(),
                event.taskNo());
    }

    @Override
    public void publishCompleted(AnalysisCompletedEvent event) {
        rabbitTemplate.convertAndSend(
                properties.getRabbit().getExchange(),
                properties.getRabbit().getCompletedRoutingKey(),
                event,
                message -> {
                    message.getMessageProperties().setHeader("eventType", "analysis.completed");
                    message.getMessageProperties().setHeader("taskId", event.taskId());
                    message.getMessageProperties().setHeader("taskNo", event.taskNo());
                    message.getMessageProperties().setHeader("caseId", event.caseId());
                    return message;
                });
        log.info("analysis.completed published exchange={} routingKey={} taskId={} taskNo={}",
                properties.getRabbit().getExchange(),
                properties.getRabbit().getCompletedRoutingKey(),
                event.taskId(),
                event.taskNo());
    }

    @Override
    public void publishFailed(AnalysisFailedEvent event) {
        rabbitTemplate.convertAndSend(
                properties.getRabbit().getExchange(),
                properties.getRabbit().getFailedRoutingKey(),
                event,
                message -> {
                    message.getMessageProperties().setHeader("eventType", "analysis.failed");
                    message.getMessageProperties().setHeader("taskId", event.taskId());
                    message.getMessageProperties().setHeader("taskNo", event.taskNo());
                    message.getMessageProperties().setHeader("caseId", event.caseId());
                    return message;
                });
        log.info("analysis.failed published exchange={} routingKey={} taskId={} taskNo={}",
                properties.getRabbit().getExchange(),
                properties.getRabbit().getFailedRoutingKey(),
                event.taskId(),
                event.taskNo());
    }

    private String resolveRequestedPayload(AnalysisRequestedEvent event) {
        return StringUtils.hasText(event.payloadJson())
                ? event.payloadJson()
                : "{}";
    }
}
