package com.Momo.ChatBot.service;

import com.Momo.ChatBot.model.Usuario;
import com.Momo.ChatBot.repository.UsuarioRepository; // El repositorio R2DBC (Asegúrate de que exista)
import org.springframework.security.crypto.password.PasswordEncoder; // Para encriptar contraseñas
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder; // Inyectamos el componente de hasheo

    // Inyección de dependencias por constructor
    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Busca un usuario por email. Utilizado principalmente por el AuthController (login y registro).
     * @param email El correo electrónico del usuario.
     * @return Mono<Usuario> que emite el usuario si existe, o un Mono vacío.
     */
    public Mono<Usuario> findByEmail(String email) {
        return usuarioRepository.findByEmail(email);
    }

    /**
     * Registra un nuevo usuario en la base de datos, hasheando la contraseña.
     * @param nombre Nombre del usuario.
     * @param email Correo electrónico.
     * @param password Contraseña en texto plano (se hashea internamente).
     * @return Mono<Usuario> que emite el usuario recién creado.
     */
    public Mono<Usuario> registrarUsuario(String nombre, String email, String password) {
        // 1. Hashear la contraseña antes de guardarla
        String hashedPassword = passwordEncoder.encode(password);

        // 2. Crear la nueva instancia de Usuario con la contraseña hasheada
        Usuario nuevoUsuario = new Usuario(nombre, email, hashedPassword);

        // 3. Guardar en la base de datos de forma reactiva
        return usuarioRepository.save(nuevoUsuario);
    }
}
