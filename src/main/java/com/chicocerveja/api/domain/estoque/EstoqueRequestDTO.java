package com.chicocerveja.api.domain.estoque;

import com.chicocerveja.api.domain.enums.CategoriaEstoque;

import java.util.Date;

public record EstoqueRequestDTO(String nome, CategoriaEstoque categoria, String marca, Double quantidade, Date data_cadastro) {
}
