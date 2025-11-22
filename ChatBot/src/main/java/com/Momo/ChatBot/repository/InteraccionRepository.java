package com.Momo.ChatBot.repository;

import com.Momo.ChatBot.model.Interaccion;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InteraccionRepository extends ReactiveCrudRepository<Interaccion, Long>{
}
