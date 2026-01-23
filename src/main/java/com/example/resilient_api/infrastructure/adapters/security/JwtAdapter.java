package com.example.resilient_api.infrastructure.adapters.security;

import com.example.resilient_api.domain.api.JwtPort;
import com.example.resilient_api.domain.enums.TechnicalMessage;
import com.example.resilient_api.domain.exceptions.BusinessException;
import com.example.resilient_api.domain.model.JwtPayload;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JwtAdapter implements JwtPort {

    private final SecretKey secretKey;
    private final long expirationTime;

    public JwtAdapter(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration:3600000}") long expirationTime) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationTime = expirationTime;
    }

    @Override
    public String generateToken(JwtPayload payload) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .subject(payload.email())
                .claim("userId", payload.userId())
                .claim("isAdmin", payload.isAdmin())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    @Override
    public Mono<JwtPayload> validateAndExtractPayload(String token) {
        return Mono.fromCallable(() -> {
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(secretKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                Long userId = claims.get("userId", Long.class);
                String email = claims.getSubject();
                Boolean isAdmin = claims.get("isAdmin", Boolean.class);

                return new JwtPayload(userId, email, isAdmin);
            } catch (ExpiredJwtException ex) {
                log.error("Token expired: {}", ex.getMessage());
                throw new BusinessException(TechnicalMessage.TOKEN_EXPIRED);
            } catch (SignatureException | MalformedJwtException ex) {
                log.error("Invalid token: {}", ex.getMessage());
                throw new BusinessException(TechnicalMessage.TOKEN_INVALID);
            } catch (Exception ex) {
                log.error("Error validating token: {}", ex.getMessage());
                throw new BusinessException(TechnicalMessage.TOKEN_INVALID);
            }
        });
    }
}
