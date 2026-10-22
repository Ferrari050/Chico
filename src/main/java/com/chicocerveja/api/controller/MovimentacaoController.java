package com.chicocerveja.api.controller;

import com.chicocerveja.api.domain.movimentacoes.Movimentacao;
import com.chicocerveja.api.service.MovimentacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/movimentacoes")
public class MovimentacaoController {

    private final MovimentacaoService movimentacaoService;

    public MovimentacaoController(MovimentacaoService movimentacaoService) {
        this.movimentacaoService = movimentacaoService;
    }

    @GetMapping
    public ResponseEntity<List<Movimentacao>> listarTodas() {
        return ResponseEntity.ok(movimentacaoService.listarTodas());
    }
}