package tn.economic.system.Controller;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tn.economic.system.enums.ProduitType;
import tn.economic.system.models.Produits;
import tn.economic.system.services.ProduitService;

import java.util.List;
import java.util.Arrays;
@Path("/produit")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ProduitController {
    @POST
    public Response addproduit(Produits p) {
        try{
        ProduitService produitService= new ProduitService();
        produitService.AjouterProduit(p);
        return Response.status(Response.Status.CREATED).build();} catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProduits() {
        try {
            ProduitService produitService= new ProduitService();
            List<Produits> produits = produitService.listerProduits();
            return Response.ok(produits).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erreur serveur lors de la récupération des produits")
                    .build();
        }
    }
     @GET
  @Path("/types")
  public List<String> listTypes() {
    return Arrays.stream(ProduitType.values()).map(Enum::name).toList();
  }
}
