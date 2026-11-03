package br.com.fiap.oficina.config;

import br.com.fiap.oficina.core.domain.enums.TipoMovimentacao;
import br.com.fiap.oficina.core.domain.enums.TipoProduto;
import br.com.fiap.oficina.dataprovider.persistence.entity.MovimentacaoEstoque;
import br.com.fiap.oficina.dataprovider.persistence.entity.Produto;
import br.com.fiap.oficina.dataprovider.persistence.entity.SaldoEstoque;
import br.com.fiap.oficina.dataprovider.persistence.repository.MovimentacaoEstoqueRepository;
import br.com.fiap.oficina.dataprovider.persistence.repository.ProdutoRepository;
import br.com.fiap.oficina.dataprovider.persistence.repository.SaldoEstoqueRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;

@Configuration
@Slf4j
public class DataLoader {

    // A ordem define os IDs 1 a 9, citados nas cópias de item do DataLoader do OS Service.
    @Bean
    @Profile("!test")
    CommandLineRunner loadData(ProdutoRepository produtoRepo, SaldoEstoqueRepository saldoRepo,
                               MovimentacaoEstoqueRepository movRepo) {
        return args -> {
            if (produtoRepo.count() > 0) {
                log.info("=== Dados já existem no banco — DataLoader ignorado ===");
                return;
            }
            criarPeca(produtoRepo, saldoRepo, movRepo, "Filtro de Óleo", "Filtro original 1L", "45.00", "UN", 50);
            criarPeca(produtoRepo, saldoRepo, movRepo, "Filtro de Ar", "Filtro de ar do motor", "35.00", "UN", 30);
            criarPeca(produtoRepo, saldoRepo, movRepo, "Óleo Motor 5W30", "Óleo sintético 5W30 - 1L", "28.00", "L", 100);
            criarPeca(produtoRepo, saldoRepo, movRepo, "Pastilha de Freio Dianteira", "Kit com 4 pastilhas", "120.00", "KIT", 20);
            criarPeca(produtoRepo, saldoRepo, movRepo, "Correia Dentada", "Correia dentada + tensor", "180.00", "KIT", 15);
            criarServico(produtoRepo, "Troca de Óleo e Filtro", "Mão de obra para troca de óleo", "80.00");
            criarServico(produtoRepo, "Alinhamento e Balanceamento", "Alinhamento 4 rodas + balanceamento", "150.00");
            criarServico(produtoRepo, "Revisão de Freios", "Inspeção e substituição de pastilhas/discos", "120.00");
            criarServico(produtoRepo, "Troca de Correia Dentada", "Mão de obra para troca de correia", "200.00");
            log.info("=== Produtos carregados: {} ===", produtoRepo.count());
        };
    }

    private void criarPeca(ProdutoRepository prodRepo, SaldoEstoqueRepository saldoRepo, MovimentacaoEstoqueRepository movRepo,
                           String nome, String desc, String preco, String unidade, int qtdInicial) {
        Produto p = prodRepo.save(Produto.builder()
                .nome(nome).descricao(desc).tipo(TipoProduto.PECA)
                .precoUnitario(new BigDecimal(preco)).unidadeMedida(unidade).ativo(true).build());
        saldoRepo.save(SaldoEstoque.builder().produto(p).quantidade(qtdInicial).build());
        movRepo.save(MovimentacaoEstoque.builder()
                .produto(p)
                .tipo(TipoMovimentacao.ENTRADA)
                .quantidade(qtdInicial)
                .motivo("Estoque inicial - carga do sistema")
                .build());
    }

    private void criarServico(ProdutoRepository prodRepo, String nome, String desc, String preco) {
        prodRepo.save(Produto.builder()
                .nome(nome).descricao(desc).tipo(TipoProduto.SERVICO)
                .precoUnitario(new BigDecimal(preco)).ativo(true).build());
    }
}
