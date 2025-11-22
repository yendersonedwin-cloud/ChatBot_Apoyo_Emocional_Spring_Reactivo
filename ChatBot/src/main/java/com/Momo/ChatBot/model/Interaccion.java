package com.Momo.ChatBot.model;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;
@Table("interacciones")
@Data
@AllArgsConstructor
public class Interaccion {

        @Id
        private Long id;
        private LocalDateTime timestamp = LocalDateTime.now();
        private String mensajeUsuario;
        private String respuestaChatbot;
        private String emocionDetectada;

        // Constructor vacío (necesario) y constructor con parámetros para guardar
        public Interaccion() {}

        public Interaccion(String mensajeUsuario, String respuestaChatbot, String emocionDetectada) {
            this.mensajeUsuario = mensajeUsuario;
            this.respuestaChatbot = respuestaChatbot;
            this.emocionDetectada = emocionDetectada;
        }
    }
