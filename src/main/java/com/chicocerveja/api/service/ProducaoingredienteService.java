package com.chicocerveja.api.service;

import com.chicocerveja.api.domain.estoque.Estoque;
import com.chicocerveja.api.domain.producao.Producao;
import com.chicocerveja.api.domain.producaoIngrediente.AtualizarQuantidadeIngredienteDTO;
import com.chicocerveja.api.domain.producaoIngrediente.ProducaoIngrediente;
import com.chicocerveja.api.domain.producaoIngrediente.ProducaoIngredienteDTO;
import com.chicocerveja.api.repositories.EstoqueRepository;
import com.chicocerveja.api.repositories.ProducaoIngredienteRepository;
import com.chicocerveja.api.repositories.ProducaoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProducaoingredienteService {
    private final ProducaoRepository producaoRepository;
    private final EstoqueRepository estoqueRepository;
    private final ProducaoIngredienteRepository producaoIngredienteRepository;

    public ProducaoingredienteService(ProducaoRepository producaoRepository, EstoqueRepository estoqueRepository, ProducaoIngredienteRepository producaoIngredienteRepository) {
        this.producaoRepository = producaoRepository;
        this.estoqueRepository = estoqueRepository;
        this.producaoIngredienteRepository = producaoIngredienteRepository;
    }

    public ProducaoIngrediente adicionar(UUID producaoId, ProducaoIngredienteDTO producaoIngredienteDTO) {
        if(producaoIngredienteDTO.quantidade() <= 0){
            throw new RuntimeException("Quantidade inválida");
        }

        Producao producao = producaoRepository.findById(producaoId).orElseThrow(()-> new RuntimeException("Produção não encontrado"));
        Estoque estoque = estoqueRepository.findById(producaoIngredienteDTO.estoqueId()).orElseThrow(()-> new RuntimeException("Ingrediente não encontrado"));

        Optional<ProducaoIngrediente> IngredienteExistente = producaoIngredienteRepository.findByProducaoIdAndEstoqueId(producaoId, producaoIngredienteDTO.estoqueId());

        if(producaoIngredienteDTO.quantidade() > estoque.getQuantidade()) {
            throw new RuntimeException("Quantidade insuficiente no estoque Disponível: " + estoque.getQuantidade());
        }
        if(IngredienteExistente.isPresent()){
            ProducaoIngrediente ingrediente = IngredienteExistente.get();

            double novaQuantidade = ingrediente.getQuantidade_utilizada() + producaoIngredienteDTO.quantidade();

            if(novaQuantidade > estoque.getQuantidade()) {
                throw new RuntimeException("Não há estoque suficiente para '" +  estoque.getNome() + "'. Disponível: " + estoque.getQuantidade() + "kg.");
            }

            ingrediente.setQuantidade_utilizada(novaQuantidade);

            return producaoIngredienteRepository.save(ingrediente);
        }

        ProducaoIngrediente ingrediente = new ProducaoIngrediente();

        ingrediente.setProducao(producao);
        ingrediente.setEstoque(estoque);
        ingrediente.setQuantidade_utilizada(producaoIngredienteDTO.quantidade());
        ingrediente.setTemperatura(producaoIngredienteDTO.temperatura());
        ingrediente.setTempo(producaoIngredienteDTO.tempo());

        return producaoIngredienteRepository.save(ingrediente);
    }

    public List<ProducaoIngrediente> listar(UUID producaoId) {
        return producaoIngredienteRepository.findByProducaoId(producaoId);
    }

    public ProducaoIngrediente atualizarQuantidade(UUID ingredienteId, AtualizarQuantidadeIngredienteDTO atualizarQuantidadeIngredienteDTO) {
        if(atualizarQuantidadeIngredienteDTO.quantidade() <= 0){
            throw new RuntimeException("A quantidade deve ser maior que zero.");
        }

        ProducaoIngrediente ingrediente = producaoIngredienteRepository.findById(ingredienteId).orElseThrow(()->new RuntimeException("Ingrediente não encontrado"));

        Estoque estoque = ingrediente.getEstoque();

        if(atualizarQuantidadeIngredienteDTO.quantidade() > estoque.getQuantidade()) {
            throw new RuntimeException("Quantidade insuficiente no estoque. Disponível: " + estoque.getQuantidade());
        }

        ingrediente.setQuantidade_utilizada(atualizarQuantidadeIngredienteDTO.quantidade());
        return producaoIngredienteRepository.save(ingrediente);
    }

    public void removerIngrediente(UUID ingredienteId) {
        ProducaoIngrediente ingrediente = producaoIngredienteRepository.findById(ingredienteId).orElseThrow(()->new RuntimeException("Ingrediente não encontrado."));
        producaoIngredienteRepository.delete(ingrediente);
    }
}
