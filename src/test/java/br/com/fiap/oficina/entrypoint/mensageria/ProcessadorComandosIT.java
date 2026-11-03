package br.com.fiap.oficina.entrypoint.mensageria;

import br.com.fiap.oficina.core.domain.entity.Produto;
import br.com.fiap.oficina.core.domain.enums.TipoProduto;
import br.com.fiap.oficina.core.usecase.ProdutoUseCase;
import br.com.fiap.oficina.dataprovider.mensageria.Envelope;
import br.com.fiap.oficina.dataprovider.mensageria.OutboxMensagem;
import br.com.fiap.oficina.dataprovider.mensageria.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ProcessadorComandosIT {

    @Autowired ProcessadorComandos processador;
    @Autowired ProdutoUseCase produtoUseCase;
    @Autowired OutboxRepository outbox;
    @Autowired ObjectMapper objectMapper;

    private Long novaPeca(int saldo) {
        return produtoUseCase.criar(Produto.builder().nome("Peça " + UUID.randomUUID()).tipo(TipoProduto.PECA)
                .precoUnitario(BigDecimal.TEN).ativo(true).build(), saldo).getId();
    }

    private Envelope comando(String tipo, Object payload) {
        return new Envelope(UUID.randomUUID(), tipo, 1, UUID.randomUUID(), 7L, "corr", null, Instant.now(),
                objectMapper.valueToTree(payload));
    }

    private List<String> eventos() {
        return outbox.findAll().stream().map(OutboxMensagem::getTipo).toList();
    }

    private int saldo(Long id) {
        return produtoUseCase.buscarPorId(id).getSaldoEstoque();
    }

    @Test
    @DisplayName("Baixa e devolução alteram o saldo uma vez cada, mesmo repetindo o comando com outro ID")
    void baixaEDevolucaoSaoIdempotentes() {
        Long peca = novaPeca(10);
        Map<String, Object> itens = Map.of("itens", List.of(Map.of("produtoId", peca, "quantidade", 4)));

        processador.processar(comando("BaixarEstoque", itens));
        processador.processar(comando("BaixarEstoque", itens));
        assertThat(saldo(peca)).isEqualTo(6);

        processador.processar(comando("DevolverEstoque", Map.of("motivo", "QR expirado")));
        processador.processar(comando("DevolverEstoque", Map.of("motivo", "QR expirado")));
        assertThat(saldo(peca)).isEqualTo(10);

        processador.processar(comando("BaixarEstoque", itens));
        assertThat(saldo(peca)).as("baixa atrasada depois da devolução").isEqualTo(10);
        assertThat(eventos()).containsExactlyInAnyOrder("EstoqueBaixado", "EstoqueBaixado", "EstoqueDevolvido", "EstoqueDevolvido");
    }

    @Test
    @DisplayName("Mesma mensagem entregue duas vezes tem um só efeito e uma só resposta")
    void mensagemDuplicada() {
        Long peca = novaPeca(10);
        Envelope baixa = comando("BaixarEstoque", Map.of("itens", List.of(Map.of("produtoId", peca, "quantidade", 4))));

        processador.processar(baixa);
        processador.processar(baixa);

        assertThat(saldo(peca)).isEqualTo(6);
        assertThat(eventos()).containsExactly("EstoqueBaixado");
    }

    @Test
    @DisplayName("Estoque insuficiente não altera nenhum saldo")
    void estoqueInsuficiente() {
        Long comSaldo = novaPeca(10);
        Long semSaldo = novaPeca(1);

        processador.processar(comando("BaixarEstoque", Map.of("itens", List.of(
                Map.of("produtoId", comSaldo, "quantidade", 2),
                Map.of("produtoId", semSaldo, "quantidade", 2)))));

        assertThat(saldo(comSaldo)).isEqualTo(10);
        assertThat(saldo(semSaldo)).isEqualTo(1);
        assertThat(eventos()).containsExactly("EstoqueInsuficiente");
    }

    @Test
    @DisplayName("Comandos de fila respondem; comando desconhecido é ignorado sem erro")
    void comandosDeFila() {
        processador.processar(comando("EnfileirarDiagnostico", Map.of("numero", "OS1", "descricaoProblema", "Barulho")));
        processador.processar(comando("EnfileirarReparo", Map.of("numero", "OS1")));
        processador.processar(comando("CancelarReparo", Map.of("motivo", "Cliente desistiu")));
        processador.processar(comando("Inexistente", Map.of()));

        assertThat(eventos()).containsExactlyInAnyOrder("DiagnosticoEnfileirado", "ReparoEnfileirado", "ReparoCancelado");
    }
}
