package br.com.fiap.oficina.dataprovider.persistence.entity;

import br.com.fiap.oficina.core.domain.enums.EtapaExecucao;
import br.com.fiap.oficina.core.domain.enums.StatusItemFila;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fila_execucao", uniqueConstraints = @UniqueConstraint(columnNames = {"os_id", "etapa"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ItemFilaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "os_id", nullable = false)
    private Long osId;

    @Column(nullable = false, length = 20)
    private String numeroOs;

    @Column(nullable = false)
    private UUID sagaId;

    @Column(length = 64)
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EtapaExecucao etapa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusItemFila status;

    @Column(length = 500)
    private String descricao;

    @Column(nullable = false, updatable = false)
    private Instant criadoEm;

    private Instant iniciadoEm;
    private Instant concluidoEm;

    @PrePersist
    protected void onCreate() {
        if (criadoEm == null) {
            criadoEm = Instant.now();
        }
    }
}
