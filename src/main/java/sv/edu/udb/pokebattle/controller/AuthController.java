package sv.edu.udb.pokebattle.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import sv.edu.udb.pokebattle.dto.*;
import sv.edu.udb.pokebattle.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
  private final AuthService service;

  @PostMapping("/registro")
  @ResponseStatus(HttpStatus.CREATED)
  public AuthResponse registro(@Valid @RequestBody RegistroRequest d) {
    return service.registrar(d);
  }

  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest d) {
    return service.login(d);
  }
}
