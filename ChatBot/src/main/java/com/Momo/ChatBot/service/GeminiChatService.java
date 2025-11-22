package com.Momo.ChatBot.service;

import com.Momo.ChatBot.model.Interaccion; // Usamos la entidad singular
import com.Momo.ChatBot.repository.InteraccionRepository; // Usamos el repositorio singular
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class GeminiChatService {

    // Se declara pero no se inyecta con @Value aquí
    private final String PROMPT_BASE_INSTRUCCIONES;
    private final WebClient webClient;
    private final InteraccionRepository interaccionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // -------------------------------------------------------------------------
    // Constructor e Inyección de Dependencias
    // -------------------------------------------------------------------------

    public GeminiChatService(WebClient.Builder webClientBuilder,
                             InteraccionRepository interaccionRepository,
                             @Value("${gemini.api.key}") String apiKey) {

        // 1. Configuración de la URL base para la llamada a la API
        this.webClient = webClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey)
                .build();
        this.interaccionRepository = interaccionRepository;

        // 2. Definición del Prompt (ahora dentro del constructor o como constante)
        this.PROMPT_BASE_INSTRUCCIONES = """
            Eres un Asistente de Apoyo Emocional Universitario, un recurso de primera línea para estudiantes. 
            Tu objetivo es escuchar, validar sentimientos y responder con empatía, manteniendo siempre un tono cálido y no intrusivo (RF04).
            
            **REGLAS CRÍTICAS DE SEGURIDAD Y ÉTICA (RNF10 y RF06):**
            1. **NO DIAGNOSTICAR:** Nunca debes dar juicios de valor, diagnósticos clínicos o consejos terapéuticos no autorizados. Si el usuario pide un diagnóstico, redirige a escuchar.
            
            
            Analiza el mensaje del usuario y responde. Si el usuario pregunta por manejo emocional, puedes sugerir ejercicios simples (RF05, RF11).
            
            Mensaje del usuario: 
            """;
    }

    // -------------------------------------------------------------------------
    // MétodoPrincipal Reactivo: Llama a la IA y Gestiona el Flujo
    // -------------------------------------------------------------------------

    public Mono<String> generarRespuesta(String mensajeUsuario) {
        String promptCompleto = PROMPT_BASE_INSTRUCCIONES + mensajeUsuario;
        String requestBody = buildRequestBody(promptCompleto);

        // Flujo reactivo de llamada a la API, parseo, guardado y respuesta
        return webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class) // Obtiene el JSON de respuesta como Mono<String>
                .map(this::parseGeminiResponse) // Parsea el JSON a String
                .flatMap(respuestaIA ->
                        processSaveAndRespond(mensajeUsuario, respuestaIA) // Lógica de Riesgo y Guardado
                )
                // Manejo de errores de conexión/API
                .onErrorResume(e -> {
                    String errorMsg = "Lo siento, tengo problemas técnicos en este momento. Por favor, inténtalo de nuevo más tarde.";
                    System.err.println("Error en la llamada a Gemini: " + e.getMessage());
                    // Guarda el error en la DB y luego devuelve el mensaje de error.
                    return saveInteractionReactivo(mensajeUsuario, errorMsg, "Error API").thenReturn(errorMsg);
                });
    }

    // -------------------------------------------------------------------------
    // Métodos Auxiliares
    // -------------------------------------------------------------------------

    // Auxiliar 1: Construye el JSON Request Body para la API de Gemini
    private String buildRequestBody(String prompt) {
        // Usa String.format con el prompt.
        return String.format("""
            {
              "contents": [
                {
                  "parts": [
                    {
                      "text": "%s"
                    }
                  ]
                }
              ]
            }
            """, prompt.replace("\"", "\\\""));
    }

    // Auxiliar 2: Lógica de Riesgo y Guardado (Reactivo)
    private Mono<String> processSaveAndRespond(String mensajeUsuario, String respuestaIA) {

        // Detección de Crisis (RF06)
        if (respuestaIA.contains("RIESGO_CRISIS")) {
            String alerta = "ALERTA CRÍTICA: Hemos detectado una situación de riesgo. Por favor, contacta inmediatamente a los Servicios Psicológicos Universitarios: [Número de teléfono] o [Link de ayuda].";

            // Guardar la alerta de forma reactiva (R2DBC)
            return saveInteractionReactivo(mensajeUsuario, alerta, "Riesgo de Crisis")
                    .thenReturn(alerta); // Devuelve el String de alerta
        }

        // Si no hay crisis: Guardar la interacción normal de forma reactiva
        return saveInteractionReactivo(mensajeUsuario, respuestaIA, "Emoción por determinar")
                .thenReturn(respuestaIA); // Devuelve la respuesta de la IA
    }

    // Auxiliar 3: Guardado R2DBC Reactivo
    private Mono<Interaccion> saveInteractionReactivo(String mensajeUsuario, String respuestaChatbot, String emocionDetectada) {
        Interaccion interaccion = new Interaccion(
                mensajeUsuario,
                respuestaChatbot,
                emocionDetectada
        );
        // R2DBC: Devuelve Mono<Interaccion>
        return interaccionRepository.save(interaccion);
    }

    // Auxiliar 4: Extrae la respuesta de texto de la estructura JSON de Gemini
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
            return "RIESGO_CRISIS: Error de formato en la respuesta de la IA. Mensaje de emergencia activado por fallo de sistema.";
        }
    }
}