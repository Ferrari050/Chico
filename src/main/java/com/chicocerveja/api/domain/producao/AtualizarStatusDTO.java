    package com.chicocerveja.api.domain.producao;

    import com.chicocerveja.api.domain.enums.StatusProducao;

    public record AtualizarStatusDTO(StatusProducao status) {
    }
