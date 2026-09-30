package sv.edu.udb.pokebattle.model;
import jakarta.persistence.*; import lombok.*; import java.util.UUID;
@Entity @Getter @Setter @NoArgsConstructor public class PokemonBatalla { @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id; @ManyToOne(optional=false) private ParticipantePartida participante; @ManyToOne(optional=false) private PokemonEquipo pokemonEquipo; private Integer vidaActual; private boolean activo; private boolean debilitado; }
