package com.chicocerveja.api.service;

import com.chicocerveja.api.domain.enums.OrigemMovimentacao;
import com.chicocerveja.api.domain.enums.StatusProducao;
import com.chicocerveja.api.domain.enums.TipoMovimentacao;
import com.chicocerveja.api.domain.estoque.Estoque;
import com.chicocerveja.api.domain.producao.Producao;
import com.chicocerveja.api.domain.producao.ProducaoRequestDTO;
import com.chicocerveja.api.domain.producaoIngrediente.ProducaoIngrediente;
import com.chicocerveja.api.repositories.EstoqueRepository;
import com.chicocerveja.api.repositories.ProducaoIngredienteRepository;
import com.chicocerveja.api.repositories.ProducaoRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class ProducaoService {
    private final ProducaoRepository producaoRepository;
    private final EstoqueRepository estoqueRepository;
    private final ProducaoIngredienteRepository producaoIngredienteRepository;
    private final MovimentacaoService movimentacaoService;

    public ProducaoService(ProducaoRepository producaoRepository, EstoqueRepository estoqueRepository, ProducaoIngredienteRepository producaoIngredienteRepository, MovimentacaoService movimentacaoService) {
        this.producaoRepository = producaoRepository;
        this.estoqueRepository = estoqueRepository;
        this.producaoIngredienteRepository = producaoIngredienteRepository;
        this.movimentacaoService = movimentacaoService;
    }

    /* Cadastra uma nova receita de produção */
    public Producao cadastroProducao(ProducaoRequestDTO producaoRequest){

        if (producaoRequest.nome_cerveja() == null || producaoRequest.nome_cerveja().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Nome da cerveja é obrigatório."
            );
        }

        Producao producao = new Producao();

        producao.setNome_cerveja(producaoRequest.nome_cerveja());
        producao.setData_producao(producaoRequest.data_producao());
        producao.setQuantidade_producao(producaoRequest.quantidade_producao());
        producao.setStatus_producao(StatusProducao.PENDENTE);

        return producaoRepository.save(producao);

    }

    /* Busca todos os itens no estoque */
    public List<Producao> listaTodaProducao(){
        return producaoRepository.findAll();
    }

    /* Busca por uma receita por id */
    public Producao buscaProducaoPorId(UUID id){
        return producaoRepository.findById(id).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND,"Receita não encontrada"));
    }

    /* Atualiza uma receita pelo id */
    public Producao atualizarProducao(Producao producao, UUID id){
        Producao producaoAtulizado = producaoRepository.findById(id).orElseThrow(() -> new RuntimeException("Receita não encontrada"));

        producaoAtulizado.setNome_cerveja(producao.getNome_cerveja());
        producaoAtulizado.setData_producao(producao.getData_producao());
        producaoAtulizado.setStatus_producao(producao.getStatus_producao());

        return producaoRepository.save(producaoAtulizado);
    }

    /* Atualiza o status da receita pelo id */
    public Producao atualizarStatus(UUID id, StatusProducao status){

        Producao producao = producaoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Produção não encontrada"));

        producao.setStatus_producao(status);

        return producaoRepository.save(producao);
    }

    /* Deleta uma receita pelo id */
    @Transactional
    public void deletaProducao(UUID id) {
        Producao producao = producaoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produção não encontrada"));

        List<ProducaoIngrediente> ingredientes = producaoIngredienteRepository.findByProducaoId(id);

        // Se a produção AINDA ESTAVA PENDENTE, estorna os ingredientes de volta para o estoque
        if (producao.getStatus_producao() == StatusProducao.PENDENTE) {
            for (ProducaoIngrediente ingrediente : ingredientes) {
                Estoque estoque = ingrediente.getEstoque();

                if (estoque != null) {
                    // Devolve a quantidade utilizada de volta ao estoque
                    Double quantidadeEstornada = ingrediente.getQuantidade_utilizada();
                    estoque.setQuantidade(estoque.getQuantidade() + quantidadeEstornada);
                    estoqueRepository.save(estoque);

                    // Registra a entrada de estorno na tabela de movimentações
                    movimentacaoService.registrarMovimentacao(
                            estoque,
                            TipoMovimentacao.ENTRADA,
                            OrigemMovimentacao.AJUSTE_MANUAL,
                            quantidadeEstornada,
                            new Date()
                    );
                }
            }
        }

        // 1. Remove os registros de vínculos da tabela producao_ingredientes
        if (ingredientes != null && !ingredientes.isEmpty()) {
            producaoIngredienteRepository.deleteAll(ingredientes);
        }

        // 2. Apaga a produção sem dar erro de Foreign Key Constraint
        producaoRepository.delete(producao);
    }

    @Transactional
    public Producao iniciarProducao(UUID producaoId) {
        Producao producao = producaoRepository.findById(producaoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produção não encontrada"));

        // Se já estiver em MOSTURACAO ou posterior, apenas retorna a produção atual sem erro
        if (producao.getStatus_producao() != StatusProducao.PENDENTE) {
            return producao;
        }

        List<ProducaoIngrediente> ingredientes = producaoIngredienteRepository.findByProducaoId(producaoId);

        if (ingredientes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A produção não possui ingredientes cadastrados.");
        }

        // 1. Valida saldo em estoque de todos os ingredientes
        for (ProducaoIngrediente ingrediente : ingredientes) {
            Estoque estoque = ingrediente.getEstoque();
            if (ingrediente.getQuantidade_utilizada() > estoque.getQuantidade()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Estoque insuficiente para " + estoque.getNome() + ". Necessário: " + ingrediente.getQuantidade_utilizada() + "kg, Disponível: " + estoque.getQuantidade() + "kg"
                );
            }
        }

        // 2. Realiza a baixa no estoque e registra a movimentação
        for (ProducaoIngrediente ingrediente : ingredientes) {
            Estoque estoque = ingrediente.getEstoque();
            Double novaQuantidade = estoque.getQuantidade() - ingrediente.getQuantidade_utilizada();

            estoque.setQuantidade(novaQuantidade);
            estoqueRepository.save(estoque);

            movimentacaoService.registrarMovimentacao(
                    estoque,
                    TipoMovimentacao.SAIDA,
                    OrigemMovimentacao.PRODUCAO,
                    ingrediente.getQuantidade_utilizada(),
                    new Date()
            );
        }

        // 3. Atualiza o status para MOSTURACAO
        producao.setStatus_producao(StatusProducao.MOSTURACAO);
        return producaoRepository.save(producao);
    }
}
