package com.example.datalake.ingestionsvc.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtUtil {
    /**
     * Base64 string injected from the configuration file; length should be ≥ 256 bits (i.e., 32 bytes)
     * application.yml:
     * ingestion:
     *   jwt:
     *     secret:
     */
    @Value("${ingestion.jwt.secret}")
    private String secret;

    /** Generate the symmetric secret key object */
    private SecretKey key() {
        // Decode the Base64 string into a byte array
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
    }

    /**
     * Issue a one-time upload token
     *
     * @param filename The name of the file to be uploaded
     * @param ttl      The token's time-to-live (validity duration)
     * @return JWT string
     */
    public String issue(String filename, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(ttl)))
                .claim("filename", filename)
                .signWith(key())
                .compact();
    }

    /**
     * Verify and parse the token
     *
     * @param token JWT
     * @return Claims object. Callers can use get("filename", String.class) to retrieve the filename.
     * @throws io.jsonwebtoken.JwtException Any validation failure (e.g., expiration or tampering) will throw this exception
     */
    public Claims verify(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

}
