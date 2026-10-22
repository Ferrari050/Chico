package com.chicocerveja.api.service;

import com.chicocerveja.api.domain.enums.OrigemMovimentacao;
import com.chicocerveja.api.domain.enums.TipoMovimentacao;
import com.chicocerveja.api.domain.estoque.Estoque;
import com.chicocerveja.api.domain.movimentacoes.Movimentacao;
import com.chicocerveja.api.repositories.MovimentacaoRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class MovimentacaoService {

    private final MovimentacaoRepository movimentacaoRepository;

    public MovimentacaoService(MovimentacaoRepository movimentacaoRepository) {this.movimentacaoRepository = movimentacaoRepository;}

    public void registrarMovimentacao(Estoque estoque, TipoMovimentacao tipoMovimentacao, OrigemMovimentacao origemMovimentacao, Double quantidade_movimentacao, Date data_movimentacao) {
        Movimentacao movimentacao = new Movimentacao();

        movimentacao.setEstoque(estoque);
        movimentacao.setTipoMovimentacao(tipoMovimentacao);
        movimentacao.setOrgiemMovimentacao(origemMovimentacao);
        movimentacao.setQuantidade_movimentacao(quantidade_movimentacao);
        movimentacao.setData_movimentacao(new Date());

        movimentacaoRepository.save(movimentacao);
    }

    public List<Movimentacao> listarTodas() {
        return movimentacaoRepository.findAll();
    }
}
