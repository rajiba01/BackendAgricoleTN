package tn.economic.system.models;

import java.math.BigDecimal;
import java.util.Date;

public class FournisseurAnnonce {
    private Long id;               // ID
    private Long userId;           // USER_ID
    private Long produitId;        // PRODUIT_ID
    private String titre;          // TITRE
    private String description;    // DESCRIPTION (CLOB)
    private Integer qualiteScore;  // QUALITE_SCORE
    private String qualiteVerdict; // QUALITE_VERDICT (HAUTE/MOYENNE/FAIBLE)
    private String imageUrl;       // IMAGE_URL
    private BigDecimal prixVente;  // PRIX_VENTE
    private Integer active;        // ACTIVE (0/1)
    private Date createdAt;        // CREATED_AT

    public FournisseurAnnonce() {}

    public FournisseurAnnonce(Long id, Long userId, Long produitId, String titre, String description,
                              Integer qualiteScore, String qualiteVerdict, String imageUrl,
                              BigDecimal prixVente, Integer active, Date createdAt) {
        this.id = id;
        this.userId = userId;
        this.produitId = produitId;
        this.titre = titre;
        this.description = description;
        this.qualiteScore = qualiteScore;
        this.qualiteVerdict = qualiteVerdict;
        this.imageUrl = imageUrl;
        this.prixVente = prixVente;
        this.active = active;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getQualiteScore() { return qualiteScore; }
    public void setQualiteScore(Integer qualiteScore) { this.qualiteScore = qualiteScore; }

    public String getQualiteVerdict() { return qualiteVerdict; }
    public void setQualiteVerdict(String qualiteVerdict) { this.qualiteVerdict = qualiteVerdict; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public BigDecimal getPrixVente() { return prixVente; }
    public void setPrixVente(BigDecimal prixVente) { this.prixVente = prixVente; }

    public Integer getActive() { return active; }
    public void setActive(Integer active) { this.active = active; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}