package tn.economic.system.repository;

import tn.economic.system.ModelException.DataAccessException;
import tn.economic.system.connection.DBConnection;
import tn.economic.system.dto.AchatReceipt;
import tn.economic.system.dto.CommandeView;

import java.math.BigDecimal;
import java.sql.*;
import java.time.Instant;
import java.util.List;

public class CommandeRepository {

  public AchatReceipt createCommande(long annonceId, String clientEmail, int qty) {
    String selectAnnonceSql = """
      SELECT a.USER_ID AS FOURNISSEUR_USER_ID,
             a.PRODUIT_ID,
             a.PRIX_VENTE,
             p.TYPE AS TYPE_PRODUIT,
             s.QTY_ON_HAND
      FROM FOURNISSEUR_ANNONCE a
      JOIN PRODUIT p ON p.IDPRODUIT = a.PRODUIT_ID
      JOIN FOURNISSEUR_STOCK s ON s.USER_ID = a.USER_ID AND s.PRODUIT_ID = a.PRODUIT_ID
      WHERE a.ID = ? AND a.ACTIVE = 1
      FOR UPDATE
    """;

    String insertCommandeSql = """
      INSERT INTO COMMANDE
        (ANNONCE_ID, FOURNISSEUR_USER_ID, PRODUIT_ID, CLIENT_EMAIL, QTY, PRIX_UNITAIRE, TOTAL, STATUS)
      VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING')
    """;

    String updateStockSql = """
      UPDATE FOURNISSEUR_STOCK
      SET QTY_ON_HAND = QTY_ON_HAND - ?
      WHERE USER_ID = ? AND PRODUIT_ID = ?
    """;

    Connection c = null;
    try {
      c = DBConnection.getConnection();
      c.setAutoCommit(false);

      long fournisseurUserId;
      long produitId;
      String typeProduit;
      BigDecimal prixUnitaire;
      int stock;

      // 1) lock annonce+stock
      try (PreparedStatement ps = c.prepareStatement(selectAnnonceSql)) {
        ps.setLong(1, annonceId);
        try (ResultSet rs = ps.executeQuery()) {
          if (!rs.next()) throw new DataAccessException("Annonce introuvable ou stock non défini");
          fournisseurUserId = rs.getLong("FOURNISSEUR_USER_ID");
          produitId = rs.getLong("PRODUIT_ID");
          typeProduit = rs.getString("TYPE_PRODUIT");
          prixUnitaire = rs.getBigDecimal("PRIX_VENTE");
          stock = rs.getInt("QTY_ON_HAND");
        }
      }

      if (qty <= 0) throw new DataAccessException("Quantité invalide");
      if (stock < qty) throw new DataAccessException("Stock insuffisant");

      BigDecimal total = prixUnitaire.multiply(BigDecimal.valueOf(qty));

      // 2) insert commande
      long idCommande;
      try (PreparedStatement ps = c.prepareStatement(insertCommandeSql, new String[]{"ID_COMMANDE"})) {
        ps.setLong(1, annonceId);
        ps.setLong(2, fournisseurUserId);
        ps.setLong(3, produitId);
        ps.setString(4, clientEmail);
        ps.setInt(5, qty);
        ps.setBigDecimal(6, prixUnitaire);
        ps.setBigDecimal(7, total);
        ps.executeUpdate();

        try (ResultSet keys = ps.getGeneratedKeys()) {
          if (keys != null && keys.next()) idCommande = keys.getLong(1);
          else throw new SQLException("Impossible de récupérer ID_COMMANDE");
        }
      }

      // 3) decrement stock
      try (PreparedStatement ps = c.prepareStatement(updateStockSql)) {
        ps.setInt(1, qty);
        ps.setLong(2, fournisseurUserId);
        ps.setLong(3, produitId);
        int updated = ps.executeUpdate();
        if (updated != 1) throw new DataAccessException("Erreur mise à jour stock");
      }

      c.commit();

      // نرجّعو receipt (نستعمل AchatReceipt اللي عندك)
      return new AchatReceipt(
        idCommande,
        produitId,
        typeProduit,
        qty,
        prixUnitaire.floatValue(),
        total.floatValue(),
        Instant.now()
      );

    } catch (SQLException e) {
      if (c != null) try { c.rollback(); } catch (SQLException ignored) {}
      throw new DataAccessException("Erreur createCommande: " + e.getMessage(), e);
    } finally {
      if (c != null) try { c.setAutoCommit(true); c.close(); } catch (SQLException ignored) {}
    }
  }
  public static class ShipResult {
    public final String clientEmail;
    public final String otp;
    public final AchatReceipt receipt;

    public ShipResult(String clientEmail, String otp, AchatReceipt receipt) {
      this.clientEmail = clientEmail;
      this.otp = otp;
      this.receipt = receipt;
    }
  }

  public ShipResult markShippedAndGenerateOtp(long commandeId, long fournisseurUserId) {
    // 1) read needed data (only if owner + status ok)
    String selectSql = """
      SELECT c.CLIENT_EMAIL,
             c.PRODUIT_ID,
             p.TYPE AS TYPE_PRODUIT,
             c.QTY,
             c.PRIX_UNITAIRE,
             c.TOTAL
      FROM COMMANDE c
      JOIN PRODUIT p ON p.IDPRODUIT = c.PRODUIT_ID
      WHERE c.ID_COMMANDE = ?
        AND c.FOURNISSEUR_USER_ID = ?
        AND c.STATUS = 'PENDING'
      FOR UPDATE
    """;

    String updateSql = """
      UPDATE COMMANDE
      SET STATUS='SHIPPED',
          OTP_CODE=?,
          OTP_EXPIRES_AT=SYSTIMESTAMP + INTERVAL '48' HOUR,
          SHIPPED_AT=SYSTIMESTAMP
      WHERE ID_COMMANDE=? AND FOURNISSEUR_USER_ID=? AND STATUS='PENDING'
    """;

    Connection c = null;
    try {
      c = DBConnection.getConnection();
      c.setAutoCommit(false);

      String clientEmail;
      long produitId;
      String typeProduit;
      int qty;
      float prixUnitaire;
      float total;

      try (PreparedStatement ps = c.prepareStatement(selectSql)) {
        ps.setLong(1, commandeId);
        ps.setLong(2, fournisseurUserId);
        try (ResultSet rs = ps.executeQuery()) {
          if (!rs.next()) throw new DataAccessException("Commande introuvable ou déjà expédiée");
          clientEmail = rs.getString("CLIENT_EMAIL");
          produitId = rs.getLong("PRODUIT_ID");
          typeProduit = rs.getString("TYPE_PRODUIT");
          qty = rs.getInt("QTY");
          prixUnitaire = rs.getBigDecimal("PRIX_UNITAIRE").floatValue();
          total = rs.getBigDecimal("TOTAL").floatValue();
        }
      }

      String otp = String.format("%06d", (int) (Math.random() * 1_000_000));

      try (PreparedStatement ps = c.prepareStatement(updateSql)) {
        ps.setString(1, otp);
        ps.setLong(2, commandeId);
        ps.setLong(3, fournisseurUserId);
        int updated = ps.executeUpdate();
        if (updated != 1) throw new DataAccessException("Échec SHIP (status invalide)");
      }

      c.commit();

      AchatReceipt receipt = new AchatReceipt(
        commandeId,
        produitId,
        typeProduit,
        qty,
        prixUnitaire,
        total,
        Instant.now()
      );

      return new ShipResult(clientEmail, otp, receipt);

    } catch (SQLException e) {
      if (c != null) try { c.rollback(); } catch (SQLException ignored) {}
      throw new DataAccessException("Erreur ship commande", e);
    } finally {
      if (c != null) try { c.setAutoCommit(true); c.close(); } catch (SQLException ignored) {}
    }
  }

  public void confirmDeliveredByOtp(long commandeId, long fournisseurUserId, String otp) {
    String sql = """
      UPDATE COMMANDE
      SET STATUS='DELIVERED',
          DELIVERED_AT=SYSTIMESTAMP
      WHERE ID_COMMANDE=? AND FOURNISSEUR_USER_ID=?
        AND STATUS='SHIPPED'
        AND OTP_CODE=?
        AND OTP_EXPIRES_AT > SYSTIMESTAMP
    """;

    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setLong(1, commandeId);
      ps.setLong(2, fournisseurUserId);
      ps.setString(3, otp);
      int updated = ps.executeUpdate();
      if (updated != 1) {
        throw new DataAccessException("OTP invalide/expiré أو commande موش SHIPPED");
      }
    } catch (SQLException e) {
      throw new DataAccessException("Erreur confirm delivered", e);
    }
  }
    public List<CommandeView> listByFournisseur(long fournisseurUserId) {
    String sql = """
      SELECT c.ID_COMMANDE,
             c.ANNONCE_ID,
             c.PRODUIT_ID,
             p.TYPE AS TYPE_PRODUIT,
             c.CLIENT_EMAIL,
             c.QTY,
             c.PRIX_UNITAIRE,
             c.TOTAL,
             c.STATUS,
             c.CREATED_AT
      FROM COMMANDE c
      JOIN PRODUIT p ON p.IDPRODUIT = c.PRODUIT_ID
      WHERE c.FOURNISSEUR_USER_ID = ?
      ORDER BY c.CREATED_AT DESC
    """;
    

  List<CommandeView> commandes = new java.util.ArrayList<>();
    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {

      ps.setLong(1, fournisseurUserId);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          CommandeView v = new CommandeView();
          v.idCommande = rs.getLong("ID_COMMANDE");
          v.annonceId = rs.getLong("ANNONCE_ID");
          v.produitId = rs.getLong("PRODUIT_ID");
          v.typeProduit = rs.getString("TYPE_PRODUIT");
          v.clientEmail = rs.getString("CLIENT_EMAIL");
          v.qty = rs.getInt("QTY");
          v.prixUnitaire = rs.getBigDecimal("PRIX_UNITAIRE");
          v.total = rs.getBigDecimal("TOTAL");
          v.status = rs.getString("STATUS");
          Timestamp ts = rs.getTimestamp("CREATED_AT");
          v.createdAt = ts == null ? null : ts.toInstant();
          commandes.add(v);
        }
      }

      return commandes;

    } catch (SQLException e) {
      throw new DataAccessException("Erreur list commandes fournisseur", e);
    }

  }
  public static class TopProduct {
    public String produitType;
    public int nbCommandes;

    public TopProduct(String produitType, int nbCommandes) {
        this.produitType = produitType;
        this.nbCommandes = nbCommandes;
    }
}
public List<TopProduct> getTopProducts(int limit) {
    String sql = "SELECT p.TYPE, COUNT(c.PRODUIT_ID) AS nb_commandes\r\n" + //
            "FROM commande c\r\n" + //
            "JOIN produit p ON c.PRODUIT_ID = p.IDPRODUIT\r\n" + //
            "GROUP BY p.TYPE\r\n" + //
            "ORDER BY nb_commandes DESC\r\n" + //
            "FETCH FIRST ? ROWS ONLY\r\n" + //
            "";
    List<TopProduct> topProducts = new java.util.ArrayList<>();
    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {

        ps.setInt(1, limit);

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
              String produitType = rs.getString("TYPE"); // colonne SQL s'appelle TYPE
int nbCommandes = rs.getInt("nb_commandes");
topProducts.add(new TopProduct(produitType, nbCommandes));

            }
        }

        return topProducts;

    } catch (SQLException e) {
        throw new DataAccessException("Erreur top produits", e);
    }
}
}