package sv.edu.udb.pokebattle.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Entity @Getter @Setter @NoArgsConstructor
public class PokemonEquipo {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY) private Equipo equipo;
    private Integer pokemonApiId; private String nombre; private String sprite; private Integer hpBase; private Integer ataqueBase; private Integer defensaBase; private Integer velocidadBase;
    @OneToMany(mappedBy = "pokemon", cascade = CascadeType.ALL, orphanRemoval = true) private List<MovimientoSeleccionado> movimientos = new ArrayList<>();
}
