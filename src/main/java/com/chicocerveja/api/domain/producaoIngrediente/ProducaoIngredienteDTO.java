package com.chicocerveja.api.domain.producaoIngrediente;

import java.util.UUID;

public record ProducaoIngredienteDTO(UUID estoqueId, Double quantidade, Double temperatura, Integer tempo) {
}
