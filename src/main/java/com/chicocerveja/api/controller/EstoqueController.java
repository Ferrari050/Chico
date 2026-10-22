package com.chicocerveja.api.controller;

import com.chicocerveja.api.domain.enums.CategoriaEstoque;
import com.chicocerveja.api.domain.estoque.Estoque;
import com.chicocerveja.api.domain.estoque.EstoqueRequestDTO;
import com.chicocerveja.api.service.EstoqueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/estoque")
public class EstoqueController {

    @Autowired
    private EstoqueService estoqueService;

    @PostMapping
    public ResponseEntity<Estoque> cadastroEstoque(@RequestBody EstoqueRequestDTO estoqueRequest) {
        Estoque estoque = this.estoqueService.cadastroEstoque(estoqueRequest);
        return ResponseEntity.ok(estoque);
    }

    @GetMapping
    public ResponseEntity<List<Estoque>> listaTodoEstoque() {
        List<Estoque> estoque = this.estoqueService.listaTodoEstoque();
        return ResponseEntity.ok(estoque);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Estoque> buscaEstoquePorId(@PathVariable UUID id) {
        Estoque estoque = this.estoqueService.buscaEstoquePorId(id);
        return ResponseEntity.ok(estoque);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Estoque> atualizarEstoque(@PathVariable UUID id, @RequestBody Estoque estoque) {
        return ResponseEntity.accepted().body(estoqueService.atualizarEstoque(estoque, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEstoque(@PathVariable UUID id) {
        estoqueService.deleteEstoque(id);
        return ResponseEntity.noContent().build();
    }

    public ResponseEntity<List<Estoque>> listarPorCategoria(@PathVariable CategoriaEstoque categoria) {
        return ResponseEntity.ok(estoqueService.listarPorCategoria(categoria));
    }
}
