package br.com.fiap.oficina.controller;

import br.com.fiap.oficina.core.domain.enums.EtapaExecucao;
import br.com.fiap.oficina.core.domain.enums.StatusItemFila;
import br.com.fiap.oficina.core.domain.mensageria.ContextoSaga;
import br.com.fiap.oficina.core.gateway.FilaExecucaoGateway;
import br.com.fiap.oficina.core.gateway.MensageriaGateway;
import br.com.fiap.oficina.core.usecase.FilaExecucaoUseCase;
import br.com.fiap.oficina.dataprovider.mensageria.OutboxMensagem;
import br.com.fiap.oficina.dataprovider.mensageria.OutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Rotas de operador: item da fila e evento na outbox entram juntos ou não entram. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WithMockUser(roles = "OPERADOR")
class ExecucaoControllerTransacaoIT {

    private static final long OS_ID = 7L;

    @Autowired MockMvc mockMvc;
    @Autowired FilaExecucaoUseCase useCase;
    @Autowired FilaExecucaoGateway filaGateway;
    @Autowired OutboxRepository outbox;
    @SpyBean MensageriaGateway mensageria;

    private ContextoSaga contexto() {
        return new ContextoSaga(UUID.randomUUID(), OS_ID, "corr-1", UUID.randomUUID());
    }

    private void falharAoPublicar(String tipo) {
        doThrow(new RuntimeException("Falha simulada na outbox"))
                .when(mensageria).publicarEvento(eq(tipo), any(), anyMap());
    }

    private StatusItemFila statusDoItem(EtapaExecucao etapa) {
        return filaGateway.buscar(OS_ID, etapa).orElseThrow().getStatus();
    }

    private void assertSemEvento(String tipo) {
        assertThat(outbox.findAll()).extracting(OutboxMensagem::getTipo).doesNotContain(tipo);
    }

    @Test
    @DisplayName("Falha na outbox ao iniciar o diagnóstico desfaz o início")
    void iniciarDiagnostico() throws Exception {
        useCase.enfileirarDiagnostico(contexto(), "OS1", "Barulho");
        falharAoPublicar("DiagnosticoIniciado");

        mockMvc.perform(post("/api/execucoes/7/diagnostico/iniciar")).andExpect(status().isInternalServerError());

        assertThat(statusDoItem(EtapaExecucao.DIAGNOSTICO)).isEqualTo(StatusItemFila.AGUARDANDO);
        assertSemEvento("DiagnosticoIniciado");
    }

    @Test
    @DisplayName("Falha na outbox ao concluir o diagnóstico desfaz a conclusão")
    void concluirDiagnostico() throws Exception {
        useCase.enfileirarDiagnostico(contexto(), "OS1", "Barulho");
        mockMvc.perform(post("/api/execucoes/7/diagnostico/iniciar")).andExpect(status().isOk());
        falharAoPublicar("DiagnosticoConcluido");

        mockMvc.perform(post("/api/execucoes/7/diagnostico/concluir")).andExpect(status().isInternalServerError());

        assertThat(statusDoItem(EtapaExecucao.DIAGNOSTICO)).isEqualTo(StatusItemFila.EM_ANDAMENTO);
        assertSemEvento("DiagnosticoConcluido");
    }

    @Test
    @DisplayName("Falha na outbox ao iniciar o reparo desfaz o início")
    void iniciarReparo() throws Exception {
        useCase.enfileirarReparo(contexto(), "OS1");
        falharAoPublicar("ReparoIniciado");

        mockMvc.perform(post("/api/execucoes/7/reparo/iniciar")).andExpect(status().isInternalServerError());

        assertThat(statusDoItem(EtapaExecucao.REPARO)).isEqualTo(StatusItemFila.AGUARDANDO);
        assertSemEvento("ReparoIniciado");
    }

    @Test
    @DisplayName("Falha na outbox ao finalizar o reparo desfaz a finalização")
    void finalizarReparo() throws Exception {
        useCase.enfileirarReparo(contexto(), "OS1");
        mockMvc.perform(post("/api/execucoes/7/reparo/iniciar")).andExpect(status().isOk());
        falharAoPublicar("ReparoFinalizado");

        mockMvc.perform(post("/api/execucoes/7/reparo/finalizar")).andExpect(status().isInternalServerError());

        assertThat(statusDoItem(EtapaExecucao.REPARO)).isEqualTo(StatusItemFila.EM_ANDAMENTO);
        assertSemEvento("ReparoFinalizado");
    }
}
