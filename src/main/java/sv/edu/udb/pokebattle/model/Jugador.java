package sv.edu.udb.pokebattle.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity @Getter @Setter @NoArgsConstructor
public class Jugador {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, unique = true, length = 40) private String nombre;
    @Column(nullable = false) private LocalDateTime creadoEn = LocalDateTime.now();
}
