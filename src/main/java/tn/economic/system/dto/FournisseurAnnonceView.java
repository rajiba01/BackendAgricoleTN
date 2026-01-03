
package tn.economic.system.dto;

import java.math.BigDecimal;
import java.util.Date;

public class FournisseurAnnonceView {
    private Long id;
    private Long produitId;
    private String titre;
    private String description;
    private Integer qualiteScore;
    private String qualiteVerdict;
    private String imageUrl;
    private BigDecimal prixVente;
    private Integer active;
    private Integer qtyOnHand; // NEW
    private Date createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public Integer getQtyOnHand() { return qtyOnHand; }
    public void setQtyOnHand(Integer qtyOnHand) { this.qtyOnHand = qtyOnHand; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}