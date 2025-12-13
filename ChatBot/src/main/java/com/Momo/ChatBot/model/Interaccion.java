package com.Momo.ChatBot.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column; // Importar Column
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Table("interacciones")
@Data
public class Interaccion {

    @Id
    private Long id;

    // CORRECCIÓN: Usamos la anotación @Column para asegurar el mapeo a 'user_id'
    @Column("user_id")
    private Long userId;

    private LocalDateTime timestamp = LocalDateTime.now();
    private String mensajeUsuario;
    private String respuestaChatbot;
    private String emocionDetectada;

    public Interaccion() {}

    /**
     * Constructor usado para guardar la interacción.
     * La columna 'timestamp' se inicializa con LocalDateTime.now() y también
     * puede ser gestionada por el valor DEFAULT en la BD (como en la consulta SQL).
     */
    public Interaccion(Long userId, String mensajeUsuario, String respuestaChatbot, String emocionDetectada) {
        this.userId = userId;
        this.mensajeUsuario = mensajeUsuario;
        this.respuestaChatbot = respuestaChatbot;
        this.emocionDetectada = emocionDetectada;
    }

    // Nota: El setter para 'timestamp' no es necesario si la BD lo maneja,
    // pero se mantiene el valor por defecto en Java por seguridad.
}