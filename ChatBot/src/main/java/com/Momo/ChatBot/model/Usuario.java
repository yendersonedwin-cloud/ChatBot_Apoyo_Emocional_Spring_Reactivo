package com.Momo.ChatBot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column; // Nueva importación

import java.time.LocalDateTime;

// 1. CORRECCIÓN: Usar el nombre de la tabla de PostgreSQL: 'users'
@Table("users")
@Data // Incluye @Getter, @Setter, @ToString
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    private Long id;

    // 2. CORRECCIÓN: Mapear al campo 'username' de la tabla 'users'
    @Column("username") // Mapeo explícito
    private String username;

    private String email;

    // 3. CORRECCIÓN: Mapear al campo 'password_hash' de la tabla 'users'
    @Column("password_hash") // Mapeo explícito
    private String passwordHash;

    // Spring Data R2DBC puede mapear 'fechaRegistro' a 'created_at' automáticamente
    // si usas el @Column o si configuras un conversor, pero lo dejaremos simple:
    @Column("created_at")
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    /**
     * Constructor para usar en el servicio (Registro)
     */
    public Usuario(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.fechaRegistro = LocalDateTime.now();
    }
}
