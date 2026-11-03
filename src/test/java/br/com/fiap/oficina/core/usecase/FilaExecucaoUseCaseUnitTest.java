package br.com.fiap.oficina.core.usecase;

import br.com.fiap.oficina.core.domain.entity.ItemFila;
import br.com.fiap.oficina.core.domain.enums.EtapaExecucao;
import br.com.fiap.oficina.core.domain.enums.StatusItemFila;
import br.com.fiap.oficina.core.domain.exception.RecursoNaoEncontradoException;
import br.com.fiap.oficina.core.domain.exception.RegraDeNegocioException;
import br.com.fiap.oficina.core.domain.mensageria.ContextoSaga;
import br.com.fiap.oficina.core.gateway.FilaExecucaoGateway;
import br.com.fiap.oficina.core.gateway.MensageriaGateway;
import br.com.fiap.oficina.core.gateway.SimuladorFalhasGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilaExecucaoUseCaseUnitTest {

    private static final Instant AGORA = Instant.parse("2026-11-03T12:00:00Z");
    private static final ContextoSaga CTX = new ContextoSaga(UUID.randomUUID(), 7L, "corr", UUID.randomUUID());

    @Mock FilaExecucaoGateway gateway;
    @Mock MensageriaGateway mensageria;
    @Mock SimuladorFalhasGateway simulador;

    private FilaExecucaoUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FilaExecucaoUseCase(gateway, mensageria, simulador, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    private ItemFila item(EtapaExecucao etapa, StatusItemFila status) {
        return ItemFila.builder().osId(7L).numeroOs("OS1").sagaId(CTX.sagaId()).correlationId("corr")
                .etapa(etapa).status(status).concluidoEm(status == StatusItemFila.CONCLUIDO ? AGORA : null).build();
    }

    private void comItem(EtapaExecucao etapa, StatusItemFila status) {
        when(gateway.buscar(7L, etapa)).thenReturn(Optional.of(item(etapa, status)));
    }

    private void salvarDevolveOItem() {
        when(gateway.salvar(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Deve enfileirar o diagnóstico e responder")
    void deveEnfileirarDiagnostico() {
        when(gateway.buscar(7L, EtapaExecucao.DIAGNOSTICO)).thenReturn(Optional.empty());

        useCase.enfileirarDiagnostico(CTX, "OS1", "Barulho");

        verify(gateway).salvar(any());
        verify(mensageria).publicarEvento("DiagnosticoEnfileirado", CTX, Map.of());
    }

    @Test
    @DisplayName("Enfileirar de novo não duplica, mas responde")
    void naoDuplicaFila() {
        comItem(EtapaExecucao.DIAGNOSTICO, StatusItemFila.AGUARDANDO);

        useCase.enfileirarDiagnostico(CTX, "OS1", "Barulho");

        verify(gateway, never()).salvar(any());
        verify(mensageria).publicarEvento("DiagnosticoEnfileirado", CTX, Map.of());
    }

    @Test
    @DisplayName("Deve enfileirar o reparo e responder")
    void deveEnfileirarReparo() {
        when(gateway.buscar(7L, EtapaExecucao.REPARO)).thenReturn(Optional.empty());

        useCase.enfileirarReparo(CTX, "OS1");

        verify(gateway).salvar(any());
        verify(mensageria).publicarEvento("ReparoEnfileirado", CTX, Map.of());
    }

    @Test
    @DisplayName("Falha simulada recusa o reparo sem enfileirar")
    void falhaSimuladaRecusaReparo() {
        when(simulador.deveFalhar("ENFILEIRAR_REPARO")).thenReturn(true);

        useCase.enfileirarReparo(CTX, "OS1");

        verify(gateway, never()).salvar(any());
        verify(mensageria).publicarEvento(eq("ReparoRecusado"), eq(CTX), anyMap());
    }

    @Test
    @DisplayName("Deve listar os itens abertos")
    void deveListar() {
        when(gateway.listarAbertos()).thenReturn(List.of(item(EtapaExecucao.REPARO, StatusItemFila.AGUARDANDO)));

        assertThat(useCase.listarAbertos()).hasSize(1);
    }

    @Test
    @DisplayName("Deve iniciar e publicar o início do diagnóstico")
    void deveIniciarDiagnostico() {
        comItem(EtapaExecucao.DIAGNOSTICO, StatusItemFila.AGUARDANDO);
        salvarDevolveOItem();

        ItemFila item = useCase.iniciarDiagnostico(7L);

        assertThat(item.getStatus()).isEqualTo(StatusItemFila.EM_ANDAMENTO);
        verify(mensageria).publicarEvento(eq("DiagnosticoIniciado"), any(), eq(Map.of("iniciadoEm", AGORA.toString())));
    }

    @Test
    @DisplayName("Não deve iniciar diagnóstico já iniciado")
    void naoIniciaDuasVezes() {
        comItem(EtapaExecucao.DIAGNOSTICO, StatusItemFila.EM_ANDAMENTO);

        assertThatThrownBy(() -> useCase.iniciarDiagnostico(7L)).isInstanceOf(RegraDeNegocioException.class);
        verify(mensageria, never()).publicarEvento(any(), any(), any());
    }

    @Test
    @DisplayName("Não deve concluir diagnóstico que não começou")
    void naoConcluiSemIniciar() {
        comItem(EtapaExecucao.DIAGNOSTICO, StatusItemFila.AGUARDANDO);

        assertThatThrownBy(() -> useCase.concluirDiagnostico(7L)).isInstanceOf(RegraDeNegocioException.class);
    }

    @Test
    @DisplayName("Deve concluir o diagnóstico em andamento")
    void deveConcluirDiagnostico() {
        comItem(EtapaExecucao.DIAGNOSTICO, StatusItemFila.EM_ANDAMENTO);
        salvarDevolveOItem();

        assertThat(useCase.concluirDiagnostico(7L).getStatus()).isEqualTo(StatusItemFila.CONCLUIDO);
        verify(mensageria).publicarEvento(eq("DiagnosticoConcluido"), any(), anyMap());
    }

    @Test
    @DisplayName("Concluir de novo republica, para reabrir o fluxo depois de falha no orçamento (D17)")
    void concluirDeNovoRepublica() {
        comItem(EtapaExecucao.DIAGNOSTICO, StatusItemFila.CONCLUIDO);

        useCase.concluirDiagnostico(7L);

        verify(gateway, never()).salvar(any());
        verify(mensageria).publicarEvento(eq("DiagnosticoConcluido"), any(), anyMap());
    }

    @Test
    @DisplayName("Deve iniciar e finalizar o reparo")
    void deveIniciarEFinalizarReparo() {
        comItem(EtapaExecucao.REPARO, StatusItemFila.AGUARDANDO);
        salvarDevolveOItem();
        useCase.iniciarReparo(7L);
        verify(mensageria).publicarEvento(eq("ReparoIniciado"), any(), anyMap());

        comItem(EtapaExecucao.REPARO, StatusItemFila.EM_ANDAMENTO);
        assertThat(useCase.finalizarReparo(7L).getStatus()).isEqualTo(StatusItemFila.CONCLUIDO);
        verify(mensageria).publicarEvento(eq("ReparoFinalizado"), any(), anyMap());
    }

    @Test
    @DisplayName("Operação em OS fora da fila responde não encontrado")
    void foraDaFila() {
        when(gateway.buscar(7L, EtapaExecucao.REPARO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.iniciarReparo(7L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("Deve cancelar reparo em andamento (D22)")
    void deveCancelarReparo() {
        comItem(EtapaExecucao.REPARO, StatusItemFila.EM_ANDAMENTO);
        salvarDevolveOItem();

        useCase.cancelarReparo(CTX, "OS1", "Pedido do cliente");

        verify(gateway).salvar(any());
        verify(mensageria).publicarEvento("ReparoCancelado", CTX, Map.of("canceladoEm", AGORA.toString()));
    }

    @Test
    @DisplayName("Cancelamento de reparo já finalizado responde ReparoJaFinalizado (D22)")
    void cancelamentoDepoisDeFinalizado() {
        comItem(EtapaExecucao.REPARO, StatusItemFila.CONCLUIDO);

        useCase.cancelarReparo(CTX, "OS1", "Tarde demais");

        verify(gateway, never()).salvar(any());
        verify(mensageria).publicarEvento("ReparoJaFinalizado", CTX, Map.of("finalizadoEm", AGORA.toString()));
    }

    @Test
    @DisplayName("Cancelar antes do enfileiramento grava marcador cancelado, sem item fantasma")
    void cancelarAntesDeEnfileirar() {
        when(gateway.buscar(7L, EtapaExecucao.REPARO))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(item(EtapaExecucao.REPARO, StatusItemFila.AGUARDANDO)));
        salvarDevolveOItem();

        useCase.cancelarReparo(CTX, "OS1", "Prazo esgotado");

        verify(mensageria).publicarEvento("ReparoCancelado", CTX, Map.of("canceladoEm", AGORA.toString()));
    }

    @Test
    @DisplayName("Cancelamento repetido só responde de novo")
    void cancelamentoRepetido() {
        ItemFila cancelado = item(EtapaExecucao.REPARO, StatusItemFila.CANCELADO);
        cancelado.setConcluidoEm(AGORA);
        when(gateway.buscar(7L, EtapaExecucao.REPARO)).thenReturn(Optional.of(cancelado));

        useCase.cancelarReparo(CTX, "OS1", "De novo");

        verify(gateway, never()).salvar(any());
        verify(mensageria).publicarEvento(eq("ReparoCancelado"), eq(CTX), anyMap());
    }
}
