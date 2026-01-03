package tn.economic.system.models;

import java.util.Date;

public class FournisseurStock {
    private Long id;          // ID
    private Long userId;      // USER_ID
    private Long produitId;   // PRODUIT_ID
    private int qtyOnHand;    // QTY_ON_HAND
    private Date updatedAt;   // UPDATED_AT

    public FournisseurStock() {}

    public FournisseurStock(Long id, Long userId, Long produitId, int qtyOnHand, Date updatedAt) {
        this.id = id;
        this.userId = userId;
        this.produitId = produitId;
        this.qtyOnHand = qtyOnHand;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public int getQtyOnHand() { return qtyOnHand; }
    public void setQtyOnHand(int qtyOnHand) { this.qtyOnHand = qtyOnHand; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}