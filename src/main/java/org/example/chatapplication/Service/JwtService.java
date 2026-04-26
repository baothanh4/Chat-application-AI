package org.example.chatapplication.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.example.chatapplication.Model.Entity.UserAccount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    public String generateToken(UserAccount user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusMillis(expirationMs);

        return Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claims(Map.of(
                        "uid", user.getId().toString(),
                        "role", user.getRole().name()
                ))
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Instant extractExpiration(String token) {
        Date expiration = extractAllClaims(token).getExpiration();
        return expiration == null ? Instant.EPOCH : expiration.toInstant();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equalsIgnoreCase(userDetails.getUsername())
                && extractExpiration(token).isAfter(Instant.now());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

