package com.chicocerveja.api.domain.quantidade;

import com.chicocerveja.api.domain.enums.OrigemMovimentacao;

public record QuantidadeDTO(Double quantidade, OrigemMovimentacao origem) {
}
