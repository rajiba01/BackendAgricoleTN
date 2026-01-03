package tn.economic.system.dto;

import java.time.Instant;

public class AchatReceipt {
    private final long idAchat;
    private final long idProduit;
    private final String typeProduit;
    private final int quantite;
    private final float prixUnitaire;
    private final float total;
    private final Instant dateAchat;

    public AchatReceipt(long idAchat, long idProduit, String typeProduit, int quantite,
                        float prixUnitaire, float total, Instant dateAchat) {
        this.idAchat = idAchat;
        this.idProduit = idProduit;
        this.typeProduit = typeProduit;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
        this.total = total;
        this.dateAchat = dateAchat;
    }

    public long getIdAchat() { return idAchat; }
    public long getIdProduit() { return idProduit; }
    public String getTypeProduit() { return typeProduit; }
    public int getQuantite() { return quantite; }
    public float getPrixUnitaire() { return prixUnitaire; }
    public float getTotal() { return total; }
    public Instant getDateAchat() { return dateAchat; }
}