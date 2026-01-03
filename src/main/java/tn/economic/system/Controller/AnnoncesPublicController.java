package tn.economic.system.Controller;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import tn.economic.system.dto.AnnonceProduitView;
import tn.economic.system.dto.AnnoncesDailyPoint;
import tn.economic.system.enums.ProduitType;
import tn.economic.system.repository.AnnoncePublicRepository;

import java.time.LocalDate;
import java.util.List;

@Path("/annonces")
@Produces(MediaType.APPLICATION_JSON)
public class AnnoncesPublicController {

  private final AnnoncePublicRepository repo = new AnnoncePublicRepository();
 // NOUVEAU: liste globale (pour Home)
  @GET
  public List<AnnonceProduitView> listAll() {
    return repo.listAllActive();
  }
  @GET
  @Path("/by-type")
  public List<AnnonceProduitView> byType(@QueryParam("type") String type) {
    if (type == null || type.isBlank()) {
      throw new BadRequestException("Paramètre query 'type' est obligatoire. Exemple: /api/annonces/by-type?type=HUILE");
    }

    // ProduitType.valueOf est sensible à la casse.
    // On accepte ici minuscule/majuscule + quelques variantes accentuées.
    ProduitType t;
    try {
      String normalized = type.trim().toUpperCase();
      // Normalisations simples pour éviter les 400 liés aux accents/espaces.
      normalized = normalized.replace('É', 'E').replace('È', 'E').replace('Ê', 'E');
      normalized = normalized.replace('À', 'A').replace('Â', 'A');
      normalized = normalized.replace('Î', 'I').replace('Ï', 'I');
      normalized = normalized.replace('Ô', 'O');
      normalized = normalized.replace('Ù', 'U').replace('Û', 'U').replace('Ü', 'U');
      normalized = normalized.replace('-', '_').replace(' ', '_');

      t = ProduitType.valueOf(normalized);
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(
          "type invalide: '" + type + "'. Valeurs possibles: " + java.util.Arrays.toString(ProduitType.values())
      );
    }

    return repo.listByProduitType(t);
  }
    @GET
  @Path("/daily")
  public List<AnnoncesDailyPoint> daily(
      @QueryParam("type") String type,
      @QueryParam("from") String from,
      @QueryParam("to") String to
  ) {
    if (type == null || type.isBlank()) throw new BadRequestException("type is required");
    if (from == null || to == null) throw new BadRequestException("from and to are required (YYYY-MM-DD)");

    LocalDate f = LocalDate.parse(from);
    LocalDate t = LocalDate.parse(to);

    return repo.dailyByTypeAndRegion(type, f, t);
  }

}