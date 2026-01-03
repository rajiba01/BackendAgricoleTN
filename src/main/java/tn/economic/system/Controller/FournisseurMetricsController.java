

package tn.economic.system.Controller;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tn.economic.system.repository.CommandeMetricsRepository;

import java.util.Map;

@Path("/public/fournisseurs")
@Produces(MediaType.APPLICATION_JSON)
public class FournisseurMetricsController {

  private final CommandeMetricsRepository repo = new CommandeMetricsRepository();

  @GET
  @Path("/{userId}/trust")
  public Response trust(@PathParam("userId") long userId) {
    var m = repo.trustByFournisseur(userId);
    return Response.ok(Map.of(
      "total", m.total,
      "delivered", m.delivered,
      "deliveredRate", m.deliveredRate,
      "trustScore", m.trustScore
    )).build();
  }
}