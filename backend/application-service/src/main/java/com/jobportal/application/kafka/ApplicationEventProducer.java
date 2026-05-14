package com.jobportal.application.kafka;

import com.jobportal.application.event.ApplicationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationEventProducer {

    private final KafkaTemplate<String, ApplicationEvent> kafkaTemplate;

    @Value("${app.kafka.topics.application-events:application-events}")
    private String topic;

    public void publish(ApplicationEvent event) {
        log.info("[EVENT-PRODUCER] Publishing application event: action={} id={} userId={} status={} applicantEmail={}",
                 event.getAction(), event.getId(), event.getUserId(), event.getStatus(), event.getApplicantEmail());
        
        kafkaTemplate.send(topic, event.getId() != null ? event.getId().toString() : null, event)
                .whenComplete((res, ex) -> {
                    if (ex != null) {
                        log.error("[EVENT-PRODUCER] Failed to publish application event: action={} id={} error={}", 
                                 event.getAction(), event.getId(), ex.getMessage(), ex);
                    } else {
                        log.info("[EVENT-PRODUCER] Published successfully: action={} id={} partition={} offset={} topic={}",
                                event.getAction(), event.getId(), res.getRecordMetadata().partition(),
                                res.getRecordMetadata().offset(), res.getRecordMetadata().topic());
                    }
                });
    }
}
