package sv.edu.udb.pokebattle.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import sv.edu.udb.pokebattle.dto.CrearEquipoRequest;
import sv.edu.udb.pokebattle.dto.EquipoResponse;
import sv.edu.udb.pokebattle.service.EquipoService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/jugadores/{jugadorId}/equipos")
public class EquipoController {
    private final EquipoService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquipoResponse crear(@PathVariable UUID jugadorId,
                                @Valid @RequestBody CrearEquipoRequest dto) {
        return service.crear(jugadorId, dto);
    }

    @GetMapping
    public List<EquipoResponse> listar(@PathVariable UUID jugadorId) {
        return service.listar(jugadorId);
    }
}
