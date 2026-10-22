package com.chicocerveja.api.controller;

import com.chicocerveja.api.domain.estoque.Estoque;
import com.chicocerveja.api.domain.quantidade.QuantidadeDTO;
import com.chicocerveja.api.service.EstoqueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/estoque")
public class QuantidadeController {
    private final EstoqueService estoqueService;

    public QuantidadeController(EstoqueService estoqueService) {
        this.estoqueService = estoqueService;
    }

    @PostMapping("/{id}/entrada")
    public ResponseEntity<Estoque> adicionarQuantidade(@PathVariable UUID id, @RequestBody QuantidadeDTO quantidadeDTO) {
        return ResponseEntity.ok(
                estoqueService.adicionarQuantidade(id, quantidadeDTO.quantidade(), quantidadeDTO.origem())
        );
    }

    @PostMapping("/{id}/saida")
    public ResponseEntity<Estoque> retirarQuantidade(@PathVariable UUID id, @RequestBody QuantidadeDTO quantidadeDTO) {
        return ResponseEntity.ok(
                estoqueService.retirarQuantidade(id, quantidadeDTO.quantidade(), quantidadeDTO.origem())
        );
    }
}
