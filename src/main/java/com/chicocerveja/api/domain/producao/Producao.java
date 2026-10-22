package com.chicocerveja.api.domain.producao;

import com.chicocerveja.api.domain.enums.StatusProducao;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "producao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Producao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String nome_cerveja;
    private Date data_producao;
    private Double quantidade_producao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusProducao status_producao;
}
