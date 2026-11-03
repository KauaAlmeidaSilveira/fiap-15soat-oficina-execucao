package br.com.fiap.oficina.entrypoint.controller.mapper;

import br.com.fiap.oficina.core.domain.entity.ItemFila;
import br.com.fiap.oficina.dto.response.ItemFilaResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ItemFilaDtoMapper {

    ItemFilaResponse toResponse(ItemFila item);
}
