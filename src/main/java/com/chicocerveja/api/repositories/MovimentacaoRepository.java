package com.chicocerveja.api.repositories;

import com.chicocerveja.api.domain.movimentacoes.Movimentacao;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MovimentacaoRepository extends JpaRepository<Movimentacao, UUID> {
    @Transactional
    void deleteByEstoqueId(UUID estoqueId);
}
