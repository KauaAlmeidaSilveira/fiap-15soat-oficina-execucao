package br.com.fiap.oficina.dataprovider.gateway;

import br.com.fiap.oficina.core.domain.entity.ItemFila;
import br.com.fiap.oficina.core.domain.enums.EtapaExecucao;
import br.com.fiap.oficina.core.domain.enums.StatusItemFila;
import br.com.fiap.oficina.core.gateway.FilaExecucaoGateway;
import br.com.fiap.oficina.dataprovider.persistence.mapper.ItemFilaPersistenceMapper;
import br.com.fiap.oficina.dataprovider.persistence.repository.ItemFilaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FilaExecucaoGatewayImpl implements FilaExecucaoGateway {

    private final ItemFilaRepository repository;
    private final ItemFilaPersistenceMapper mapper;

    @Override
    @Transactional
    public ItemFila salvar(ItemFila item) {
        return mapper.toDomain(repository.save(mapper.toEntity(item)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ItemFila> buscar(Long osId, EtapaExecucao etapa) {
        return repository.findByOsIdAndEtapa(osId, etapa).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ItemFila> listarAbertos() {
        return repository.findByStatusInOrderByCriadoEmAsc(List.of(StatusItemFila.AGUARDANDO, StatusItemFila.EM_ANDAMENTO))
                .stream().map(mapper::toDomain).toList();
    }
}
