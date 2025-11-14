package com.vatek.hrmtoolnextgen.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration for Server-Sent Events (SSE)
 * Ensures proper CORS and headers for SSE connections
 */
@Configuration
public class SseConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/sse/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Cache-Control", "Content-Length", "Content-Type")
                .allowCredentials(false)
                .maxAge(3600);
    }
}

