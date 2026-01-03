package tn.economic.system.models;

import tn.economic.system.enums.ProduitType;

public class Produits {
    private Long idProduit ;
    private ProduitType type;
    private  int quantite;
    private float prix;
    // Constructor without parameters
    public Produits() {
    }

    // Constructor with parameters
    public Produits(Long idProduit, ProduitType type, int quantite, float prix) {
        this.idProduit = idProduit;
        this.type = type;
        this.quantite = quantite;
        this.prix = prix;
    }

    // Getters and Setters
    public Long getIdProduit() {
        return idProduit;
    }

    public void setIdProduit(Long idProduit) {
        this.idProduit = idProduit;
    }

    public ProduitType getType() {
        return type;
    }

    public void setType(ProduitType type) {
        this.type = type;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public float getPrix() {
        return prix;
    }

    public void setPrix(float prix) {
        this.prix = prix;
    }
}
