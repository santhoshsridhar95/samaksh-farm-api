package com.samaksh.farms.auth.jwt;

import com.samaksh.farms.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final String secret;

    private final long expirationMillis;

    public JwtService(
            @Value("${app.security.jwt.secret}") String secret,
            @Value("${app.security.jwt.expiration-ms:86400000}")
            long expirationMillis
    ) {

        if (secret == null
                || secret.getBytes(StandardCharsets.UTF_8).length < 32) {

            throw new IllegalStateException(
                    "JWT secret must be configured and at least 32 bytes long"
            );
        }

        this.secret = secret;
        this.expirationMillis = expirationMillis;
    }

    private SecretKey getSigningKey() {

        return Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateToken(
            User user
    ) {

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .claim("name", user.getName())
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + expirationMillis
                        )
                )
                .signWith(
                        getSigningKey()
                )
                .compact();
    }

    public String extractEmail(
            String token
    ) {

        Claims claims =
                Jwts.parser()
                        .verifyWith(
                                getSigningKey()
                        )
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

        return claims.getSubject();
    }

    public boolean isTokenValid(
            String token
    ) {

        try {

            extractEmail(token);
            return true;

        } catch (JwtException | IllegalArgumentException ex) {

            return false;
        }
    }
}
