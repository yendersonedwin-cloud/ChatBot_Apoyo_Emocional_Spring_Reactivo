package com.Momo.ChatBot.controller;

import com.Momo.ChatBot.model.Interaccion;
import com.Momo.ChatBot.model.Usuario;
import com.Momo.ChatBot.service.GeminiChatService;
import com.Momo.ChatBot.service.UsuarioService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

record ChatRequest(String message) {}
record ChatResponse(String response) {}

@RestController
@RequestMapping("/api/chat")
public class chatController {

    private final GeminiChatService chatService;
    private final UsuarioService usuarioService;

    public chatController(GeminiChatService chatService, UsuarioService usuarioService) {
        this.chatService = chatService;
        this.usuarioService = usuarioService;
    }

    private Mono<Long> getAuthenticatedUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> securityContext.getAuthentication())
                .map(Authentication::getName)
                .flatMap(usuarioService::findByEmail)
                .map(Usuario::getId)
                .switchIfEmpty(Mono.error(new IllegalStateException("Usuario no autenticado o no encontrado.")));
    }


    @PostMapping("/message")
    public Mono<ChatResponse> sendMessage(@RequestBody ChatRequest request) {
        return getAuthenticatedUserId()
                .flatMap(userId ->
                        chatService.generarRespuesta(userId, request.message())
                )
                .map(respuestaTexto -> new ChatResponse(respuestaTexto));
    }

    @GetMapping("/history")
    public Flux<Interaccion> getChatHistory() {
        return getAuthenticatedUserId()
                .flatMapMany(chatService::getHistorial);
    }
}