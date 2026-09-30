package sv.edu.udb.pokebattle.model;
import jakarta.persistence.*; import lombok.*; import java.util.UUID;
@Entity @Getter @Setter @NoArgsConstructor @Table(uniqueConstraints=@UniqueConstraint(columnNames={"partida_id","jugador_id"}))
public class ParticipantePartida { @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id; @ManyToOne(optional=false) @JoinColumn(name="partida_id") private Partida partida; @ManyToOne(optional=false) @JoinColumn(name="jugador_id") private Jugador jugador; @ManyToOne(optional=false) private Equipo equipo; private boolean confirmado; }
