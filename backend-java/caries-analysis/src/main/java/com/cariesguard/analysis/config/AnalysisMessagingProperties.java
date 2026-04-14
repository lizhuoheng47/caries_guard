package com.cariesguard.analysis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "caries.analysis.messaging")
public class AnalysisMessagingProperties {

    private String mode = "logging";
    private final Rabbit rabbit = new Rabbit();

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Rabbit getRabbit() {
        return rabbit;
    }

    public static class Rabbit {

        private String exchange = "caries.analysis.exchange";
        private String requestedQueue = "caries.analysis.requested.queue";
        private String completedQueue = "caries.analysis.completed.queue";
        private String failedQueue = "caries.analysis.failed.queue";
        private String requestedRoutingKey = "analysis.requested";
        private String completedRoutingKey = "analysis.completed";
        private String failedRoutingKey = "analysis.failed";

        public String getExchange() {
            return exchange;
        }

        public void setExchange(String exchange) {
            this.exchange = exchange;
        }

        public String getRequestedQueue() {
            return requestedQueue;
        }

        public void setRequestedQueue(String requestedQueue) {
            this.requestedQueue = requestedQueue;
        }

        public String getCompletedQueue() {
            return completedQueue;
        }

        public void setCompletedQueue(String completedQueue) {
            this.completedQueue = completedQueue;
        }

        public String getFailedQueue() {
            return failedQueue;
        }

        public void setFailedQueue(String failedQueue) {
            this.failedQueue = failedQueue;
        }

        public String getRequestedRoutingKey() {
            return requestedRoutingKey;
        }

        public void setRequestedRoutingKey(String requestedRoutingKey) {
            this.requestedRoutingKey = requestedRoutingKey;
        }

        public String getCompletedRoutingKey() {
            return completedRoutingKey;
        }

        public void setCompletedRoutingKey(String completedRoutingKey) {
            this.completedRoutingKey = completedRoutingKey;
        }

        public String getFailedRoutingKey() {
            return failedRoutingKey;
        }

        public void setFailedRoutingKey(String failedRoutingKey) {
            this.failedRoutingKey = failedRoutingKey;
        }
    }
}
