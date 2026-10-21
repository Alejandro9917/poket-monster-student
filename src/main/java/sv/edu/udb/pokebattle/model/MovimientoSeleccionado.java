package sv.edu.udb.pokebattle.model;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class MovimientoSeleccionado {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  private PokemonEquipo pokemon;

  private Integer movimientoApiId;
  private String nombre;
  private Integer poder;
  private Integer precisionMovimiento;
  private String tipo;
}
