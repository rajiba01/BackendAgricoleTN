package tn.economic.system.Controller;

import io.jsonwebtoken.Claims;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import tn.economic.system.dto.FournisseurAnnonceRequest;
import tn.economic.system.dto.StockAdjustRequest;
import tn.economic.system.enums.Role;
import tn.economic.system.models.FournisseurAnnonce;
import tn.economic.system.services.FournisseurService;
import tn.economic.system.util.JwtUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.List;
import tn.economic.system.dto.DeliverCommandeRequest;
import tn.economic.system.models.User;
import tn.economic.system.repository.CommandeRepository;
import tn.economic.system.repository.UserRep;
import tn.economic.system.services.EmailService;
import tn.economic.system.services.FactureTemplate;
import java.util.Set;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;

@Path("/fournisseur")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class FournisseurController {

    private static final Set<String> ALLOWED_VERDICTS = Set.of("HAUTE", "MOYENNE", "FAIBLE");

    private String requireFournisseurAndGetEmail(ContainerRequestContext ctx) {
        String auth = ctx.getHeaderString("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new WebApplicationException(
                    Response.status(Response.Status.UNAUTHORIZED).entity("Missing token").build()
            );
        }

        String token = auth.substring("Bearer ".length()).trim();

        Claims claims;
        try {
            claims = JwtUtil.validateToken(token);
        } catch (Exception e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.UNAUTHORIZED).entity("Invalid token").build()
            );
        }

        String role = (String) claims.get("role");
        if (role == null || !role.equals(Role.FOURNISSEUR.name())) {
            throw new WebApplicationException(
                    Response.status(Response.Status.FORBIDDEN).entity("Accès refusé: FOURNISSEUR uniquement").build()
            );
        }

        return claims.getSubject(); // email
    }

    @POST
    @Path("/annonce")
    public Response publierAnnonce(FournisseurAnnonceRequest req, @Context ContainerRequestContext ctx) {
        String email = requireFournisseurAndGetEmail(ctx);

        // ---- validations ----
        if (req == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Body invalide").build();
        }
        if (req.getProduitId() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("produitId manquant").build();
        }
        if (req.getTitre() == null || req.getTitre().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("titre manquant").build();
        }
        if (req.getPrixVente() <= 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("prixVente invalide").build();
        }
        if (req.getQualiteScore() != null) {
            int s = req.getQualiteScore();
            if (s < 0 || s > 100) {
                return Response.status(Response.Status.BAD_REQUEST).entity("qualiteScore doit être entre 0 et 100").build();
            }
        }
        if (req.getQualiteVerdict() != null) {
            String v = req.getQualiteVerdict().trim().toUpperCase();
            if (!ALLOWED_VERDICTS.contains(v)) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("qualiteVerdict invalide (HAUTE|MOYENNE|FAIBLE)")
                        .build();
            }
            // normalize to uppercase
            req.setQualiteVerdict(v);
        }

        // ---- create annonce ----
        try {
            FournisseurService service = new FournisseurService();
            Long annonceId = service.createAnnonce(email, req);

            return Response.status(Response.Status.CREATED)
                    .entity(java.util.Map.of("annonceId", annonceId))
                    .build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erreur publication annonce: " + e.getMessage())
                    .build();
        }
    }
        @GET
    @Path("/annonces")
    public Response mesAnnonces(@Context ContainerRequestContext ctx) {
        String email = requireFournisseurAndGetEmail(ctx);

        try {
            FournisseurService service = new FournisseurService();
            List<FournisseurAnnonce> annonces = service.listAnnonces(email);
            return Response.ok(annonces).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erreur listing annonces: " + e.getMessage())
                    .build();
        }
    }

    // (optionnel) stock adjust ici aussi si tu veux garder tout dans le même controller
    @POST
    @Path("/stock/adjust")
    public Response adjustStock(StockAdjustRequest req, @Context ContainerRequestContext ctx) {
        String email = requireFournisseurAndGetEmail(ctx);

        if (req == null || req.getProduitId() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("produitId manquant").build();
        }
        if (req.getDeltaQty() == 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("deltaQty ne peut pas être 0").build();
        }

        try {
            FournisseurService service = new FournisseurService();
            service.adjustStock(email, req.getProduitId(), req.getDeltaQty());
            return Response.ok(java.util.Map.of("status", "ok")).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("Erreur stock: " + e.getMessage()).build();
        }
    }

    // NOTE: Multipart endpoint removed temporarily.
    // Reason: if 'jersey-media-multipart' isn't on the runtime classpath, Jersey fails to load this
    // controller class at startup (NoClassDefFoundError), which breaks even unrelated endpoints like GET /annonces.
    // The safe upload endpoint remains available at POST /annonce/upload-raw.

    /**
     * Fallback endpoint: raw bytes upload (application/octet-stream) + query params.
     * Useful for simple clients that don't send multipart.
     */
    @POST
    @Path("/annonce/upload")
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response publierAnnonceAvecImageRaw(
            @Context ContainerRequestContext ctx,
            @jakarta.ws.rs.core.Context jakarta.ws.rs.core.HttpHeaders headers,
            InputStream fileInputStream,
            @QueryParam("type") String type,
            @QueryParam("titre") String titre,
            @QueryParam("description") String description,
            @QueryParam("prixVente") String prixVente,
            @QueryParam("qtyOnHand") String qtyOnHand
    ) {
        String email = requireFournisseurAndGetEmail(ctx);

        if (type == null || type.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("type manquant").build();
        }
        if (titre == null || titre.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("titre manquant").build();
        }
        if (prixVente == null || prixVente.isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("prixVente manquant").build();
        }
        if (fileInputStream == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("image (body) manquante").build();
        }

        final String imageUrl;
        try {
            String filename = headers != null ? headers.getHeaderString("X-Filename") : null;
            imageUrl = saveImage(fileInputStream, filename);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("Erreur upload image").build();
        }

        try {
            FournisseurService service = new FournisseurService();
            Long annonceId = service.createAnnonceWithTypeAndImage(
                    email,
                    type.trim(),
                    titre.trim(),
                    description,
                    new BigDecimal(prixVente.trim()),
                    (qtyOnHand == null || qtyOnHand.isBlank()) ? null : Integer.parseInt(qtyOnHand.trim()),
                    imageUrl
            );

            return Response.status(Response.Status.CREATED)
                    .entity(java.util.Map.of("annonceId", annonceId, "imageUrl", imageUrl))
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erreur publication annonce: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Upload annonce + image (multipart/form-data) WITHOUT jersey-media-multipart types.
     *
     * This avoids crashing the app when multipart libs aren't present at runtime.
     * It relies on Servlet multipart support (request.getParts()).
     */
    @POST
    @Path("/annonce/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response publierAnnonceAvecImageMultipart(
            @Context ContainerRequestContext ctx,
            @Context HttpServletRequest request
    ) {
        String email = requireFournisseurAndGetEmail(ctx);

        if (request == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Requête invalide").build();
        }

        try {
            // Ensure servlet multipart is available
            request.setCharacterEncoding("UTF-8");

            // Some clients use different field names for the uploaded file.
            jakarta.servlet.http.Part imagePart = request.getPart("image");
            if (imagePart == null) {
                imagePart = request.getPart("file");
            }
            String type = getFormField(request, "type");
            String titre = getFormField(request, "titre");
            String description = getFormField(request, "description");
            String prixVente = getFormField(request, "prixVente");
            String qtyOnHand = getFormField(request, "qtyOnHand");

            if (type == null || type.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST).entity("type manquant").build();
            }
            if (titre == null || titre.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).entity("titre manquant").build();
            }
            if (prixVente == null || prixVente.isBlank()) {
                return Response.status(Response.Status.BAD_REQUEST).entity("prixVente manquant").build();
            }
            if (imagePart == null) {
                String partNames;
                try {
                    partNames = request.getParts().stream()
                            .map(jakarta.servlet.http.Part::getName)
                            .distinct()
                            .sorted()
                            .reduce((a, b) -> a + "," + b)
                            .orElse("(aucun)");
                } catch (Exception ignored) {
                    partNames = "(inconnu)";
                }
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("image manquante. Utilise le champ form-data 'image' (ou 'file'). Champs reçus: " + partNames)
                        .build();
            }

            String originalFilename = imagePart.getSubmittedFileName();
            String imageUrl;
            try (InputStream in = imagePart.getInputStream()) {
                imageUrl = saveImage(in, originalFilename);
            }

            FournisseurService service = new FournisseurService();
            Long annonceId = service.createAnnonceWithTypeAndImage(
                    email,
                    type.trim(),
                    titre.trim(),
                    description,
                    new BigDecimal(prixVente.trim()),
                    (qtyOnHand == null || qtyOnHand.isBlank()) ? null : Integer.parseInt(qtyOnHand.trim()),
                    imageUrl
            );

            return Response.status(Response.Status.CREATED)
                    .entity(java.util.Map.of("annonceId", annonceId, "imageUrl", imageUrl))
                    .build();

        } catch (jakarta.servlet.ServletException se) {
            // Typically: multipart not configured on servlet container
            return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE)
                    .entity("Multipart non supporté par le serveur (Servlet multipart non configuré)")
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Erreur upload multipart: " + e.getMessage())
                    .build();
        }
    }

    private String getFormField(HttpServletRequest request, String fieldName) {
        try {
            jakarta.servlet.http.Part p = request.getPart(fieldName);
            if (p == null) return null;
            try (InputStream in = p.getInputStream()) {
                return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private String saveImage(InputStream fileInputStream, String originalFilename) throws Exception {
        // Filename is optional; clients may send it as header `X-Filename`.
        String original = (originalFilename == null || originalFilename.isBlank()) ? "image.jpg" : originalFilename;

        String ext = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) ext = original.substring(dot).toLowerCase();

        if (!(ext.equals(".png") || ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".webp"))) {
            throw new IllegalArgumentException("Format image non supporté (png|jpg|jpeg|webp)");
        }

        File dir = new File("uploads");
        if (!dir.exists()) dir.mkdirs();

        String filename = "annonce-" + UUID.randomUUID() + ext;
        File out = new File(dir, filename);

        try (FileOutputStream fos = new FileOutputStream(out)) {
            fileInputStream.transferTo(fos);
        }

        return "/uploads/" + filename;
    }
    private final UserRep userRep = new UserRep();
    private final CommandeRepository commandeRepo = new CommandeRepository();
    private final EmailService emailService = new EmailService();

    @POST
    @Path("/commandes/{id}/ship")
    public Response shipCommande(@PathParam("id") long idCommande,
                                 @Context ContainerRequestContext ctx) {
        String email = requireFournisseurAndGetEmail(ctx);
        User u = userRep.findByEmail(email);
        if (u == null) return Response.status(Response.Status.UNAUTHORIZED).entity("Utilisateur introuvable").build();

        try {
            CommandeRepository.ShipResult r = commandeRepo.markShippedAndGenerateOtp(idCommande, u.getId());

            // envoyer mail client (facture + OTP)
            String html = FactureTemplate.buildClientMail(r.otp, r.receipt);
            emailService.sendHtml(r.clientEmail, "Votre code de livraison (OTP) - Commande #" + idCommande, html);

            // response: ما نرجعوش OTP (خليه سري عند client)
            return Response.ok(java.util.Map.of("status", "SHIPPED", "message", "OTP envoyé au client")).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("Erreur ship: " + e.getMessage()).build();
        }
    }

    @POST
    @Path("/commandes/{id}/deliver")
    public Response deliverCommande(@PathParam("id") long idCommande,
                                    DeliverCommandeRequest req,
                                    @Context ContainerRequestContext ctx) {
        String email = requireFournisseurAndGetEmail(ctx);
        User u = userRep.findByEmail(email);
        if (u == null) return Response.status(Response.Status.UNAUTHORIZED).entity("Utilisateur introuvable").build();

        if (req == null || req.getOtp() == null || req.getOtp().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("otp manquant").build();
        }

        try {
            commandeRepo.confirmDeliveredByOtp(idCommande, u.getId(), req.getOtp().trim());
            return Response.ok(java.util.Map.of("status", "DELIVERED")).build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.BAD_REQUEST).entity("Erreur deliver: " + e.getMessage()).build();
        }}
            @GET
    @Path("/commandes")
    public Response mesCommandes(@Context ContainerRequestContext ctx) {
        String email = requireFournisseurAndGetEmail(ctx);
        User u = userRep.findByEmail(email);
        if (u == null) return Response.status(Response.Status.UNAUTHORIZED).entity("Utilisateur introuvable").build();

        try {
            return Response.ok(commandeRepo.listByFournisseur(u.getId())).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity("Erreur listing commandes").build();
        }
    }
}