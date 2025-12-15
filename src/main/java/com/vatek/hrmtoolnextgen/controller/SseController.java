package com.vatek.hrmtoolnextgen.controller;

import com.vatek.hrmtoolnextgen.dto.principle.UserPrincipalDto;
import com.vatek.hrmtoolnextgen.service.SseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequiredArgsConstructor
@Log4j2
@RequestMapping("/sse")
@Tag(name = "Server-Sent Events", description = "Real-time event streaming APIs using Server-Sent Events (SSE)")
public class SseController {

    private final SseService sseService;

    /**
     * Establish SSE connection for authenticated user
     *
     * @param userPrincipal Authenticated user principal
     * @param request       HTTP request
     * @return SseEmitter for the connection
     */
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Establish SSE connection",
            description = "Establishes a Server-Sent Events (SSE) connection for the authenticated user. Returns an SseEmitter that streams real-time events to the client. The connection remains open until closed by the client or server."
    )
    public SseEmitter connect(
            @AuthenticationPrincipal UserPrincipalDto userPrincipal,
            HttpServletRequest request) {

        if (userPrincipal == null) {
            log.warn("Unauthenticated SSE connection attempt from: {}", request.getRemoteAddr());
            throw new com.vatek.hrmtoolnextgen.exception.BadRequestException("Authentication required");
        }

        String userId = String.valueOf(userPrincipal.getId());
        log.info("SSE connection request from user: {} at {}", userId, request.getRemoteAddr());

        return sseService.createConnection(userId);
    }

    /**
     * Get active connection count (admin endpoint)
     *
     * @param userPrincipal Authenticated user principal
     * @return Number of active connections
     */
    @GetMapping("/connections/count")
    @Operation(
            summary = "Get active SSE connection count",
            description = "Returns the total number of currently active SSE connections. Requires authentication."
    )
    public ResponseEntity<Integer> getConnectionCount(
            @AuthenticationPrincipal UserPrincipalDto userPrincipal) {

        if (userPrincipal == null) {
            throw new com.vatek.hrmtoolnextgen.exception.BadRequestException("Authentication required");
        }

        int count = sseService.getActiveConnectionCount();
        return ResponseEntity.ok(count);
    }

    /**
     * Test endpoint to send a test event to the authenticated user
     *
     * @param userPrincipal Authenticated user principal
     * @param message       Optional message to send
     * @return Success response
     */
    @PostMapping("/test")
    @Operation(
            summary = "Send test SSE event",
            description = "Sends a test event to the authenticated user via their active SSE connection. Useful for testing SSE functionality. Requires an active SSE connection."
    )
    public ResponseEntity<Map<String, String>> sendTestEvent(
            @AuthenticationPrincipal UserPrincipalDto userPrincipal,
            @RequestParam(required = false, defaultValue = "Test message") String message) {

        if (userPrincipal == null) {
            throw new com.vatek.hrmtoolnextgen.exception.BadRequestException("Authentication required");
        }

        String userId = String.valueOf(userPrincipal.getId());
        sseService.sendEvent(userId, "test", Map.of(
                "message", message,
                "userId", userId,
                "timestamp", java.time.LocalDateTime.now().toString()
        ));

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Test event sent to user: " + userId
        ));
    }
}

