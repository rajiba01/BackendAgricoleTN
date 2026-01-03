package tn.economic.system.models;

import java.time.LocalDateTime;

public class Achat {
    private Long idAchat;
    private Long idProduit;
    private int quantite;
    private float prixUnitaire;
    private float total;
    private LocalDateTime dateAchat;

    // No-args constructor
    public Achat() {
    }

    // All-args constructor
    public Achat(Long idAchat, Long idProduit, int quantite, float prixUnitaire, float total, LocalDateTime dateAchat) {
        this.idAchat = idAchat;
        this.idProduit = idProduit;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
        this.total = total;
        this.dateAchat = dateAchat;
    }

    public Long getIdAchat() {
        return idAchat;
    }

    public void setIdAchat(Long idAchat) {
        this.idAchat = idAchat;
    }

    public Long getIdProduit() {
        return idProduit;
    }

    public void setIdProduit(Long idProduit) {
        this.idProduit = idProduit;
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
    }

    public float getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(float prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
    }

    public float getTotal() {
        return total;
    }

    public void setTotal(float total) {
        this.total = total;
    }

    public LocalDateTime getDateAchat() {
        return dateAchat;
    }

    public void setDateAchat(LocalDateTime dateAchat) {
        this.dateAchat = dateAchat;
    }
}