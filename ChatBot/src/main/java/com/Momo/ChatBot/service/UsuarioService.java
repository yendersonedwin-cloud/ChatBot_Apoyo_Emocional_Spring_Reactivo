package com.Momo.ChatBot.service;

import com.Momo.ChatBot.model.Usuario;
import com.Momo.ChatBot.repository.UsuarioRepository; // El repositorio R2DBC (Asegúrate de que exista)
import org.springframework.security.crypto.password.PasswordEncoder; // Para encriptar contraseñas
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    // Constructor: Spring inyecta el Repositorio y el PasswordEncoder (BCrypt)
    // Esto solo funciona si el @Bean de PasswordEncoder está en ChatBotApplication.java
    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * RF01: Registra un nuevo usuario, encriptando su contraseña.
     * @param nombre Nombre del usuario.
     * @param email Email (clave única).
     * @param password Contraseña plana enviada por el cliente.
     * @return Mono<Usuario> del usuario guardado de forma reactiva.
     */
    public Mono<Usuario> registrarUsuario(String nombre, String email, String password) {

        // 1. Encriptar la contraseña (Hashing) - CUMPLIMIENTO RNF01
        String passwordHash = passwordEncoder.encode(password);

        // 2. Crear el objeto Usuario con el hash
        Usuario nuevoUsuario=new Usuario(nombre,email,passwordHash);

        // 3. Guardar en la base de datos R2DBC
        return usuarioRepository.save(nuevoUsuario);
    }

    /**
     * Busca un usuario por email de forma reactiva (necesario para login y verificación de registro).
     * @param email Email a buscar.
     * @return Mono<Usuario> si se encuentra.
     */
    public Mono<Usuario> findByEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }
}
