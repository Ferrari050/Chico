package com.chicocerveja.api.controller;


import com.chicocerveja.api.domain.producaoIngrediente.AtualizarQuantidadeIngredienteDTO;
import com.chicocerveja.api.domain.producaoIngrediente.ProducaoIngrediente;
import com.chicocerveja.api.domain.producaoIngrediente.ProducaoIngredienteDTO;
import com.chicocerveja.api.service.ProducaoingredienteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/producao/{producaoId}/ingredientes")
public class ProducaoIngredienteController {
    private final ProducaoingredienteService producaoingredienteService;

    public ProducaoIngredienteController(ProducaoingredienteService producaoingredienteService) {
        this.producaoingredienteService = producaoingredienteService;
    }

    @PostMapping
    public ResponseEntity<ProducaoIngrediente> adicionar(@PathVariable UUID producaoId, @RequestBody ProducaoIngredienteDTO producaoIngredienteDTO) {
        return ResponseEntity.ok(producaoingredienteService.adicionar(producaoId, producaoIngredienteDTO));
    }

    @GetMapping
    public ResponseEntity<List<ProducaoIngrediente>> listar(@PathVariable UUID producaoId) {
        return ResponseEntity.ok(producaoingredienteService.listar(producaoId));
    }

    @PutMapping("/{ingredienteId}")
    public ResponseEntity<ProducaoIngrediente> atualizarQuantidade(@PathVariable UUID ingredienteId, @RequestBody AtualizarQuantidadeIngredienteDTO ingredienteDTO){
        return ResponseEntity.ok(producaoingredienteService.atualizarQuantidade(ingredienteId, ingredienteDTO));
    }

    @DeleteMapping("/{ingredienteId}")
    public ResponseEntity<Void> remover(@PathVariable UUID ingredienteId){
        producaoingredienteService.removerIngrediente(ingredienteId);
        return ResponseEntity.noContent().build();
    }
}
