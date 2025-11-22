package com.Momo.ChatBot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("usuarios") // Define el nombre de la tabla
@NoArgsConstructor
@Data
@AllArgsConstructor // Este constructor es para Spring (incluye el ID)
public class Usuario {
    @Id
    private Long id; // Usar Long en vez de long
    private String nombre;
    private String email;
    private String passwordHash; // Almacena el hash de la contraseña, NUNCA el texto plano
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    /**
     * Constructor para crear un nuevo usuario desde el servicio (sin ID ni fechaRegistro)
     */
    public Usuario(String nombre, String email, String passwordHash) {
        this.nombre = nombre;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fechaRegistro = LocalDateTime.now();
    }
}
