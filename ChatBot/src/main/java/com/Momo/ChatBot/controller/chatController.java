package com.Momo.ChatBot.controller;

import com.Momo.ChatBot.service.GeminiChatService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

record ChatRequest(String message) {}

/**
 * DTO para la respuesta de chat (Salida)
 * JSON: {"response": "..."}
 */
record ChatResponse(String response) {}


@RestController
@RequestMapping("/api/chat")
public class chatController {

    private final GeminiChatService chatService;

    public chatController(GeminiChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/message")
    public Mono<ChatResponse> sendMessage(@RequestBody ChatRequest request) {

        return chatService.generarRespuesta(request.message())
                .map(respuestaTexto -> new ChatResponse(respuestaTexto));
    }
}
