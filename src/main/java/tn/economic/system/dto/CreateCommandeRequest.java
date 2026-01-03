package tn.economic.system.dto;

public class CreateCommandeRequest {
  private Long annonceId;
  private Integer quantite;
  private String email;

  public Long getAnnonceId() { return annonceId; }
  public void setAnnonceId(Long annonceId) { this.annonceId = annonceId; }

  public Integer getQuantite() { return quantite; }
  public void setQuantite(Integer quantite) { this.quantite = quantite; }

  public String getEmail() { return email; }
  public void setEmail(String email) { this.email = email; }
}