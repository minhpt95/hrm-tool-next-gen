package com.vatek.hrmtoolnextgen.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

/**
 * Filter to wrap response for idempotency handling.
 * This allows us to read the response body multiple times.
 * Must run after Spring Security filters but before the interceptor.
 */
@Component
@Order(100)
@Log4j2
public class IdempotencyFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Only wrap if idempotency-key header is present
        String idempotencyKey = request.getHeader("idempotency-key");
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

            try {
                filterChain.doFilter(request, wrappedResponse);
            } finally {
                // Copy the cached response body to the actual response
                wrappedResponse.copyBodyToResponse();
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }
}

