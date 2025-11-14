package com.vatek.hrmtoolnextgen.config.security;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
@AllArgsConstructor
public class MethodSecurityConfig extends GlobalMethodSecurityConfiguration {

    private WebSecurityConfig webSecurityConfig;

    @Override
    protected AuthenticationManager authenticationManager() throws Exception {
        return super.authenticationManager();
    }

    @Override
    protected MethodSecurityExpressionHandler createExpressionHandler() {
        var d = new DefaultMethodSecurityExpressionHandler();
        d.setRoleHierarchy(webSecurityConfig.roleHierarchy());
        return d;
    }
}
