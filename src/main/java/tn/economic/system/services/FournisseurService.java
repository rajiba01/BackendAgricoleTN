

package tn.economic.system.services;

import tn.economic.system.models.FournisseurAnnonce;
import tn.economic.system.models.FournisseurProfile;
import tn.economic.system.models.User;
import tn.economic.system.enums.ProduitType;
import tn.economic.system.repository.*;
import tn.economic.system.dto.FournisseurAnnonceRequest;
import tn.economic.system.repository.UserRep;

import java.math.BigDecimal;
import java.util.List;

public class FournisseurService {

    private final UserRep userRep = new UserRep();

    private final FournisseurProfileRepository profileRepo = new FournisseurProfileRep();
    private final FournisseurAnnonceRepository annonceRepo = new FournisseurAnnonceRep();
    private final FournisseurStockRepository stockRepo = new FournisseurStockRep();

    private User requireUserByEmail(String email) {
        User u = userRep.findByEmail(email);
        if (u == null) throw new IllegalStateException("Utilisateur introuvable");
        return u;
    }

    public void upsertProfile(String email, FournisseurProfile profile) {
        User u = requireUserByEmail(email);
        profile.setUserId(u.getId());
        profileRepo.upsert(profile);
    }

    public Long createAnnonce(String email, FournisseurAnnonceRequest req) {
        User u = requireUserByEmail(email);

        FournisseurAnnonce a = new FournisseurAnnonce();
        a.setUserId(u.getId());
        a.setProduitId(req.getProduitId());
        a.setTitre(req.getTitre());
        a.setDescription(req.getDescription());
        a.setQualiteScore(req.getQualiteScore());
        a.setQualiteVerdict(req.getQualiteVerdict());
        a.setImageUrl(req.getImageUrl());
        a.setPrixVente(BigDecimal.valueOf(req.getPrixVente()));
        a.setActive(1);

        return annonceRepo.create(a);
    }

    public void adjustStock(String email, Long produitId, int deltaQty) {
        User u = requireUserByEmail(email);
        stockRepo.adjust(u.getId(), produitId, deltaQty);
    }

    public List<FournisseurAnnonce> listAnnonces(String email) {
        User u = requireUserByEmail(email);
        return annonceRepo.listByUserId(u.getId());
    }

    /**
     * Convenience method used by multipart upload controller:
     * - resolve produitId from ProduitType string (must match enum)
     * - save annonce with imageUrl
     * - optionally adjust stock if qtyOnHand provided
     */
    public Long createAnnonceWithTypeAndImage(
            String email,
            String type,
            String titre,
            String description,
            BigDecimal prixVente,
            Integer qtyOnHand,
            String imageUrl
    ) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type manquant");
        }
        if (titre == null || titre.isBlank()) {
            throw new IllegalArgumentException("titre manquant");
        }
        if (prixVente == null || prixVente.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("prixVente invalide");
        }

        // Parse ProduitType (case-insensitive)
        ProduitType produitType = ProduitType.valueOf(type.trim().toUpperCase());

        User u = requireUserByEmail(email);

        // Resolve produitId from DB by type via ProduitRepository
        ProduitRepository produitRepo = new ProduitRepository();
        Long produitId = produitRepo.findIdByType(produitType);
        if (produitId == null) {
            throw new IllegalStateException("Produit introuvable pour type=" + produitType);
        }

        FournisseurAnnonce a = new FournisseurAnnonce();
        a.setUserId(u.getId());
        a.setProduitId(produitId);
        a.setTitre(titre);
        a.setDescription(description);
        a.setQualiteScore(null);
        a.setQualiteVerdict(null);
        a.setImageUrl(imageUrl);
        a.setPrixVente(prixVente);
        a.setActive(1);

        Long annonceId = annonceRepo.create(a);

        if (qtyOnHand != null) {
            // Set absolute qty by adjusting current to desired
            Integer current = stockRepo.getQty(u.getId(), produitId);
            int delta = qtyOnHand - (current == null ? 0 : current);
            if (delta != 0) {
                stockRepo.adjust(u.getId(), produitId, delta);
            }
        }

        return annonceId;
    }
}