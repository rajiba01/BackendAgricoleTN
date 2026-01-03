
package tn.economic.system.dto;

public class FournisseurAnnonceRequest {
    private Long produitId;          // FK PRODUITS.ID
    private String titre;            // TITRE
    private String description;      // DESCRIPTION
    private Integer qualiteScore;    // QUALITE_SCORE (0..100) optionnel
    private String qualiteVerdict;   // QUALITE_VERDICT optionnel: "HAUTE"|"MOYENNE"|"FAIBLE"
    private String imageUrl;         // IMAGE_URL optionnel
    private float prixVente;         // PRIX_VENTE

    public FournisseurAnnonceRequest() {}

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

    public float getPrixVente() { return prixVente; }
    public void setPrixVente(float prixVente) { this.prixVente = prixVente; }
}