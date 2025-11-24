package com.Momo.ChatBot.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity // Habilita la seguridad reactiva de Spring WebFlux
@EnableReactiveMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // Se inyecta el filtro JWT que acabas de crear
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // Bean para el PasswordEncoder (requerido por UsuarioService y AuthController)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Define las reglas de acceso (Cumplimiento del RNF06)
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {

        // Deshabilita CSRF (típico en APIs REST)
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                // Agrega el filtro JWT en la cadena de seguridad (Paso 1 del Informe [cite: 53])
                .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                // Define las reglas de autorización
                .authorizeExchange(exchanges -> exchanges
                        // Permitir acceso libre a los endpoints de autenticación (Paso 2 del Informe [cite: 54])
                        .pathMatchers("/auth/**").permitAll()
                        // Proteger todos los demás endpoints, incluyendo /api/chat (Paso 3 del Informe [cite: 55])
                        .anyExchange().authenticated()
                );

        return http.build();
    }
}
