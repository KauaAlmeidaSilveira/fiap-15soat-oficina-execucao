package br.com.fiap.oficina.entrypoint.controller;

import br.com.fiap.oficina.core.domain.entity.Produto;
import br.com.fiap.oficina.core.usecase.ProdutoUseCase;
import br.com.fiap.oficina.dto.response.ProdutoInternoResponse;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/produtos")
@RequiredArgsConstructor
@Hidden
public class ProdutoInternoController {

    private final ProdutoUseCase produtoUseCase;

    @GetMapping("/{id}")
    public ProdutoInternoResponse buscar(@PathVariable Long id) {
        Produto produto = produtoUseCase.buscarPorId(id);
        return new ProdutoInternoResponse(produto.getId(), produto.getNome(), produto.getTipo(), produto.getPrecoUnitario());
    }
}
