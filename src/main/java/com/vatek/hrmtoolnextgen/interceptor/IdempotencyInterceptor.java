package com.vatek.hrmtoolnextgen.interceptor;

import com.vatek.hrmtoolnextgen.annotation.Idempotent;
import com.vatek.hrmtoolnextgen.service.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Log4j2
public class IdempotencyInterceptor implements HandlerInterceptor {

    private static final String IDEMPOTENCY_KEY_HEADER = "idempotency-key";

    private final IdempotencyService idempotencyService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Only process if handler is a method handler
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        // Check if the method or class has @Idempotent annotation
        Idempotent idempotent = handlerMethod.getMethodAnnotation(Idempotent.class);
        if (idempotent == null) {
            idempotent = handlerMethod.getBeanType().getAnnotation(Idempotent.class);
        }

        // If not annotated, skip idempotency check
        if (idempotent == null) {
            return true;
        }

        // Get idempotency key from header
        String idempotencyKey = request.getHeader(IDEMPOTENCY_KEY_HEADER);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            // No idempotency key provided, continue normally
            return true;
        }

        // Check for cached response
        String method = request.getMethod();
        String path = request.getRequestURI();
        Optional<ResponseEntity<String>> cachedResponse = idempotencyService.getCachedResponse(
                idempotencyKey, method, path);

        if (cachedResponse.isPresent()) {
            // Return cached response
            ResponseEntity<String> responseEntity = cachedResponse.get();
            response.setStatus(responseEntity.getStatusCode().value());
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());

            PrintWriter writer = response.getWriter();
            writer.write(responseEntity.getBody());
            writer.flush();

            log.info("Returning cached response for idempotency key: {} on {} {}", idempotencyKey, method, path);
            return false; // Stop further processing
        }

        // Store idempotency key in request attribute for postHandle
        request.setAttribute("idempotency-key", idempotencyKey);
        request.setAttribute("idempotency-ttl", idempotent.ttlHours());
        request.setAttribute("idempotency-method", method);
        request.setAttribute("idempotency-path", path);

        return true; // Continue processing
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                          ModelAndView modelAndView) throws Exception {
        // Response body might not be fully written yet in postHandle
        // We'll handle caching in afterCompletion instead
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) throws Exception {
        // Check if we should store the response
        String idempotencyKey = (String) request.getAttribute("idempotency-key");
        if (idempotencyKey == null) {
            return;
        }

        // Don't cache if there was an exception
        if (ex != null) {
            log.debug("Not caching response for idempotency key {} due to exception", idempotencyKey);
            return;
        }

        // Only cache successful responses (2xx status codes)
        int statusCode = response.getStatus();
        if (statusCode < 200 || statusCode >= 300) {
            log.debug("Not caching response for idempotency key {} due to status code: {}", idempotencyKey, statusCode);
            return;
        }

        // Get response body from ContentCachingResponseWrapper if available
        String responseBody = null;
        if (response instanceof ContentCachingResponseWrapper wrappedResponse) {
            byte[] contentAsByteArray = wrappedResponse.getContentAsByteArray();
            if (contentAsByteArray.length > 0) {
                responseBody = new String(contentAsByteArray, StandardCharsets.UTF_8);
            }
        } else {
            log.warn("Response is not wrapped with ContentCachingResponseWrapper for idempotency key: {}", idempotencyKey);
        }

        if (responseBody != null && !responseBody.isBlank()) {
            String method = (String) request.getAttribute("idempotency-method");
            String path = (String) request.getAttribute("idempotency-path");
            Long ttlHours = (Long) request.getAttribute("idempotency-ttl");

            ResponseEntity<String> responseEntity = ResponseEntity
                    .status(statusCode)
                    .body(responseBody);

            idempotencyService.storeResponse(idempotencyKey, method, path, responseEntity, ttlHours);
        } else {
            log.debug("Response body is empty or null for idempotency key: {}", idempotencyKey);
        }
    }
}

