package com.minhpt.hrmtoolnextgen.config.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.minhpt.hrmtoolnextgen.component.jwt.JwtAuthTokenFilter;
import com.minhpt.hrmtoolnextgen.component.jwt.JwtProvider;
import com.minhpt.hrmtoolnextgen.constant.ApiConstant;
import com.minhpt.hrmtoolnextgen.constant.RoleConstant;
import com.minhpt.hrmtoolnextgen.repository.redis.UserTokenRedisRepository;
import com.minhpt.hrmtoolnextgen.service.security.UserDetailsServiceImpl;

import lombok.extern.log4j.Log4j2;
import lombok.RequiredArgsConstructor;

@Log4j2
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig implements WebMvcConfigurer {
    private final UserDetailsServiceImpl userDetailsService;
    // Inject via the interface type to work with JDK dynamic proxies
    private final AuthenticationEntryPoint unauthorizedHandler;
    private final JwtProvider jwtProvider;
    private final UserTokenRedisRepository userTokenRedisRepository;

        @Value("${hrm.security.swagger-enabled:false}")
        private boolean swaggerEnabled;

    @Bean
    public JwtAuthTokenFilter authenticationJwtTokenFilter() {
        return new JwtAuthTokenFilter(jwtProvider, userDetailsService, userTokenRedisRepository);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        var authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());

        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfiguration) throws Exception {
        return authConfiguration.getAuthenticationManager();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        String hierarchy =
                RoleConstant.ADMIN + " > " + RoleConstant.PROJECT_MANAGER + "\n" +
                        RoleConstant.ADMIN + " > " + RoleConstant.HR + "\n" +
                        RoleConstant.IT_ADMIN + " > " + RoleConstant.PROJECT_MANAGER + "\n" +
                        RoleConstant.IT_ADMIN + " > " + RoleConstant.HR + "\n" +
                        RoleConstant.PROJECT_MANAGER + " > " + RoleConstant.USER + "\n" +
                        RoleConstant.HR + " > " + RoleConstant.USER;
        return RoleHierarchyImpl.fromHierarchy(hierarchy);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCryptPasswordEncoder.BCryptVersion.$2Y, 10);
    }

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        // 1. CORS and CSRF Configuration
        http
                .cors(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable);

        // 2. Security response headers
        http
                .headers(headers -> headers
                        .contentTypeOptions(ct -> {}) // X-Content-Type-Options: nosniff
                        .frameOptions(fo -> fo.deny())  // X-Frame-Options: DENY
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31_536_000))
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'self'; frame-ancestors 'none'"))
                );


        // 2. Authorization Rules
        http
                .authorizeHttpRequests(authz -> {
                    if (swaggerEnabled) {
                        authz.requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll();
                    } else {
                        authz.requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).denyAll();
                    }

                    authz.requestMatchers(
                            "/error"
                    ).permitAll();

                    authz.requestMatchers(
                            "/actuator/health"
                    ).permitAll();

                    authz.requestMatchers(
                            ApiConstant.AUTH_BASE + "/**",
                            ApiConstant.AUTH_V1_BASE + "/**"
                    ).permitAll();

                    authz.requestMatchers(
                            ApiConstant.ADMIN_BASE,
                            ApiConstant.ADMIN_BASE + "/**",
                            ApiConstant.ADMIN_V1_BASE,
                            ApiConstant.ADMIN_V1_BASE + "/**"
                    ).hasAnyAuthority(RoleConstant.ADMIN, RoleConstant.IT_ADMIN);

                    authz.requestMatchers(
                            ApiConstant.MANAGER_BASE,
                            ApiConstant.MANAGER_BASE + "/**",
                            ApiConstant.MANAGER_V1_BASE,
                            ApiConstant.MANAGER_V1_BASE + "/**"
                    ).hasAuthority(RoleConstant.PROJECT_MANAGER);

                    authz.requestMatchers(
                            ApiConstant.USER_BASE,
                            ApiConstant.USER_BASE + "/**",
                            ApiConstant.USER_V1_BASE,
                            ApiConstant.USER_V1_BASE + "/**",
                            ApiConstant.SSE_BASE,
                            ApiConstant.SSE_BASE + "/**",
                            ApiConstant.SSE_V1_BASE,
                            ApiConstant.SSE_V1_BASE + "/**"
                    ).hasAnyAuthority(
                            RoleConstant.USER,
                            RoleConstant.PROJECT_MANAGER,
                            RoleConstant.HR,
                            RoleConstant.ADMIN,
                            RoleConstant.IT_ADMIN
                    );

                    authz.requestMatchers(
                            ApiConstant.HOLIDAYS_BASE,
                            ApiConstant.HOLIDAYS_BASE + "/**",
                            ApiConstant.HOLIDAYS_V1_BASE,
                            ApiConstant.HOLIDAYS_V1_BASE + "/**"
                    ).authenticated();

                    authz.anyRequest().authenticated();
                });

        http
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(unauthorizedHandler)
                );

        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );


        http.addFilterBefore(authenticationJwtTokenFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Override
    public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> resolvers) {
        WebMvcConfigurer.super.addArgumentResolvers(resolvers);
    }
}

