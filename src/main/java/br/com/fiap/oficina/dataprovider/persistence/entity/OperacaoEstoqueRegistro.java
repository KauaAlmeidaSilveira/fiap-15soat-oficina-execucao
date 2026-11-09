package br.com.fiap.oficina.dataprovider.persistence.entity;

import br.com.fiap.oficina.core.domain.enums.OperacaoEstoqueSaga;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "operacao_estoque_saga", uniqueConstraints = @UniqueConstraint(columnNames = {"os_id", "operacao"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class OperacaoEstoqueRegistro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "os_id", nullable = false)
    private Long osId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperacaoEstoqueSaga operacao;
}
