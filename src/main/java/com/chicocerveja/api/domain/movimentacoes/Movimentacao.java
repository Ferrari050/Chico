package com.chicocerveja.api.domain.movimentacoes;

import com.chicocerveja.api.domain.enums.OrigemMovimentacao;
import com.chicocerveja.api.domain.enums.TipoMovimentacao;
import com.chicocerveja.api.domain.estoque.Estoque;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "movimentacoes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Movimentacao {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimentacao tipoMovimentacao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigemMovimentacao orgiemMovimentacao;

    private Double quantidade_movimentacao;
    private Date data_movimentacao;

    @ManyToOne
    @JoinColumn(name = "estoque_id")
    private Estoque estoque;
}
