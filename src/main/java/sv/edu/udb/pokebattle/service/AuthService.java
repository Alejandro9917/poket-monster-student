package sv.edu.udb.pokebattle.service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sv.edu.udb.pokebattle.dto.*;
import sv.edu.udb.pokebattle.exception.*;
import sv.edu.udb.pokebattle.model.Jugador;
import sv.edu.udb.pokebattle.repository.JugadorRepository;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
  private final JugadorRepository jugadores;
  private final PasswordEncoder passwords;
  private final JwtEncoder encoder;

  @Value("${security.jwt.issuer}")
  private String issuer;

  @Value("${security.jwt.expiration-minutes}")
  private long expiration;

  public AuthResponse registrar(RegistroRequest d) {
    String u = normalizar(d.usuario());
    if (jugadores.existsByUsuarioIgnoreCase(u))
      throw new ConflictoException("El usuario ya está registrado");
    Jugador j = new Jugador();
    j.setUsuario(u);
    j.setNombre(u);
    j.setPasswordHash(passwords.encode(d.password()));
    return respuesta(jugadores.save(j));
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest d) {
    Jugador j =
        jugadores
            .findByUsuarioIgnoreCase(normalizar(d.usuario()))
            .orElseThrow(
                () -> new CredencialesInvalidasException("Usuario o contraseña incorrectos"));
    if (!passwords.matches(d.password(), j.getPasswordHash()))
      throw new CredencialesInvalidasException("Usuario o contraseña incorrectos");
    return respuesta(j);
  }

  private AuthResponse respuesta(Jugador j) {
    Instant now = Instant.now(), expires = now.plus(expiration, ChronoUnit.MINUTES);
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(issuer)
            .issuedAt(now)
            .expiresAt(expires)
            .subject(j.getId().toString())
            .claim("usuario", j.getUsuario())
            .build();
    String token =
        encoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    return new AuthResponse(token, "Bearer", expiration * 60, JugadorResponse.desde(j));
  }

  private String normalizar(String s) {
    return s.trim().toLowerCase(Locale.ROOT);
  }
}
