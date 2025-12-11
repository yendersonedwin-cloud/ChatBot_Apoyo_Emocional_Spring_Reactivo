package com.Momo.ChatBot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration // Indica que esta clase contiene definiciones de Beans
@EnableWebFlux
public class WebConfig {
    @Bean
    public WebFluxConfigurer corsConfigurer() {
        return new WebFluxConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Permite que cualquier origen (front-end) acceda a cualquier ruta.
                // En producción, se recomienda cambiar "*" por el dominio de tu front-end.
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        // Permitir los métodos HTTP estándar usados, incluyendo OPTIONS (para preflight)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        // Es crucial permitir el header Authorization para que el token JWT sea aceptado
                        .allowedHeaders("*");
            }
        };
    }
}
