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
@AllArgsConstructor
public class Usuarios {
    @Id
    private long id;
    private String nombre;
    private String email;
    private String passwordHash; // Almacena el hash de la contraseña, NUNCA el texto plano
    private LocalDateTime fechaRegistro = LocalDateTime.now();
}
