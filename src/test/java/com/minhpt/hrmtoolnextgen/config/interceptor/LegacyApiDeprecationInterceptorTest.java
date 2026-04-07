package com.minhpt.hrmtoolnextgen.config.interceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.minhpt.hrmtoolnextgen.constant.ApiConstant;

class LegacyApiDeprecationInterceptorTest {

    @Test
    void shouldAddDeprecationHeadersForLegacyApiPath() throws Exception {
        LegacyApiDeprecationInterceptor interceptor = new LegacyApiDeprecationInterceptor();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.preHandle(request, response, new Object());

        assertEquals("true", response.getHeader("Deprecation"));
        assertEquals(ApiConstant.LEGACY_API_SUNSET, response.getHeader("Sunset"));
        assertEquals("</api/v1/auth/login>; rel=\"successor-version\"", response.getHeader("Link"));
    }
}