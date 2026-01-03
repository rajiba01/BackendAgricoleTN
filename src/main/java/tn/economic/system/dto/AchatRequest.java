package tn.economic.system.dto;

public class AchatRequest {
    private Long idProduit;
    private int quantite;
    private String email; // email du client

    public AchatRequest() {}

    public AchatRequest(Long idProduit, int quantite, String email) {
        this.idProduit = idProduit;
        this.quantite = quantite;
        this.email = email;
    }

    public Long getIdProduit() { return idProduit; }
    public void setIdProduit(Long idProduit) { this.idProduit = idProduit; }

    public int getQuantite() { return quantite; }
    public void setQuantite(int quantite) { this.quantite = quantite; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}