package br.com.fiap.oficina.core.domain.entity;

import br.com.fiap.oficina.core.domain.enums.EtapaExecucao;
import br.com.fiap.oficina.core.domain.enums.StatusItemFila;
import br.com.fiap.oficina.core.domain.exception.RegraDeNegocioException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemFila {

    private Long id;
    private Long osId;
    private String numeroOs;
    private UUID sagaId;
    private String correlationId;
    private EtapaExecucao etapa;

    @Builder.Default
    private StatusItemFila status = StatusItemFila.AGUARDANDO;

    private String descricao;
    private Instant criadoEm;
    private Instant iniciadoEm;
    private Instant concluidoEm;

    public void iniciar(Instant agora) {
        if (status != StatusItemFila.AGUARDANDO) {
            throw new RegraDeNegocioException(
                    "A etapa " + etapa + " da OS " + osId + " precisa estar AGUARDANDO para ser iniciada.");
        }
        status = StatusItemFila.EM_ANDAMENTO;
        iniciadoEm = agora;
    }

    public void concluir(Instant agora) {
        if (status != StatusItemFila.EM_ANDAMENTO) {
            throw new RegraDeNegocioException(
                    "A etapa " + etapa + " da OS " + osId + " precisa estar EM_ANDAMENTO para ser concluída.");
        }
        status = StatusItemFila.CONCLUIDO;
        concluidoEm = agora;
    }

    public void cancelar(Instant agora) {
        status = StatusItemFila.CANCELADO;
        concluidoEm = agora;
    }
}
