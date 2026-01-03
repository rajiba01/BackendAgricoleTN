package tn.economic.system.Controller;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tn.economic.system.dto.AchatReceipt;
import tn.economic.system.dto.AchatRequest;
import tn.economic.system.services.AchatService;

@Path("/achat")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AchatController {

    @POST
    public Response acheter(AchatRequest request) {
        try {
            if (request == null || request.getIdProduit() == null || request.getEmail() == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Body invalide: idProduit/email manquant")
                        .build();
            }

            AchatService achatService = new AchatService();
            AchatReceipt receipt = achatService.acheterProduitEtEnvoyerFacture(
                    request.getIdProduit(),
                    request.getQuantite(),
                    request.getEmail()
            );

            return Response.status(Response.Status.CREATED).entity(receipt).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erreur serveur lors de l'achat: " + e.getMessage())
                    .build();
        }
    }
}