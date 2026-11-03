package br.com.fiap.oficina.config;

import br.com.fiap.oficina.core.gateway.EstoqueGateway;
import br.com.fiap.oficina.core.gateway.ProdutoGateway;
import br.com.fiap.oficina.core.usecase.ProdutoUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    public ProdutoUseCase produtoUseCase(ProdutoGateway produtoGateway, EstoqueGateway estoqueGateway) {
        return new ProdutoUseCase(produtoGateway, estoqueGateway);
    }
}
