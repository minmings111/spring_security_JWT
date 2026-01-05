package me.minimings.backend.global.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider{

    private final Logger LOGGER = LoggerFactory.getLogger(JwtTokenProvider.class);
    @Value("${jwt.secret}") // Same as the address in "application.yml"
    private String secretKey = "secretKey";
    private final long tokenValidMillisecond = 1000L * 60 * 60; // Token validity: 1 hours

    // Define a secretKey to generate a token, and encode it in Base64.
    @PostConstruct
    protected void init() {
        LOGGER.info("[init] JwtTokenProvider: Start init secretKey");
        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes(StandardCharsets.UTF_8));
        LOGGER.info("[init] JwtTokenProvider: Finish init secretKey");
    }



}
