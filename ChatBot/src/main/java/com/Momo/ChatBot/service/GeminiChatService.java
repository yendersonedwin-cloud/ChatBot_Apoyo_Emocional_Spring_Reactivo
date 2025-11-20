package com.Momo.ChatBot.service;
import com.Momo.ChatBot.model.Interacciones;
import com.Momo.ChatBot.repository.InteraccionesRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class GeminiChatService {
    // Se inyecta la clave API desde application.properties (gemini.api.key)
    @Value("${gemini.api.key}")
    private String apiKey;

    private final WebClient webClient;
    private final InteraccionesRepository interaccionesRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // -------------------------------------------------------------------------
    // T-2: Instrucciones del Prompt (El núcleo de la ética RNF10 y empatía RF04)
    // -------------------------------------------------------------------------

    private final String PROMPT_BASE_INSTRUCCIONES =
            """
            Eres un Asistente de Apoyo Emocional Universitario, un recurso de primera línea para estudiantes. 
            Tu objetivo es escuchar, validar sentimientos y responder con empatía, manteniendo siempre un tono cálido y no intrusivo (RF04).
    
            **REGLAS CRÍTICAS DE SEGURIDAD Y ÉTICA (RNF10 y RF06):**
            1. **NO DIAGNOSTICAR:** Nunca debes dar juicios de valor, diagnósticos clínicos o consejos terapéuticos no autorizados. Si el usuario pide un diagnóstico, redirige a escuchar.
    
    
            Analiza el mensaje del usuario y responde. Si el usuario pregunta por manejo emocional, puedes sugerir ejercicios simples (RF05, RF11).
    
            Mensaje del usuario: 
            """;


    // Constructor para Inyección de Dependencias (WebClient y Repositorio)
    public GeminiChatService(WebClient.Builder webClientBuilder,
                             InteraccionesRepository interaccionesRepository) {

        // T-1: Configuración de la URL base para la llamada a la API
        this.webClient = webClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey)
                .build();
        this.interaccionesRepository = interaccionesRepository;
    }


    // -------------------------------------------------------------------------
    // Método Principal: Maneja la conversación completa
    // -------------------------------------------------------------------------

    public String getChatResponse(String mensajeUsuario) {
        String promptCompleto = PROMPT_BASE_INSTRUCCIONES + mensajeUsuario;
        String respuestaIA;

        try {
            // T-1: Llamada a la API
            respuestaIA = callGeminiApi(promptCompleto);
        } catch (Exception e) {
            // Manejo de errores de conexión o API
            respuestaIA = "Lo siento, tengo problemas técnicos en este momento. Por favor, inténtalo de nuevo más tarde.";
            saveInteraction(mensajeUsuario, respuestaIA, "Error API"); // RF07
            return respuestaIA;
        }

        // T-3: Detección y manejo de Crisis (RF06)
        if (respuestaIA.contains("RIESGO_CRISIS")) {
            // Se activa la alerta y la información de contacto (RF06)
            String alerta = "ALERTA CRÍTICA: Hemos detectado una situación de riesgo. Por favor, contacta inmediatamente a los Servicios Psicológicos Universitarios: [Número de teléfono] o [Link de ayuda].";

            saveInteraction(mensajeUsuario, alerta, "Riesgo de Crisis"); // RF07

            return alerta;
        }

        // Asumiendo que la IA puede devolver la emoción dentro de un formato estructurado o se parsea después
        // Por ahora, usamos un marcador de posición.
        saveInteraction(mensajeUsuario, respuestaIA, "Emoción por determinar"); // RF07

        return respuestaIA;
    }

    // -------------------------------------------------------------------------
    // Métodos Auxiliares
    // -------------------------------------------------------------------------

    // Auxiliar para la T-1: Realiza la llamada HTTP a la API de Gemini
    private String callGeminiApi(String prompt) {
        // Estructura del JSON que la API de Gemini espera
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
        );

        // Envía la solicitud y espera la respuesta (Nota: .block() se usa por simplicidad,
        // pero en un entorno reactivo puro se usaría Mono/Flux)
        String jsonResponse = webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // Parsea la respuesta JSON para extraer el texto de la IA
        return parseGeminiResponse(jsonResponse);
    }

    // Auxiliar para el RF07: Guarda la interacción en la base de datos
    private void saveInteraction(String mensajeUsuario, String respuestaChatbot, String emocionDetectada) {
        // El ID será generado automáticamente por la DB
        Interacciones interaccion = new Interacciones(
                mensajeUsuario,
                respuestaChatbot,
                emocionDetectada
                // Se asume que la fecha/hora se inicializa en el constructor de Interacciones.java
        );
        interaccionesRepository.save(interaccion);
    }

    // Auxiliar para extraer la respuesta de texto de la estructura JSON de Gemini
    private String parseGeminiResponse(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            // La respuesta de la IA está anidada profundamente en la estructura JSON
            return root
                    .path("candidates").path(0)
                    .path("content").path("parts").path(0)
                    .path("text").asText();
        } catch (Exception e) {
            // En caso de fallo de parseo (ej. respuesta vacía o formato inesperado)
            return "Lo siento, la respuesta de la IA no pudo procesarse.";
        }
    }
}
