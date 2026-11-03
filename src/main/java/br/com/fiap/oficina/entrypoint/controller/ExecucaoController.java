package br.com.fiap.oficina.entrypoint.controller;

import br.com.fiap.oficina.core.usecase.FilaExecucaoUseCase;
import br.com.fiap.oficina.dto.response.ItemFilaResponse;
import br.com.fiap.oficina.entrypoint.controller.mapper.ItemFilaDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/execucoes")
@RequiredArgsConstructor
@Tag(name = "Execução", description = "Fila de execução, diagnóstico e reparo")
public class ExecucaoController {

    private final FilaExecucaoUseCase filaUseCase;
    private final ItemFilaDtoMapper mapper;

    @GetMapping
    @Operation(summary = "Listar a fila de execução: itens aguardando ou em andamento, mais antigos primeiro")
    public ResponseEntity<List<ItemFilaResponse>> listar() {
        return ResponseEntity.ok(filaUseCase.listarAbertos().stream().map(mapper::toResponse).toList());
    }

    @PostMapping("/{osId}/diagnostico/iniciar")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    @Operation(summary = "Iniciar o diagnóstico da OS")
    public ResponseEntity<ItemFilaResponse> iniciarDiagnostico(@PathVariable Long osId) {
        return ResponseEntity.ok(mapper.toResponse(filaUseCase.iniciarDiagnostico(osId)));
    }

    @PostMapping("/{osId}/diagnostico/concluir")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    @Operation(summary = "Concluir o diagnóstico da OS. Os itens da OS congelam neste ponto.")
    public ResponseEntity<ItemFilaResponse> concluirDiagnostico(@PathVariable Long osId) {
        return ResponseEntity.ok(mapper.toResponse(filaUseCase.concluirDiagnostico(osId)));
    }

    @PostMapping("/{osId}/reparo/iniciar")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    @Operation(summary = "Iniciar o reparo da OS")
    public ResponseEntity<ItemFilaResponse> iniciarReparo(@PathVariable Long osId) {
        return ResponseEntity.ok(mapper.toResponse(filaUseCase.iniciarReparo(osId)));
    }

    @PostMapping("/{osId}/reparo/finalizar")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    @Operation(summary = "Finalizar o reparo da OS")
    public ResponseEntity<ItemFilaResponse> finalizarReparo(@PathVariable Long osId) {
        return ResponseEntity.ok(mapper.toResponse(filaUseCase.finalizarReparo(osId)));
    }
}
