package sv.edu.udb.pokebattle.dto;

public record AuthResponse(
    String accessToken, String tokenType, long expiresIn, JugadorResponse jugador) {}
