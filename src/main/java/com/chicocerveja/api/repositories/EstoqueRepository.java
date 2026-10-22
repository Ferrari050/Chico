package com.chicocerveja.api.repositories;

import com.chicocerveja.api.domain.enums.CategoriaEstoque;
import com.chicocerveja.api.domain.estoque.Estoque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EstoqueRepository extends JpaRepository<Estoque, UUID> {
    Optional<Estoque> findByNomeAndCategoriaAndMarcaIgnoreCase(String nome, CategoriaEstoque categoria, String marca);
    List<Estoque> findByQuantidadeGreaterThan(Double quantidade);
    List<Estoque> findByCategoria(CategoriaEstoque categoria);
}
