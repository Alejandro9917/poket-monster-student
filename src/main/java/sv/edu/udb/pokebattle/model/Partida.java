package sv.edu.udb.pokebattle.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Getter @Setter @NoArgsConstructor
public class Partida {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, unique = true, length = 8) private String codigoSala;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private EstadoPartida estado = EstadoPartida.ESPERANDO_JUGADOR;
    @ManyToOne(fetch = FetchType.LAZY) private Jugador turnoDe;
    @ManyToOne(fetch = FetchType.LAZY) private Jugador ganador;
    @Column(nullable = false) private LocalDateTime creadaEn = LocalDateTime.now();
    private LocalDateTime iniciadaEn;
}
