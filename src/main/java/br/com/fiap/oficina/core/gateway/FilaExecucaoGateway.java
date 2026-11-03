package br.com.fiap.oficina.core.gateway;

import br.com.fiap.oficina.core.domain.entity.ItemFila;
import br.com.fiap.oficina.core.domain.enums.EtapaExecucao;

import java.util.List;
import java.util.Optional;

public interface FilaExecucaoGateway {

    ItemFila salvar(ItemFila item);

    Optional<ItemFila> buscar(Long osId, EtapaExecucao etapa);

    List<ItemFila> listarAbertos();
}
