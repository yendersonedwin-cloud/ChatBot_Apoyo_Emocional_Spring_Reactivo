package com.Momo.ChatBot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;

@Configuration
// Usamos WebFluxConfigurer, no WebMvcConfigurer
public class WebConfig implements WebFluxConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Asegura que los archivos en /static/ sean accesibles en /
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}