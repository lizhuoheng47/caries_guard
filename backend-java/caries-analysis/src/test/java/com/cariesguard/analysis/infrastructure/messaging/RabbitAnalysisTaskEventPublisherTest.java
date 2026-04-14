package com.cariesguard.analysis.infrastructure.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.cariesguard.analysis.config.AnalysisMessagingProperties;
import com.cariesguard.analysis.domain.model.AnalysisCompletedEvent;
import com.cariesguard.analysis.domain.model.AnalysisFailedEvent;
import com.cariesguard.analysis.domain.model.AnalysisRequestedEvent;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class RabbitAnalysisTaskEventPublisherTest {

    private final RabbitTemplate rabbitTemplate = org.mockito.Mockito.mock(RabbitTemplate.class);
    private final AnalysisMessagingProperties properties = new AnalysisMessagingProperties();
    private final RabbitAnalysisTaskEventPublisher publisher = new RabbitAnalysisTaskEventPublisher(rabbitTemplate, properties);

    @Test
    void shouldPublishRequestedPayloadAsJsonMessage() {
        publisher.publishRequested(new AnalysisRequestedEvent(1L, "TASK001", "QUEUEING", "{\"taskNo\":\"TASK001\"}"));

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq("caries.analysis.exchange"), eq("analysis.requested"), messageCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(
                "{\"taskNo\":\"TASK001\"}",
                new String(messageCaptor.getValue().getBody(), java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test
    void shouldPublishCompletedEventToCompletedRoutingKey() {
        publisher.publishCompleted(new AnalysisCompletedEvent(2L, "TASK002", 3L, "caries-v1", LocalDateTime.now()));

        verify(rabbitTemplate).convertAndSend(
                eq("caries.analysis.exchange"),
                eq("analysis.completed"),
                any(AnalysisCompletedEvent.class),
                any(MessagePostProcessor.class));
    }

    @Test
    void shouldPublishFailedEventToFailedRoutingKey() {
        publisher.publishFailed(new AnalysisFailedEvent(4L, "TASK003", 5L, "network", LocalDateTime.now()));

        verify(rabbitTemplate).convertAndSend(
                eq("caries.analysis.exchange"),
                eq("analysis.failed"),
                any(AnalysisFailedEvent.class),
                any(MessagePostProcessor.class));
    }
}
