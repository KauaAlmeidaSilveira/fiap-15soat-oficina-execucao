package br.com.fiap.oficina.controller;

import br.com.fiap.oficina.core.domain.entity.Produto;
import br.com.fiap.oficina.core.domain.enums.TipoProduto;
import br.com.fiap.oficina.core.usecase.ProdutoUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProdutoInternoControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired ProdutoUseCase produtoUseCase;

    @Test
    @DisplayName("Rota interna exige token e devolve os dados da cópia do item")
    void deveExigirToken() throws Exception {
        Produto criado = produtoUseCase.criar(Produto.builder().nome("Velas").tipo(TipoProduto.PECA)
                .precoUnitario(new BigDecimal("30.00")).ativo(true).build(), 1);

        mockMvc.perform(get("/internal/produtos/" + criado.getId())).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/internal/produtos/" + criado.getId()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Velas"))
                .andExpect(jsonPath("$.tipo").value("PECA"))
                .andExpect(jsonPath("$.precoUnitario").value(30.00));
    }
}
