package br.com.fiap.oficina.dto.response;

import br.com.fiap.oficina.core.domain.enums.EtapaExecucao;
import br.com.fiap.oficina.core.domain.enums.StatusItemFila;

import java.time.Instant;

public record ItemFilaResponse(
    Long osId,
    String numeroOs,
    EtapaExecucao etapa,
    StatusItemFila status,
    String descricao,
    Instant criadoEm,
    Instant iniciadoEm,
    Instant concluidoEm
) {}
