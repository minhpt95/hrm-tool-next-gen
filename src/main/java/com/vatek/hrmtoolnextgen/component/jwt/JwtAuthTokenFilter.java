package com.vatek.hrmtoolnextgen.component.jwt;

import com.vatek.hrmtoolnextgen.dto.principle.UserPrincipalDto;
import com.vatek.hrmtoolnextgen.entity.redis.UserTokenRedisEntity;
import com.vatek.hrmtoolnextgen.enumeration.EUserTokenType;
import com.vatek.hrmtoolnextgen.repository.redis.UserTokenRedisRepository;
import com.vatek.hrmtoolnextgen.service.security.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Log4j2
@RequiredArgsConstructor
public class JwtAuthTokenFilter extends OncePerRequestFilter {

    private final JwtProvider tokenProvider;
    private final UserDetailsServiceImpl userDetailsService;
    private final UserTokenRedisRepository userTokenRedisRepository;

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String jwt = getJwt(request);

            if(jwt == null){
                filterChain.doFilter(request, response);
                return;
            }

            if(!tokenProvider.validateJwtToken(jwt)){
                filterChain.doFilter(request,response);
                return;
            }

            String email = tokenProvider.getEmailFromJwtToken(jwt);
            Long userId = tokenProvider.getIdFromJwtToken(jwt);

            // Check if token exists in Redis (not invalidated)
            if (userId != null) {
                UserTokenRedisEntity storedToken = userTokenRedisRepository
                        .findUserByUserIdAndTokenType(userId, EUserTokenType.ACCESS_TOKEN);
                
                if (storedToken == null || !storedToken.getToken().equals(jwt)) {
                    // Token was invalidated (logout) or doesn't match
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            UserPrincipalDto userDetails = userDetailsService.loadUserByUsername(email);
            userDetails.setAccessToken(jwt);

            userDetails.setRemainTime(tokenProvider.getRemainTimeFromJwtToken(jwt));

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            log.error("Can NOT set user authentication -> Message", e);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwt(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if(StringUtils.isBlank(authHeader)){
            return null;
        }

        if (!StringUtils.containsIgnoreCase(authHeader,"Bearer ")) {
            return authHeader.trim();
        }

        return StringUtils.replaceIgnoreCase(authHeader,"bearer","",1).trim();
    }
}
