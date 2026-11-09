package br.com.fiap.oficina.dto.response;

import br.com.fiap.oficina.core.domain.enums.TipoProduto;

import java.math.BigDecimal;

public record ProdutoInternoResponse(Long id, String nome, TipoProduto tipo, BigDecimal precoUnitario) {}
