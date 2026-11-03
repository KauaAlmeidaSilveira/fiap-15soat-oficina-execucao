package br.com.fiap.oficina.dataprovider.persistence.repository;

import br.com.fiap.oficina.core.domain.enums.EtapaExecucao;
import br.com.fiap.oficina.core.domain.enums.StatusItemFila;
import br.com.fiap.oficina.dataprovider.persistence.entity.ItemFilaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ItemFilaRepository extends JpaRepository<ItemFilaEntity, Long> {

    Optional<ItemFilaEntity> findByOsIdAndEtapa(Long osId, EtapaExecucao etapa);

    List<ItemFilaEntity> findByStatusInOrderByCriadoEmAsc(Collection<StatusItemFila> status);
}
