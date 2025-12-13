package com.Momo.ChatBot.repository;

import com.Momo.ChatBot.model.Interaccion;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface InteraccionRepository extends ReactiveCrudRepository<Interaccion, Long> {

    // **CORRECCIÓN CLAVE: Consulta de historial por userId y Timestamp**
    Flux<Interaccion> findByUserIdOrderByTimestampDesc(Long userId);
}
