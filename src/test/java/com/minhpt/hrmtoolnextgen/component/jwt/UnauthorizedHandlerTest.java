package com.minhpt.hrmtoolnextgen.component.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

import com.fasterxml.jackson.databind.ObjectMapper;

class UnauthorizedHandlerTest {

    @Test
    void shouldWriteUnauthorizedJsonResponse() throws Exception {
        UnauthorizedHandler handler = new UnauthorizedHandler(new ObjectMapper());
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(80);
        request.setRequestURI("/api/auth/login");

        handler.commence(request, response, new BadCredentialsException("Invalid credentials"));

        assertEquals(401, response.getStatus());
        assertEquals("application/json", response.getContentType());
        assertTrue(response.getContentAsString().contains("Invalid credentials"));
        assertTrue(response.getContentAsString().contains("/api/auth/login"));
    }
}