package sv.edu.udb.pokebattle.controller;
import java.util.*; import lombok.RequiredArgsConstructor; import org.springframework.web.bind.annotation.*; import sv.edu.udb.pokebattle.dto.JugadorResponse; import sv.edu.udb.pokebattle.service.JugadorService;
@RestController @RequestMapping("/api/jugadores") @RequiredArgsConstructor public class JugadorController { private final JugadorService service; @GetMapping public List<JugadorResponse> listar(){return service.listar();}@GetMapping("/{id}") public JugadorResponse obtener(@PathVariable UUID id){return service.obtener(id);} }
