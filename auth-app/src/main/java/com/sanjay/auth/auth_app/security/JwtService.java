package com.sanjay.auth.auth_app.security;

import com.sanjay.auth.auth_app.entities.Role;
import com.sanjay.auth.auth_app.entities.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Data
public class JwtService {
    private final SecretKey key;
    private final long accessTtlSeconds;
    private final long refreshTtlSeconds;
    private final String issuer;

    public JwtService(
            @Value("${security.jwt.secret}") String secretKey,
            @Value("${security.jwt.access-ttl-seconds}") long accessTtlSeconds,
            @Value("${security.jwt.refresh-ttl-seconds}") long refreshTtlSeconds,
            @Value("${security.jwt.issuer}") String issuer){

        if (secretKey == null || secretKey.length()<64)
            throw new IllegalArgumentException("Invalid secret key");

        this.key= Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds=accessTtlSeconds;
        this.refreshTtlSeconds=refreshTtlSeconds;
        this.issuer=issuer;
    }

    //generate access token - only pass User
    public String generateAccessToken(User user){
        Instant now = Instant.now();
        List<String> roles =user.getRoles() == null ? List.of() :
                user.getRoles().stream().map(Role::getName).toList();

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
                .claims(Map.of(
                        "email",user.getEmail(),
                        "roles",user.getRoles(),
                        "typ","access"

                ))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();




    }

    // generate refresh Token - pass User with Id

    public String generateRefreshToken(User user , String jti){
        Instant now = Instant.now();



        return Jwts.builder()
                .id(jti)
                .subject(user.getId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now.plusSeconds(refreshTtlSeconds)))
                .claim("typ", "refresh")
                .signWith(key,SignatureAlgorithm.HS512)
                .compact();
    }

    //parse the token
    public Jws<Claims> parse(String token){
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
        }catch (JwtException e){
            throw e;
        }
    }

    //check is acces
    public boolean isAccessToken(String token){
        Claims s = parse(token).getPayload();
        return "access".equals(s.get("typ"));
    }

    // check token is refresh
    public  boolean isRefreshToken(String token){
        Claims c = (parse(token)).getPayload();
        return "refresh".equals(c.get("typ"));
    }

    // get user
    public UUID getUserId(String token){
        Claims c = parse(token).getPayload();
        return UUID.fromString(c.getSubject());
    }
    //get token id
    public String getJti(String token){
        return parse(token).getPayload().getId();
    }

}