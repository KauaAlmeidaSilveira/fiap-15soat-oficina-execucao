package br.com.fiap.oficina.dataprovider.persistence.mapper;

import br.com.fiap.oficina.core.domain.entity.ItemFila;
import br.com.fiap.oficina.dataprovider.persistence.entity.ItemFilaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ItemFilaPersistenceMapper {

    ItemFila toDomain(ItemFilaEntity entity);

    ItemFilaEntity toEntity(ItemFila item);
}
