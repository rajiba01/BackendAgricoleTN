package tn.economic.system.dto;


public class StockAdjustRequest {
    private Long produitId;
    private int deltaQty;

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public int getDeltaQty() { return deltaQty; }
    public void setDeltaQty(int deltaQty) { this.deltaQty = deltaQty; }
}