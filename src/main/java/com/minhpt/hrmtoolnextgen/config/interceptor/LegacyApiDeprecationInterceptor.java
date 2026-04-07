package com.minhpt.hrmtoolnextgen.config.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.minhpt.hrmtoolnextgen.constant.ApiConstant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LegacyApiDeprecationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if (!ApiConstant.isLegacyPath(path)) {
            return true;
        }

        String successorPath = ApiConstant.toVersionedPath(path);
        response.setHeader("Deprecation", "true");
        response.setHeader("Sunset", ApiConstant.LEGACY_API_SUNSET);
        response.setHeader("Link", "<" + successorPath + ">; rel=\"successor-version\"");
        response.addHeader("Warning", "299 - \"Deprecated API, migrate to " + successorPath + "\"");
        return true;
    }
}