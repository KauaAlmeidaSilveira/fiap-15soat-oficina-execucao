package br.com.fiap.oficina.config;

import br.com.fiap.oficina.core.gateway.EstoqueGateway;
import br.com.fiap.oficina.core.gateway.EstoqueSagaGateway;
import br.com.fiap.oficina.core.gateway.FilaExecucaoGateway;
import br.com.fiap.oficina.core.gateway.MensageriaGateway;
import br.com.fiap.oficina.core.gateway.ProdutoGateway;
import br.com.fiap.oficina.core.gateway.SimuladorFalhasGateway;
import br.com.fiap.oficina.core.usecase.EstoqueSagaUseCase;
import br.com.fiap.oficina.core.usecase.FilaExecucaoUseCase;
import br.com.fiap.oficina.core.usecase.ProdutoUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class UseCaseConfig {

    @Bean
    public ProdutoUseCase produtoUseCase(ProdutoGateway produtoGateway, EstoqueGateway estoqueGateway) {
        return new ProdutoUseCase(produtoGateway, estoqueGateway);
    }

    @Bean
    public FilaExecucaoUseCase filaExecucaoUseCase(FilaExecucaoGateway filaGateway, MensageriaGateway mensageria,
                                                   SimuladorFalhasGateway simulador) {
        return new FilaExecucaoUseCase(filaGateway, mensageria, simulador, Clock.systemUTC());
    }

    @Bean
    public EstoqueSagaUseCase estoqueSagaUseCase(EstoqueSagaGateway estoqueSagaGateway, MensageriaGateway mensageria) {
        return new EstoqueSagaUseCase(estoqueSagaGateway, mensageria);
    }
}
