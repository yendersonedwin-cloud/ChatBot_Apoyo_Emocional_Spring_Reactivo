package com.Momo.ChatBot.repository;

import com.Momo.ChatBot.model.Interacciones;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InteraccionesRepository extends ReactiveCrudRepository<Interacciones, Long>{
}
