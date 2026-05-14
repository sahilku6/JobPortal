package com.jobportal.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaEventPayloadParser {

    private final ObjectMapper objectMapper;

    public <T> T parse(String payload, Class<T> eventType) {
        try {
            return objectMapper.readValue(payload, eventType);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid Kafka payload for " + eventType.getSimpleName(), ex);
        }
    }
}