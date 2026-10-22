package com.chicocerveja.api.repositories;

import com.chicocerveja.api.domain.producao.Producao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProducaoRepository extends JpaRepository<Producao, UUID> {
}
