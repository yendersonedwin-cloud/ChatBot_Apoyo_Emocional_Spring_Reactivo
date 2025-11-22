package com.Momo.ChatBot.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtParserBuilder; // Importación explícita
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    // Se inyecta la clave secreta desde application.properties
    @Value("${jwt.secret}")
    private String secret;

    // Tiempo de expiración del token por defecto (en horas)
    @Value("${jwt.expiration-hours}")
    private long expirationHours;

    /**
     * Genera un token JWT para un usuario.
     * @param email Email del usuario.
     * @param expiration Fecha y hora de expiración.
     * @return Token JWT.
     */
    public String generateToken(String email, LocalDateTime expiration) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, email, expiration);
    }

    // Método auxiliar para crear el token
    private String createToken(Map<String, Object> claims, String subject, LocalDateTime expiration) {
        // Convierte LocalDateTime a java.util.Date (necesario para JWT)
        Date expirationDate = Date.from(expiration.atZone(ZoneId.systemDefault()).toInstant());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject) // Email del usuario
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(expirationDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Valida si un token es válido.
     * @param token Token a validar.
     * @return true si el token es válido y no ha expirado.
     */
    public Boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            // Manejo de tokens inválidos (malformados, firma incorrecta, etc.)
            return false;
        }
    }

    /**
     * Verifica si el token ha expirado.
     */
    private Boolean isTokenExpired(String token) {
        final Date expiration = getClaimFromToken(token, Claims::getExpiration);
        return expiration.before(new Date());
    }

    /**
     * Obtiene un dato específico (Claim) del token.
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Obtiene todos los Claims (datos) del token.
     */
    private Claims getAllClaimsFromToken(String token) {
        // *** CORRECCIÓN CLAVE: Uso explícito de Jwts.parser()
        JwtParserBuilder parserBuilder = Jwts.parser();

        return parserBuilder
                .setSigningKey(getSigningKey())
                .build()
                // El método .parseClaimsJws(token) ha sido reemplazado por .parseSignedClaims(token) en las versiones modernas de JJWT
                .parseSignedClaims(token)
                .getBody();
    }

    /**
     * Obtiene el nombre de usuario (email) del token.
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }


    /**
     * Convierte la clave secreta de String a un objeto Key.
     */
    private Key getSigningKey() {
        // Asegura de que la clave secreta en application.properties sea base64
        byte[] keyBytes = Decoders.BASE64.decode(this.secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}