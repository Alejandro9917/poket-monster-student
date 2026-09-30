package sv.edu.udb.pokebattle.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Entity @Getter @Setter @NoArgsConstructor
public class Equipo {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @Column(nullable = false, length = 50) private String nombre;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Jugador jugador;
    @OneToMany(mappedBy = "equipo", cascade = CascadeType.ALL, orphanRemoval = true) private List<PokemonEquipo> pokemon = new ArrayList<>();
}
