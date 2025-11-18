package com.Momo.ChatBot.repository;

import com.Momo.ChatBot.model.Usuarios;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends ReactiveCrudRepository<Usuarios, Long> {
}
