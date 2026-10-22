package com.chicocerveja.api.domain.producao;

import com.chicocerveja.api.domain.enums.StatusProducao;
import org.antlr.v4.runtime.misc.NotNull;

import java.util.Date;

public record ProducaoRequestDTO(@NotNull String nome_cerveja, @NotNull Date data_producao, @NotNull Double quantidade_producao) {
}
