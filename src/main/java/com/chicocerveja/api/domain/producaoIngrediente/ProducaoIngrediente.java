package com.chicocerveja.api.domain.producaoIngrediente;

import com.chicocerveja.api.domain.estoque.Estoque;
import com.chicocerveja.api.domain.producao.Producao;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "producao_ingredientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProducaoIngrediente {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private Double quantidade_utilizada;

    @Column(nullable = false)
    private Double temperatura;

    @Column(nullable = false)
    private Integer tempo;

    @ManyToOne
    @JoinColumn(name = "estoque_id", nullable = false)
    private Estoque estoque;

    @ManyToOne
    @JoinColumn(name = "producao_id", nullable = false)
    private Producao producao;
}
