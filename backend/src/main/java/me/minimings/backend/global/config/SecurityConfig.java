package me.minimings.backend.global.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;
import me.minimings.backend.global.filter.JwtAuthenticationFilter;
import me.minimings.backend.global.util.JwtTokenProvider;


// JWT
@Configuration // setting file
@EnableWebSecurity // turn on springsecurity
@RequiredArgsConstructor // constructor injection
public class SecurityConfig {

    // JwtTokenProvider
    private final JwtTokenProvider jwtTokenProvider;

    // Password encryption
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // security chain setting
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // It is not necessary because we used JWT
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
            .httpBasic(httpBasic -> httpBasic.disable())
            .formLogin(formLogin -> formLogin.disable())
            
            // Set addresses that can be accessed without authentication
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/user/signup", 
                                "/api/user/login"
                            ).permitAll()
                .anyRequest().authenticated() // other feature requires login
            )
            
            // Add JWT authentication filter
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), 
                       UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allow requests from React server (port 3000)
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        // Allow http method
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        // Allow header infomation(for JWT)
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type"));
        // Allow credentials(cookies, etc.)
        configuration.setAllowCredentials(true);
        // Expose Authorization header
        configuration.setExposedHeaders(Arrays.asList("Authorization"));

        // Apply CORS settings to all routes
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
    
}
