package tn.economic.system.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class CommandeView {
  public long idCommande;
  public long annonceId;
  public long produitId;
  public String typeProduit;
  public String clientEmail;
  public int qty;
  public BigDecimal prixUnitaire;
  public BigDecimal total;
  public String status;
  public Instant createdAt;
}