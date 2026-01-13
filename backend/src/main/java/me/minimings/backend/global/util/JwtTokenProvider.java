package me.minimings.backend.global.util;

import java.security.Key;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider{

    private final Logger LOGGER = LoggerFactory.getLogger(JwtTokenProvider.class);
    private final UserDetailsService userDetailsService;
    @Value("${jwt.secret}") // Same as the address in "application.yml"
    private String secretKey = "secretKey";
    private Key key;
    private final long tokenValidMillisecond = 1000L * 60 * 60; // Token validity: 1 hours

    // 1. Initialize the Key object by decoding the secret key
    @PostConstruct
    protected void init() {
        LOGGER.info("[init] JwtTokenProvider: Start init secretKey");
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        LOGGER.info("[init] JwtTokenProvider: Finish init secretKey");
    }

    // 2. generate a JWT token
    public String createToken(String userUid, List<String> roles){
        LOGGER.info("[createToken] JwtTokenProvider: Start token geration");
        Claims claims = Jwts.claims().setSubject(userUid);
        claims.put("roles", roles);
        Date now = new Date();

        String token = Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + tokenValidMillisecond))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        LOGGER.info("[createToken] Finish token geration");
        return token;
    }

    // 3. Get authentication information from token
    public Authentication getAuthentication(String token){
        LOGGER.info("[getAuthentication] Start Token Authentication Information Inquiry");
        UserDetails userDetails = userDetailsService.loadUserByUsername(this.getUsername(token));
        LOGGER.info("[getAuthentication] Finish Token Authentication Information Inquiry, UserDetails UserName : {}",
                userDetails.getUsername());
        return new UsernamePasswordAuthenticationToken(userDetails,"", userDetails.getAuthorities());
    }

    //4. Extract member identification information from token
    public String getUsername(String token){
        LOGGER.info("[getUsername] Start Token-based member identification information extraction");
        String info = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody()
                .getSubject();
        LOGGER.info("[getUsername] Finish Token-based member identification information extraction, info: {}", info);
        return info;
    }

    // 5. Extract token from HTTP header
    public String resolveToken(HttpServletRequest request){
        LOGGER.info("[resolveToken] Start Token Extraction from HTTP header");
        return request.getHeader("X-AUTH-TOKEN");
    }
    
    // 6. Validate token
    public boolean validateToken(String token){
        LOGGER.info("[validateToken] Start Token Validation");
        try{
            Jws<Claims> claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return !claims.getBody().getExpiration().before(new Date());
        } catch(Exception e){
            LOGGER.info("[validateToken] Token Validation Exception");
            return false;
        }
    }


}
