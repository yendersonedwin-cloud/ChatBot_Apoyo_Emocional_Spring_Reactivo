package com.Momo.ChatBot.model;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Data Transfer Object (DTO) para recibir el mensaje del usuario y el
 * ID de la conversación actual desde el frontend.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    // El texto que el usuario escribe en el chat
    private String userMessage;

    // El ID de la conversación actual. Se usa para vincular los mensajes nuevos
    // al historial existente. Si es nulo, significa que es una nueva conversación.
    private Long conversationId;
}