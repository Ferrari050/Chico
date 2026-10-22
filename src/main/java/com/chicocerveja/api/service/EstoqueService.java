package com.chicocerveja.api.service;

import com.chicocerveja.api.domain.enums.CategoriaEstoque;
import com.chicocerveja.api.domain.enums.OrigemMovimentacao;
import com.chicocerveja.api.domain.enums.TipoMovimentacao;
import com.chicocerveja.api.domain.estoque.Estoque;
import com.chicocerveja.api.domain.estoque.EstoqueRequestDTO;
import com.chicocerveja.api.domain.producaoIngrediente.ProducaoIngrediente;
import com.chicocerveja.api.repositories.EstoqueRepository;
import com.chicocerveja.api.repositories.MovimentacaoRepository;
import com.chicocerveja.api.repositories.ProducaoIngredienteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class EstoqueService {

    private final EstoqueRepository estoqueRepository;
    private final MovimentacaoService movimentacaoService;
    private final MovimentacaoRepository movimentacaoRepository;
    private final ProducaoIngredienteRepository producaoIngredienteRepository;

    public EstoqueService(EstoqueRepository estoqueRepository,MovimentacaoService movimentacaoService,MovimentacaoRepository movimentacaoRepository,ProducaoIngredienteRepository producaoIngredienteRepository) {
        this.estoqueRepository = estoqueRepository;
        this.movimentacaoService = movimentacaoService;
        this.movimentacaoRepository = movimentacaoRepository;
        this.producaoIngredienteRepository = producaoIngredienteRepository;
    }

    /* Cadastra os valores do item no estoque */
    public Estoque cadastroEstoque(EstoqueRequestDTO estoqueRequest) {
        Optional<Estoque> estoqueExistente = estoqueRepository.findByNomeAndCategoriaAndMarcaIgnoreCase(estoqueRequest.nome(), estoqueRequest.categoria(), estoqueRequest.marca());

        Estoque estoqueSalvo;

        if(estoqueExistente.isPresent()) {
            Estoque estoqueAcumulado = estoqueExistente.get();

            estoqueAcumulado.setQuantidade(estoqueAcumulado.getQuantidade() + estoqueRequest.quantidade());

            estoqueSalvo = estoqueRepository.save(estoqueAcumulado);
        } else {

            Estoque estoque = new Estoque();

            estoque.setNome(estoqueRequest.nome());
            estoque.setCategoria(estoqueRequest.categoria());
            estoque.setMarca(estoqueRequest.marca());
            estoque.setQuantidade(estoqueRequest.quantidade());
            estoque.setData_cadastro(estoqueRequest.data_cadastro());

            estoqueSalvo = estoqueRepository.save(estoque);
        }

        movimentacaoService.registrarMovimentacao(
                estoqueSalvo,
                TipoMovimentacao.ENTRADA,
                OrigemMovimentacao.CADASTRO,
                estoqueRequest.quantidade(),
                new Date()
        );

        return estoqueSalvo;
    }

    /* Busca todos os itens no estoque */
    public List<Estoque> listaTodoEstoque() {
        return estoqueRepository.findByQuantidadeGreaterThan(0.0);
    }

    /* Busca por um item no estoque por id */
    public Estoque buscaEstoquePorId(UUID id) {
        return estoqueRepository.findById(id).orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND,"Item não encontrado"));
    }

    /* Atualiza o item no estoque pelo id */
    public Estoque atualizarEstoque(Estoque estoque, UUID id) {
        Estoque estoqueAtualizado = estoqueRepository.findById(id).orElseThrow(() -> new RuntimeException("Item não encontrado no estoque!"));

        estoqueAtualizado.setNome(estoque.getNome());
        estoqueAtualizado.setCategoria(estoque.getCategoria());
        estoqueAtualizado.setMarca(estoque.getMarca());
        estoqueAtualizado.setQuantidade(estoque.getQuantidade());
        estoqueAtualizado.setData_cadastro(estoque.getData_cadastro());

        return estoqueRepository.save(estoqueAtualizado);
    }

    /* Deleta o item no estoque pelo id */
    @org.springframework.transaction.annotation.Transactional
    public void deleteEstoque(UUID id) {
        // 1. Remove qualquer vínculo em produções que ainda utilizem este ingrediente
        List<ProducaoIngrediente> ingredientesVinculados = producaoIngredienteRepository.findByEstoqueId(id);
        if (ingredientesVinculados != null && !ingredientesVinculados.isEmpty()) {
            producaoIngredienteRepository.deleteAll(ingredientesVinculados);
        }

        // 2. Remove as movimentações atreladas a este ingrediente
        movimentacaoRepository.deleteByEstoqueId(id);

        // 3. Deleta o registro do estoque
        estoqueRepository.deleteById(id);
    }

    /* Adiciona a quantidade de um item no estoque pelo id */
    public Estoque adicionarQuantidade(UUID id, Double quantidade, OrigemMovimentacao origem) {
        Estoque estoque = estoqueRepository.findById(id).orElseThrow(() -> new RuntimeException("Item não encontrado no estoque!"));

        if (quantidade <= 0) {
            throw new RuntimeException("A quantidade deve ser maior que zero.");
        }
        estoque.setQuantidade(estoque.getQuantidade() + quantidade);

        Estoque estoqueSalvo = estoqueRepository.save(estoque);

        OrigemMovimentacao origemFinal = (origem != null) ? origem : OrigemMovimentacao.AJUSTE_MANUAL;

        movimentacaoService.registrarMovimentacao(
                estoqueSalvo,
                TipoMovimentacao.ENTRADA,
                origemFinal,
                quantidade,
                new Date()
        );

        return estoqueSalvo;
    }

    public Estoque adicionarQuantidade(UUID id, Double quantidade) {
        return adicionarQuantidade(id, quantidade, OrigemMovimentacao.AJUSTE_MANUAL);
    }

    /* Remove a quantidade de um item no estoque pelo id */
    public Estoque retirarQuantidade(UUID id, Double quantidade, OrigemMovimentacao origem) {
        Estoque estoque = estoqueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Item não encontrado no estoque!"));

        if (quantidade <= 0) {
            throw new RuntimeException("A quantidade deve ser maior que zero.");
        }

        if (quantidade > estoque.getQuantidade()) {
            throw new RuntimeException("Quantidade indisponivel no estoque.");
        }

        Double novaQuantidade = estoque.getQuantidade() - quantidade;

        // Se nenhuma origem for especificada, usa AJUSTE_MANUAL por padrão
        OrigemMovimentacao origemFinal = (origem != null) ? origem : OrigemMovimentacao.AJUSTE_MANUAL;

        movimentacaoService.registrarMovimentacao(
                estoque,
                TipoMovimentacao.SAIDA,
                origemFinal,
                quantidade,
                new Date()
        );

        estoque.setQuantidade(novaQuantidade);
        return estoqueRepository.save(estoque);
    }

    public Estoque retirarQuantidade(UUID id, Double quantidade) {
        return retirarQuantidade(id, quantidade, OrigemMovimentacao.AJUSTE_MANUAL);
    }

    public List<Estoque> listarPorCategoria(CategoriaEstoque categoria) {
        return estoqueRepository.findByCategoria(categoria);
    }
}
