package br.com.fiap.oficina.core.usecase;

import br.com.fiap.oficina.core.domain.enums.OperacaoEstoqueSaga;
import br.com.fiap.oficina.core.domain.enums.TipoMovimentacao;
import br.com.fiap.oficina.core.domain.mensageria.ContextoSaga;
import br.com.fiap.oficina.core.gateway.EstoqueSagaGateway;
import br.com.fiap.oficina.core.gateway.EstoqueSagaGateway.Movimento;
import br.com.fiap.oficina.core.gateway.MensageriaGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstoqueSagaUseCaseUnitTest {

    private static final ContextoSaga CTX = new ContextoSaga(UUID.randomUUID(), 7L, "corr", UUID.randomUUID());

    @Mock EstoqueSagaGateway gateway;
    @Mock MensageriaGateway mensageria;

    private EstoqueSagaUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new EstoqueSagaUseCase(gateway, mensageria);
    }

    @Test
    @DisplayName("Deve baixar tudo e somar quantidades do mesmo produto")
    void deveBaixarTudo() {
        when(gateway.travarSaldos(any())).thenReturn(Map.of(1L, 5, 2L, 1));

        useCase.baixar(CTX, List.of(new Movimento(1L, 2), new Movimento(1L, 3), new Movimento(2L, 1)));

        verify(gateway).marcarOperacao(7L, OperacaoEstoqueSaga.BAIXA);
        verify(gateway).registrar(eq(7L), eq(OperacaoEstoqueSaga.BAIXA), eq(TipoMovimentacao.SAIDA), eq(new Movimento(1L, 5)), anyString());
        verify(gateway).registrar(eq(7L), eq(OperacaoEstoqueSaga.BAIXA), eq(TipoMovimentacao.SAIDA), eq(new Movimento(2L, 1)), anyString());
        verify(mensageria).publicarEvento(eq("EstoqueBaixado"), eq(CTX), anyMap());
    }

    @Test
    @DisplayName("Falta em um item não baixa nenhum: a baixa é tudo ou nada (D18)")
    void deveSerTudoOuNada() {
        when(gateway.travarSaldos(any())).thenReturn(Map.of(1L, 5));

        useCase.baixar(CTX, List.of(new Movimento(1L, 2), new Movimento(2L, 1)));

        verify(gateway, never()).marcarOperacao(any(), any());
        verify(gateway, never()).registrar(any(), any(), any(), any(), any());
        verify(mensageria).publicarEvento(eq("EstoqueInsuficiente"), eq(CTX), anyMap());
    }

    @Test
    @DisplayName("Baixa repetida só responde de novo, sem nova movimentação")
    void baixaRepetidaNaoMovimenta() {
        when(gateway.existeOperacao(7L, OperacaoEstoqueSaga.DEVOLUCAO)).thenReturn(false);
        when(gateway.existeOperacao(7L, OperacaoEstoqueSaga.BAIXA)).thenReturn(true);
        when(gateway.listarMovimentos(7L, OperacaoEstoqueSaga.BAIXA)).thenReturn(List.of(new Movimento(1L, 2)));

        useCase.baixar(CTX, List.of(new Movimento(1L, 2)));

        verify(gateway, never()).registrar(any(), any(), any(), any(), any());
        verify(mensageria).publicarEvento(eq("EstoqueBaixado"), eq(CTX), anyMap());
    }

    @Test
    @DisplayName("Baixa atrasada depois da devolução não tem efeito nem resposta")
    void baixaAtrasadaEhIgnorada() {
        when(gateway.existeOperacao(7L, OperacaoEstoqueSaga.DEVOLUCAO)).thenReturn(true);

        useCase.baixar(CTX, List.of(new Movimento(1L, 2)));

        verify(gateway, never()).registrar(any(), any(), any(), any(), any());
        verify(mensageria, never()).publicarEvento(any(), any(), any());
    }

    @Test
    @DisplayName("Deve devolver tudo o que foi baixado")
    void deveDevolver() {
        when(gateway.listarMovimentos(7L, OperacaoEstoqueSaga.BAIXA))
                .thenReturn(List.of(new Movimento(1L, 2), new Movimento(2L, 1)));

        useCase.devolver(CTX, "QR expirado");

        verify(gateway).marcarOperacao(7L, OperacaoEstoqueSaga.DEVOLUCAO);
        verify(gateway, times(2)).registrar(eq(7L), eq(OperacaoEstoqueSaga.DEVOLUCAO), eq(TipoMovimentacao.ENTRADA), any(), anyString());
        verify(mensageria).publicarEvento(eq("EstoqueDevolvido"), eq(CTX), anyMap());
    }

    @Test
    @DisplayName("Devolução sem baixa grava o marcador e responde vazio")
    void devolucaoSemBaixa() {
        when(gateway.listarMovimentos(7L, OperacaoEstoqueSaga.BAIXA)).thenReturn(List.of());

        useCase.devolver(CTX, "prazo esgotado");

        verify(gateway).marcarOperacao(7L, OperacaoEstoqueSaga.DEVOLUCAO);
        verify(gateway, never()).registrar(any(), any(), any(), any(), any());
        verify(mensageria).publicarEvento("EstoqueDevolvido", CTX, Map.of("movimentacoes", List.of()));
    }

    @Test
    @DisplayName("Devolução repetida não devolve de novo")
    void devolucaoRepetida() {
        when(gateway.existeOperacao(7L, OperacaoEstoqueSaga.DEVOLUCAO)).thenReturn(true);
        when(gateway.listarMovimentos(7L, OperacaoEstoqueSaga.DEVOLUCAO)).thenReturn(List.of(new Movimento(1L, 2)));

        useCase.devolver(CTX, "repetida");

        verify(gateway, never()).registrar(any(), any(), any(), any(), any());
        verify(mensageria).publicarEvento(eq("EstoqueDevolvido"), eq(CTX), anyMap());
    }
}
