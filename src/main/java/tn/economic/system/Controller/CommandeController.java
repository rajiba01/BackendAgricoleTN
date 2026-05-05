package tn.economic.system.Controller;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tn.economic.system.ModelException.DataAccessException;
import tn.economic.system.dto.CreateCommandeRequest;
import tn.economic.system.dto.AchatReceipt;
import tn.economic.system.repository.CommandeRepository;
import tn.economic.system.repository.CommandeRepository.TopProduct;
import java.util.List;

@Path("/commandes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CommandeController {

  private final CommandeRepository repo = new CommandeRepository();

  @POST
  public Response create(CreateCommandeRequest req) {
    if (req == null) return Response.status(Response.Status.BAD_REQUEST).entity("Body invalide").build();
    if (req.getAnnonceId() == null) return Response.status(Response.Status.BAD_REQUEST).entity("annonceId manquant").build();
    if (req.getQuantite() == null) return Response.status(Response.Status.BAD_REQUEST).entity("quantite manquante").build();
    if (req.getEmail() == null || req.getEmail().trim().isEmpty()) return Response.status(Response.Status.BAD_REQUEST).entity("email manquant").build();

    try {
      AchatReceipt receipt = repo.createCommande(req.getAnnonceId(), req.getEmail().trim(), req.getQuantite());
      return Response.status(Response.Status.CREATED).entity(receipt).build();
    } catch (DataAccessException e) {
      return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
    } catch (Exception e) {
      e.printStackTrace();
      return Response.serverError().entity("Erreur création commande").build();
    }
  }
@GET
  @Path("/top-products")

public List<TopProduct> topProducts(@QueryParam("limit") @DefaultValue("5") int limit) {
    return repo.getTopProducts(limit);
}
}