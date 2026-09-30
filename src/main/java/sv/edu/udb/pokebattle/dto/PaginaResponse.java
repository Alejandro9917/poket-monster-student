package sv.edu.udb.pokebattle.dto; import java.util.*; public record PaginaResponse<T>(List<T> contenido,int pagina,int tamano,long totalElementos,int totalPaginas) { }
