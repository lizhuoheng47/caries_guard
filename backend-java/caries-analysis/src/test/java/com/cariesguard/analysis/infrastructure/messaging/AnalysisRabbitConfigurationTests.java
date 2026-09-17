package com.cariesguard.analysis.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cariesguard.analysis.config.AnalysisMessagingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;

class AnalysisRabbitConfigurationTests {

    @Test
    void shouldDeclareQuorumQueueWhenConfigured() {
        AnalysisMessagingProperties properties = new AnalysisMessagingProperties();
        properties.getRabbit().setQueueType("quorum");

        Queue queue = new AnalysisRabbitConfiguration().analysisRequestedQueue(properties);

        assertThat(queue.isDurable()).isTrue();
        assertThat(queue.getArguments()).containsEntry("x-queue-type", "quorum");
    }

    @Test
    void shouldRemainCompatibleWithClassicQueueByDefault() {
        AnalysisMessagingProperties properties = new AnalysisMessagingProperties();

        Queue queue = new AnalysisRabbitConfiguration().analysisRequestedQueue(properties);

        assertThat(queue.isDurable()).isTrue();
        assertThat(queue.getArguments()).doesNotContainKey("x-queue-type");
    }

    @Test
    void shouldRejectUnknownQueueTypes() {
        AnalysisMessagingProperties properties = new AnalysisMessagingProperties();

        assertThatThrownBy(() -> properties.getRabbit().setQueueType("stream"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("classic or quorum");
    }
}
