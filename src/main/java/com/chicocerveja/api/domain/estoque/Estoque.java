package com.chicocerveja.api.domain.estoque;

import com.chicocerveja.api.domain.enums.CategoriaEstoque;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Table(name = "estoque")
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Estoque {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaEstoque categoria;

    private String marca;
    private Double quantidade;
    private Date data_cadastro;
}
