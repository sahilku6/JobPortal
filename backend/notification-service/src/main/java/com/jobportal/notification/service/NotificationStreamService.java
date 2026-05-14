package com.jobportal.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service @Slf4j
public class NotificationStreamService {

    private static final long SSE_TIMEOUT_MS = 0L;

    // userId (as String) → list of active emitters
    private final Map<String, CopyOnWriteArrayList<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        return subscribe(String.valueOf(userId));
    }

    public SseEmitter subscribe(String userId) {
        log.info("[SSE] New subscription: userId={}", userId);
        
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        
        log.info("[SSE] Emitter registered: userId={}, totalEmitters={}", userId, userEmitters.get(userId).size());
        
        emitter.onCompletion(() -> {
            log.info("[SSE] Emitter completed: userId={}", userId);
            removeEmitter(userId, emitter);
        });
        emitter.onTimeout(    () -> {
            log.info("[SSE] Emitter timeout: userId={}", userId);
            removeEmitter(userId, emitter);
        });
        emitter.onError(   ex -> {
            log.warn("[SSE] Emitter error: userId={}, error={}", userId, ex.getMessage());
            removeEmitter(userId, emitter);
        });
        
        try {
            emitter.send(SseEmitter.event().name("connected")
                .data(Map.of("timestamp", Instant.now().toString())));
            log.info("[SSE] Connection event sent: userId={}", userId);
        } catch (IOException ex) {
            log.error("[SSE] Failed to send connection event: userId={}, error={}", userId, ex.getMessage());
            removeEmitter(userId, emitter);
        }
        
        return emitter;
    }

    public void publishNotification(Long userId, Long notificationId) {
        publishNotification(String.valueOf(userId), String.valueOf(notificationId));
    }

    public void publishNotification(String userId, String notificationId) {
        log.info("[SSE] Publishing notification: notificationId={}, userId={}", notificationId, userId);
        
        CopyOnWriteArrayList<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            log.warn("[SSE] No active SSE emitters for userId={} (notification will not be sent in real-time)", userId);
            return;
        }

        log.info("[SSE] Found {} active emitters for userId={}, sending event...", emitters.size(), userId);
        int successCount = 0;
        int failureCount = 0;
        
        for (SseEmitter emitter : emitters) {
            try {
                log.debug("[SSE] Sending event to emitter: notificationId={}, userId={}", notificationId, userId);
                emitter.send(SseEmitter.event().name("notification").id(notificationId)
                    .data(Map.of("notificationId", notificationId)));
                successCount++;
                log.debug("[SSE] Event sent successfully: notificationId={}, userId={}", notificationId, userId);
            } catch (Exception ex) {
                log.warn("[SSE] Failed sending SSE to userId={}: {} - removing emitter", userId, ex.getMessage());
                removeEmitter(userId, emitter);
                failureCount++;
            }
        }
        log.info("[SSE] Publishing complete: notificationId={}, userId={}, success={}, failures={}", 
                 notificationId, userId, successCount, failureCount);
    }

    @Scheduled(fixedDelay = 25_000)
    public void sendHeartbeats() {
        userEmitters.forEach((userId, emitters) ->
            emitters.forEach(emitter -> {
                try { emitter.send(SseEmitter.event().name("heartbeat").data("ping")); }
                catch (Exception ex) { removeEmitter(userId, emitter); }
            }));
    }

    private void removeEmitter(String userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = userEmitters.get(userId);
        if (list == null) return;
        list.remove(emitter);
        if (list.isEmpty()) userEmitters.remove(userId);
    }
}
