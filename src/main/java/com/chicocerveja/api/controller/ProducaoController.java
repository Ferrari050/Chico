package com.chicocerveja.api.controller;

import com.chicocerveja.api.domain.producao.AtualizarStatusDTO;
import com.chicocerveja.api.domain.producao.Producao;
import com.chicocerveja.api.domain.producao.ProducaoRequestDTO;
import com.chicocerveja.api.service.ProducaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/producao")
public class ProducaoController {
    @Autowired
    private ProducaoService producaoService;

    @PostMapping
    public ResponseEntity<Producao> cadastroProducao(@Validated @RequestBody ProducaoRequestDTO producaoRequest){
        Producao producao = this.producaoService.cadastroProducao(producaoRequest);
        return ResponseEntity.ok(producao);
    }

    @GetMapping
    public ResponseEntity<List<Producao>> listaTodaProducao(){
        List<Producao> producao = this.producaoService.listaTodaProducao();
        return ResponseEntity.ok(producao);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Producao> buscaProducaoPorId(@PathVariable UUID id){
        Producao producao = this.producaoService.buscaProducaoPorId(id);
        return ResponseEntity.ok(producao);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Producao> atualizarProducao(@PathVariable UUID id, @RequestBody Producao producao){
        return ResponseEntity.accepted().body(producaoService.atualizarProducao(producao, id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Producao> atualizarStatus(@PathVariable UUID id, @RequestBody AtualizarStatusDTO dto) {
        return ResponseEntity.ok(
                producaoService.atualizarStatus(id, dto.status())
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Producao> deletarProducao(@PathVariable UUID id){
        producaoService.deletaProducao(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/iniciar")
    public ResponseEntity<Producao> iniciarProducao(@PathVariable UUID id){
        return ResponseEntity.ok(producaoService.iniciarProducao(id));
    }
}
