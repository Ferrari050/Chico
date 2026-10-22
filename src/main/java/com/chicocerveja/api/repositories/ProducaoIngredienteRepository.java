package com.chicocerveja.api.repositories;

import com.chicocerveja.api.domain.producaoIngrediente.ProducaoIngrediente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProducaoIngredienteRepository extends JpaRepository<ProducaoIngrediente, UUID> {
    List<ProducaoIngrediente>findByProducaoId(UUID producaoId);
    Optional<ProducaoIngrediente> findByProducaoIdAndEstoqueId(UUID producaoId, UUID estoqueId);

    List<ProducaoIngrediente> findByEstoqueId(UUID id);
}
