package sv.edu.udb.pokebattle.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import sv.edu.udb.pokebattle.dto.CrearPartidaRequest;
import sv.edu.udb.pokebattle.dto.PartidaResponse;
import sv.edu.udb.pokebattle.dto.UnirsePartidaRequest;
import sv.edu.udb.pokebattle.service.PartidaService;

@RestController
@RequestMapping("/api/partidas")
@RequiredArgsConstructor
public class PartidaController {
    private final PartidaService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PartidaResponse crear(@Valid @RequestBody CrearPartidaRequest dto) { return service.crear(dto); }

    @PostMapping("/{partidaId}/unirse")
    public PartidaResponse unirse(@PathVariable UUID partidaId,
                                  @Valid @RequestBody UnirsePartidaRequest dto) {
        return service.unirse(partidaId, dto);
    }

    @GetMapping("/{partidaId}")
    public PartidaResponse obtener(@PathVariable UUID partidaId) { return service.obtener(partidaId); }
}
