package com.Momo.ChatBot.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

@Component
public class JwtAuthenticationFilter implements WebFilter {
    private final JwtUtil jwtUtil;

    // El constructor inyecta tu clase JwtUtil
    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    // Método central que se ejecuta con cada petición HTTP
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        // 1. Obtener la cabecera de autorización
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String token = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7); // Extraer el token sin "Bearer "
        }

        // 2. Si hay token y es válido (usando tu JwtUtil)
        if (token != null && jwtUtil.validateToken(token)) {

            // 3. Obtener el email del usuario desde el token
            String username = jwtUtil.getUsernameFromToken(token);

            // 4. Crear el objeto de autenticación de Spring Security
            UserDetails userDetails = new User(username, "", new ArrayList<>());
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            // 5. Establecer la autenticación en el contexto reactivo para que Spring sepa quién es el usuario
            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
        }

        // 6. Si no hay token o es inválido, dejar que la petición continúe para que SecurityConfig la bloquee
        return chain.filter(exchange);
    }
}
