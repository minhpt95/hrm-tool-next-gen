package com.vatek.hrmtoolnextgen.config.security;

import com.vatek.hrmtoolnextgen.component.jwt.JwtAuthTokenFilter;
import com.vatek.hrmtoolnextgen.component.jwt.JwtProvider;
import com.vatek.hrmtoolnextgen.constant.RoleConstant;
import com.vatek.hrmtoolnextgen.repository.redis.UserTokenRedisRepository;
import com.vatek.hrmtoolnextgen.service.security.UserDetailsServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Log4j2
@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class WebSecurityConfig implements WebMvcConfigurer {
    private final UserDetailsServiceImpl userDetailsService;
    // Inject via the interface type to work with JDK dynamic proxies
    private final AuthenticationEntryPoint unauthorizedHandler;
    private final JwtProvider jwtProvider;
    private final UserTokenRedisRepository userTokenRedisRepository;

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
    public DefaultWebSecurityExpressionHandler webSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultWebSecurityExpressionHandler expressionHandler = new DefaultWebSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy);
        return expressionHandler;
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
                .csrf(AbstractHttpConfigurer::disable); // Use new lambda style for disabling


        // 2. Authorization Rules
        http
                .authorizeHttpRequests(authz -> authz
                        // Permit access to public endpoints
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**", // Permit Swagger UI
                                "/v3/api-docs/**" // Permit OpenAPI v3 docs (adjust path if necessary)
                        ).permitAll()

                        .requestMatchers(
                                "/error" // Allow Error
                        ).permitAll()

                        .requestMatchers(
                                "/actuator/health" // Health Check
                        ).permitAll()

                        .requestMatchers(
                                "/api/auth/login" // Login API
                        ).permitAll()

                        .requestMatchers(
                                "/api/admin",
                                "/api/admin/**"
                        ).hasAnyAuthority(RoleConstant.ADMIN, RoleConstant.IT_ADMIN)

                        .requestMatchers(
                                "/api/manager",
                                "/api/manager/**"
                        ).hasAuthority(RoleConstant.PROJECT_MANAGER)

                        .requestMatchers(
                                "/api/user",
                                "/api/user/**"
                        ).hasAuthority(RoleConstant.USER)

                        .anyRequest().authenticated()
                );

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
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        WebMvcConfigurer.super.addArgumentResolvers(resolvers);
    }
}

