
package tn.economic.system.dto;

import java.math.BigDecimal;

public class AnnonceProduitView {
  public long id;
  public long produitId;
  public String produitType;   // ex: "BANANE"
  public String titre;
  public String description;
  public Integer qualiteScore;
  public String qualiteVerdict;
  public String imageUrl;
  public BigDecimal prixVente;
  public Integer qtyOnHand;
  
  public long userId;
}