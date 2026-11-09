package br.com.fiap.oficina.controller;

import br.com.fiap.oficina.core.domain.mensageria.ContextoSaga;
import br.com.fiap.oficina.core.usecase.FilaExecucaoUseCase;
import br.com.fiap.oficina.dataprovider.mensageria.OutboxMensagem;
import br.com.fiap.oficina.dataprovider.mensageria.OutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WithMockUser(roles = "OPERADOR")
class ExecucaoControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired FilaExecucaoUseCase useCase;
    @Autowired OutboxRepository outbox;

    private ContextoSaga contexto() {
        return new ContextoSaga(UUID.randomUUID(), 7L, "corr-1", UUID.randomUUID());
    }

    @Test
    @DisplayName("Deve percorrer diagnóstico e reparo, gravando os eventos na outbox")
    void devePercorrerFluxo() throws Exception {
        useCase.enfileirarDiagnostico(contexto(), "OS1", "Barulho");

        mockMvc.perform(get("/api/execucoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].osId").value(7))
                .andExpect(jsonPath("$[0].etapa").value("DIAGNOSTICO"));

        mockMvc.perform(post("/api/execucoes/7/diagnostico/iniciar")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));
        mockMvc.perform(post("/api/execucoes/7/diagnostico/concluir")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDO"));

        useCase.enfileirarReparo(contexto(), "OS1");
        mockMvc.perform(post("/api/execucoes/7/reparo/iniciar")).andExpect(status().isOk());
        mockMvc.perform(post("/api/execucoes/7/reparo/finalizar")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDO"));

        assertThat(outbox.findAll()).extracting(OutboxMensagem::getTipo).containsExactlyInAnyOrder(
                "DiagnosticoEnfileirado", "DiagnosticoIniciado", "DiagnosticoConcluido",
                "ReparoEnfileirado", "ReparoIniciado", "ReparoFinalizado");
        mockMvc.perform(get("/api/execucoes")).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Passo fora de ordem responde 422; OS fora da fila responde 404")
    void deveValidarPassos() throws Exception {
        mockMvc.perform(post("/api/execucoes/7/reparo/iniciar")).andExpect(status().isNotFound());

        useCase.enfileirarDiagnostico(contexto(), "OS1", "Barulho");
        mockMvc.perform(post("/api/execucoes/7/diagnostico/concluir")).andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(roles = "RECEPCAO")
    @DisplayName("Recepção só consulta a fila")
    void recepcaoSoConsulta() throws Exception {
        mockMvc.perform(get("/api/execucoes")).andExpect(status().isOk());
        mockMvc.perform(post("/api/execucoes/7/diagnostico/iniciar")).andExpect(status().isForbidden());
    }
}
