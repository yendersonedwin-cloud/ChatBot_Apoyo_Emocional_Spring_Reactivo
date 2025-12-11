package com.Momo.ChatBot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean; // <--- Nuevo Import
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // <--- Nuevo Import
import org.springframework.security.crypto.password.PasswordEncoder; // <--- Nuevo Import

// IMPORTANTE: Excluir la autoconfiguración de seguridad hasta que implementemos JWT
@SpringBootApplication(exclude = {org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration.class, org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration.class})
public class ChatBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatBotApplication.class, args);
    }

    /**
     * Bean para encriptar contraseñas (BCrypt). OBLIGATORIO para RNF01.
     */

}