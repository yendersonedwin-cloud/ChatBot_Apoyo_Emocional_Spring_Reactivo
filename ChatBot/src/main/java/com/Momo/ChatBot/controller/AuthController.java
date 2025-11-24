package com.Momo.ChatBot.controller;

import com.Momo.ChatBot.security.JwtUtil;
import com.Momo.ChatBot.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;

// DTOs (pueden estar en otro archivo o aquí)
record RegisterRequest(String nombre, String email, String password) {}
record LoginRequest(String email, String password) {}
record AuthResponse(String token) {}

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UsuarioService usuarioService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder; // Necesario para validar la contraseña

    public AuthController(UsuarioService usuarioService, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.usuarioService = usuarioService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    // RF01: Endpoint para el registro de nuevos usuarios
    @PostMapping("/register")
    public Mono<ResponseEntity<String>> register(@RequestBody RegisterRequest request) {
        // 1. Verificar si el usuario ya existe (RNF01)
        return usuarioService.findByEmail(request.email())
                .flatMap(existingUser -> Mono.just(ResponseEntity.badRequest().body("El email ya está registrado.")))
                // 2. Si no existe, registrarlo (UsuarioService ya hashea la contraseña)
                .switchIfEmpty(usuarioService.registrarUsuario(request.nombre(), request.email(), request.password())
                        .thenReturn(ResponseEntity.ok("Registro exitoso. Ahora puede iniciar sesión.")));
    }

    // RNF06: Endpoint para el inicio de sesión
    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody LoginRequest request) {

        // El Mono de error debe coincidir con el tipo de retorno: Mono<ResponseEntity<AuthResponse>>
        Mono<ResponseEntity<AuthResponse>> unauthorized = Mono.just(
                // El <AuthResponse> en el build fuerza el tipo genérico del ResponseEntity vacío
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).<AuthResponse>build()
        );

        // 1. Buscar el usuario por email
        return usuarioService.findByEmail(request.email())
                .flatMap(user -> {
                    // 2. Si existe, validar la contraseña encriptada
                    if (passwordEncoder.matches(request.password(), user.getPasswordHash())) {

                        // 3. Contraseña válida: Generar Token JWT (RNF06)
                        LocalDateTime expiration = LocalDateTime.now().plusHours(2);
                        String token = jwtUtil.generateToken(user.getEmail(), expiration);

                        return Mono.just(ResponseEntity.ok(new AuthResponse(token)));
                    } else {
                        // 4. Contraseña inválida: Usamos el Mono de error definido
                        return unauthorized;
                    }
                })
                // 5. Usuario no encontrado: Usamos el Mono de error en caso de vacío
                .switchIfEmpty(unauthorized);
    }
}
