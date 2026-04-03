package me.minimings.backend.global.filter;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.minimings.backend.global.util.JwtTokenProvider;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtTokenProvider jwtTokenProvider;

    // exclude URL setting
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String[] excludePath = {"/api/user/signup", "/api/user/login"};
        String path = request.getRequestURI();
        return Arrays.asList(excludePath).contains(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain filterChain) throws ServletException, IOException {
        log.info("JwtAuthenticationFilter : jwt authentication processing");

        String jwtToken = parseJwt(request);
        log.info("JWT token present: {}", jwtToken != null);

        if (jwtToken != null) {
            if (jwtTokenProvider.validateToken(jwtToken)) {
                Authentication auth = jwtTokenProvider.getAuthentication(jwtToken);
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.info("JWT token validation success");
            } else {
                log.error("Invalid or expired JWT token");
                throw new RuntimeException("Invalid or expired JWT token");
            }
        } else {
            log.error("JWT token is missing");
            throw new RuntimeException("JWT token is missing");
        }
        filterChain.doFilter(request, response);
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }

    
}