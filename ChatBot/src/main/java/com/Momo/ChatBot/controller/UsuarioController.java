package com.Momo.ChatBot.controller;

import com.Momo.ChatBot.service.UsuarioService;
import com.Momo.ChatBot.security.JwtUtil;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/auth") // Ruta base para autenticación
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // Inyección de dependencias
    public UsuarioController(UsuarioService usuarioService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    // -------------------------------------------------------------------------
    // DTOs para comunicación con el cliente
    // -------------------------------------------------------------------------

    // DTO para la solicitud de Registro (Request)
    @Data
    @NoArgsConstructor
    public static class RegistroRequest {
        private String nombre;
        private String email;
        private String password;
    }

    // DTO para la respuesta de autenticación (Response: incluye el token)
    @Data
    @NoArgsConstructor
    public static class AuthResponse {
        private String token;
        private String tokenType = "Bearer";
        private String email;
        private LocalDateTime expiresAt;

        // Constructor para la respuesta exitosa
        public AuthResponse(String token, String email, LocalDateTime expiresAt) {
            this.token = token;
            this.email = email;
            this.expiresAt = expiresAt;
        }

        // Constructor para el caso de error (token = "Error", email = mensaje de error, expiresAt = null)
        public AuthResponse(String token, String email) {
            this.token = token;
            this.email = email;
            this.expiresAt = null;
        }
    }

    // DTO para la solicitud de Login (Request)
    @Data
    @NoArgsConstructor
    public static class LoginRequest {
        private String email;
        private String password;
    }


    // -------------------------------------------------------------------------
    // Endpoints
    // -------------------------------------------------------------------------

    /**
     * RF01: Permite el registro de nuevos usuarios.
     */
    @PostMapping("/register")
    public Mono<ResponseEntity<AuthResponse>> registerUser(@RequestBody RegistroRequest request) {
        // 1. Verificar si el usuario ya existe
        return usuarioService.findByEmail(request.getEmail())
                .flatMap(existingUser ->
                        // Si existe, devuelve error 400. Notar que envuelve el error en un Mono
                        Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(new AuthResponse("Error", "Email ya registrado")))
                                .cast(ResponseEntity.class) // Asegura el tipo de retorno
                )
                .switchIfEmpty(
                        // Si no existe, procede con el registro (RF01)
                        usuarioService.registrarUsuario(request.getNombre(), request.getEmail(), request.getPassword())
                                .map(usuario -> {
                                    // 2. Generar el token JWT para el nuevo usuario
                                    LocalDateTime expiresAt = LocalDateTime.now().plusHours(2); // Token válido por 2 horas
                                    String token = jwtUtil.generateToken(usuario.getEmail(), expiresAt);

                                    // 3. Devolver la respuesta de autenticación
                                    AuthResponse response = new AuthResponse(token, usuario.getEmail(), expiresAt);
                                    return ResponseEntity.status(HttpStatus.CREATED).body(response);
                                })
                                .cast(ResponseEntity.class) // Asegura el tipo de retorno después del map
                )
                .cast(ResponseEntity.class) // Cast intermedio para unificar el retorno
                .map(response -> (ResponseEntity<AuthResponse>) response); // Mapeo final para asegurar el tipo de Mono
    }


    /**
     * RF02: Permite la autenticación de usuarios.
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> loginUser(@RequestBody LoginRequest request) {
        // 1. Buscar el usuario por email
        return usuarioService.findByEmail(request.getEmail())
                .flatMap(usuario -> {
                    // 2. Verificar la contraseña usando PasswordEncoder (RNF01)
                    if (passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
                        // 3. Generar el token si la contraseña es correcta
                        LocalDateTime expiresAt = LocalDateTime.now().plusHours(2);
                        String token = jwtUtil.generateToken(usuario.getEmail(), expiresAt);
                        AuthResponse response = new AuthResponse(token, usuario.getEmail(), expiresAt);
                        return Mono.just(ResponseEntity.ok(response))
                                .cast(ResponseEntity.class); // <-- Cast añadido aquí
                    } else {
                        // 4. Contraseña incorrecta
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(new AuthResponse("Error", "Credenciales inválidas")))
                                .cast(ResponseEntity.class); // <-- Cast añadido aquí
                    }
                })
                .switchIfEmpty(
                        // Usuario no encontrado
                        Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body(new AuthResponse("Error", "Credenciales inválidas")))
                                .cast(ResponseEntity.class) // <-- Cast añadido aquí
                )
                .cast(ResponseEntity.class) // Cast intermedio para unificar el retorno
                .map(response -> (ResponseEntity<AuthResponse>) response); // Mapeo final para asegurar el tipo de Mono
    }
}