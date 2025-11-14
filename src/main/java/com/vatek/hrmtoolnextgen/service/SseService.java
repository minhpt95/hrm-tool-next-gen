package com.vatek.hrmtoolnextgen.service;

import com.vatek.hrmtoolnextgen.dto.response.SseEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Log4j2
public class SseService {

    // Map of connectionId -> SseEmitter
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    // Map of userId -> connectionId for tracking user connections
    private final Map<String, String> userConnections = new ConcurrentHashMap<>();
    private static final long DEFAULT_TIMEOUT = 30 * 60 * 1000L; // 30 minutes

    /**
     * Create a new SSE connection for a user
     * @param userId User ID to identify the connection
     * @return SseEmitter instance
     */
    public SseEmitter createConnection(String userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        String connectionId = UUID.randomUUID().toString();
        
        emitter.onCompletion(() -> {
            log.info("SSE connection completed for user: {}", userId);
            emitters.remove(connectionId);
            userConnections.remove(userId);
        });
        
        emitter.onTimeout(() -> {
            log.info("SSE connection timeout for user: {}", userId);
            emitters.remove(connectionId);
            userConnections.remove(userId);
        });
        
        emitter.onError((ex) -> {
            log.error("SSE connection error for user: {}", userId, ex);
            emitters.remove(connectionId);
            userConnections.remove(userId);
        });

        emitters.put(connectionId, emitter);
        userConnections.put(userId, connectionId);
        log.info("SSE connection created for user: {} with connectionId: {}", userId, connectionId);
        
        // Send initial connection event
        try {
            emitter.send(SseEmitter.event()
                    .name("connection")
                    .data(Map.of("connectionId", connectionId, "status", "connected")));
        } catch (IOException e) {
            log.error("Error sending initial connection event", e);
        }
        
        return emitter;
    }

    /**
     * Send an event to a specific user
     * @param userId User ID
     * @param eventType Event type
     * @param data Event data
     */
    public void sendEvent(String userId, String eventType, Object data) {
        SseEventDto event = SseEventDto.create(eventType, data);
        sendEventToUser(userId, event);
    }

    /**
     * Send an event to a specific user with event DTO
     * @param userId User ID
     * @param event SSE Event DTO
     */
    public void sendEventToUser(String userId, SseEventDto event) {
        String connectionId = userConnections.get(userId);
        if (connectionId == null) {
            log.warn("No active connection found for user: {}", userId);
            return;
        }

        SseEmitter emitter = emitters.get(connectionId);
        if (emitter == null) {
            log.warn("Emitter not found for connectionId: {}", connectionId);
            userConnections.remove(userId);
            return;
        }

        try {
            SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                    .name(event.getEvent())
                    .data(event.getData());
            
            if (event.getId() != null) {
                eventBuilder.id(event.getId());
            }
            if (event.getComment() != null) {
                eventBuilder.comment(event.getComment());
            }
            
            emitter.send(eventBuilder);
        } catch (IOException e) {
            log.error("Error sending SSE event to user: {}", userId, e);
            emitters.remove(connectionId);
            userConnections.remove(userId);
        }
    }

    /**
     * Send an event to all connected users
     * @param eventType Event type
     * @param data Event data
     */
    public void broadcastEvent(String eventType, Object data) {
        SseEventDto event = SseEventDto.create(eventType, data);
        broadcastEvent(event);
    }

    /**
     * Broadcast an event to all connected users
     * @param event SSE Event DTO
     */
    public void broadcastEvent(SseEventDto event) {
        emitters.entrySet().removeIf(entry -> {
            try {
                SseEmitter emitter = entry.getValue();
                emitter.send(SseEmitter.event()
                        .id(event.getId())
                        .name(event.getEvent())
                        .data(event.getData())
                        .comment(event.getComment()));
                return false;
            } catch (IOException e) {
                log.error("Error broadcasting SSE event", e);
                return true; // Remove failed emitter
            }
        });
    }

    /**
     * Close connection for a specific user
     * @param userId User ID
     */
    public void closeConnection(String userId) {
        String connectionId = userConnections.remove(userId);
        if (connectionId != null) {
            SseEmitter emitter = emitters.remove(connectionId);
            if (emitter != null) {
                emitter.complete();
                log.info("SSE connection closed for user: {}", userId);
            }
        }
    }

    /**
     * Get the number of active connections
     * @return Number of active connections
     */
    public int getActiveConnectionCount() {
        return emitters.size();
    }

    /**
     * Check if a user has an active connection
     * @param userId User ID
     * @return true if user has active connection
     */
    public boolean hasActiveConnection(String userId) {
        String connectionId = userConnections.get(userId);
        if (connectionId == null) {
            return false;
        }
        SseEmitter emitter = emitters.get(connectionId);
        return emitter != null;
    }
}

