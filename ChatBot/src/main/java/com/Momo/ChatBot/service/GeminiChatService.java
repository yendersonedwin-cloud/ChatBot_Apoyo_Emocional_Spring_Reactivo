package com.Momo.ChatBot.service;

import com.Momo.ChatBot.model.Interaccion;
import com.Momo.ChatBot.repository.InteraccionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
public class GeminiChatService {

    private final String PROMPT_BASE_INSTRUCCIONES;
    private final WebClient webClient;
    private final InteraccionRepository interaccionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructor que inyecta dependencias y configura WebClient con la API Key.
     * * @param webClientBuilder Builder para configurar el WebClient.
     * @param interaccionRepository Repositorio para guardar y obtener interacciones.
     * @param apiKey Clave de la API de Gemini inyectada desde application.properties.
     */
    public GeminiChatService(WebClient.Builder webClientBuilder,
                             InteraccionRepository interaccionRepository,
                             @Value("${gemini.api.key}") String apiKey) {

        // CORRECCIÓN CRÍTICA: Se añade la API Key como parámetro de consulta en la URL base
        // Esto resuelve el error de autenticación con la API de Google.
        String baseUrlConKey = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        this.webClient = webClientBuilder
                .baseUrl(baseUrlConKey)
                .build();
        this.interaccionRepository = interaccionRepository;

        // Definición del prompt base, incluyendo el historial
        this.PROMPT_BASE_INSTRUCCIONES = """
            Eres un Asistente de Apoyo Emocional Universitario, un recurso de primera línea para estudiantes. 
            Tu objetivo es escuchar, validar sentimientos y responder con empatía, manteniendo siempre un tono cálido y no intrusivo (RF04).
            
            *REGLAS CRÍTICAS DE SEGURIDAD Y ÉTICA (RNF10 y RF06):*
            1. *NO DIAGNOSTICAR:* Nunca debes dar juicios de valor, diagnósticos clínicos o consejos terapéuticos no autorizados. Si el usuario pide un diagnóstico, redirige a escuchar.
            
            A continuación, se te proporciona el historial de la conversación. Si no hay historial, ignora el campo.
            
            --- HISTORIAL DE CONVERSACIÓN ---
            %s 
            ---
            
            Analiza el mensaje del usuario y responde. Si el usuario pregunta por manejo emocional, puedes sugerir ejercicios simples (RF05, RF11).
            
            Mensaje del usuario: 
            """;
    }

    /**
     * Genera una respuesta del chatbot, guarda la interacción y la devuelve.
     * * @param userId ID del usuario autenticado.
     * @param mensajeUsuario Mensaje enviado por el usuario.
     * @return Mono que emite la respuesta del Chatbot.
     */
    public Mono<String> generarRespuesta(Long userId, String mensajeUsuario) {

        return getFormattedHistorial(userId)
                .defaultIfEmpty("Aún no hay mensajes en esta sesión.")
                .flatMap(historialFormateado -> {

                    String promptCompleto = String.format(PROMPT_BASE_INSTRUCCIONES, historialFormateado) + mensajeUsuario;
                    String requestBody = buildRequestBody(promptCompleto);

                    return webClient.post()
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(requestBody)
                            .retrieve()
                            .bodyToMono(String.class)
                            .map(this::parseGeminiResponse)
                            .flatMap(respuestaIA ->
                                    processSaveAndRespond(userId, mensajeUsuario, respuestaIA)
                            )
                            // Manejo de errores de la API de Gemini (HTTP 4xx/5xx)
                            .onErrorResume(e -> {
                                String errorMsg = "Lo siento, tengo problemas técnicos en este momento. Por favor, asegúrate de que tu clave Gemini sea válida y el servicio esté disponible. Inténtalo de nuevo más tarde.";
                                System.err.println("Error en la llamada a Gemini: " + e.getMessage());
                                return saveInteractionReactivo(userId, mensajeUsuario, errorMsg, "Error API").thenReturn(errorMsg);
                            });
                });
    }

    /**
     * Obtiene el historial de interacciones del usuario, ordenado por fecha de forma descendente.
     * * @param userId ID del usuario.
     * @return Flux de Interaccion (las últimas 20).
     */
    public Flux<Interaccion> getHistorial(Long userId) {
        // CORRECCIÓN DE BASE DE DATOS: Usa el método con 'Timestamp'
        return interaccionRepository.findByUserIdOrderByTimestampDesc(userId)
                .take(20);
    }

    private Mono<String> getFormattedHistorial(Long userId) {
        return getHistorial(userId)
                .collectList()
                .map(list -> list.stream()
                        .map(i -> "Usuario: " + i.getMensajeUsuario() + "\n" +
                                "Asistente: " + i.getRespuestaChatbot())
                        .collect(Collectors.joining("\n---\n")));
    }

    private String buildRequestBody(String prompt) {
        // Simple JSON para enviar contenido de texto a Gemini
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
            """, prompt.replace("\"", "\\\"").replace("\n", " ").replace("\r", ""));
    }

    private Mono<String> processSaveAndRespond(Long userId, String mensajeUsuario, String respuestaIA) {
        // Lógica para detectar crisis (si aplica)
        if (respuestaIA.contains("RIESGO_CRISIS")) {
            String alerta = "ALERTA CRÍTICA: Hemos detectado una situación de riesgo. Por favor, contacta inmediatamente a los Servicios Psicológicos Universitarios: [Número de teléfono] o [Link de ayuda].";

            return saveInteractionReactivo(userId, mensajeUsuario, alerta, "Riesgo de Crisis")
                    .thenReturn(alerta);
        }

        return saveInteractionReactivo(userId, mensajeUsuario, respuestaIA, "Emoción por determinar")
                .thenReturn(respuestaIA);
    }

    private Mono<Interaccion> saveInteractionReactivo(Long userId, String mensajeUsuario, String respuestaChatbot, String emocionDetectada) {
        Interaccion interaccion = new Interaccion(
                userId,
                mensajeUsuario,
                respuestaChatbot,
                emocionDetectada
        );
        // La entidad Interaccion debe manejar la inserción del timestamp automáticamente
        return interaccionRepository.save(interaccion);
    }

    private String parseGeminiResponse(String jsonResponse) {
        // Parsea la respuesta JSON de Gemini para extraer el texto
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            return root
                    .path("candidates").path(0)
                    .path("content").path("parts").path(0)
                    .path("text").asText();
        } catch (Exception e) {
            System.err.println("Error al parsear la respuesta de Gemini: " + e.getMessage());
            // Devuelve un mensaje de emergencia si el formato JSON es inesperado
            return "RIESGO_CRISIS: Error de formato en la respuesta de la IA. Mensaje de emergencia activado por fallo de sistema.";
        }
    }
}