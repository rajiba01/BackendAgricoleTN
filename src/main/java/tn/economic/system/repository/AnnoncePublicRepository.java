package tn.economic.system.repository;

import tn.economic.system.ModelException.DataAccessException;
import tn.economic.system.connection.DBConnection;
import tn.economic.system.dto.AnnonceProduitView;
import tn.economic.system.dto.AnnoncesDailyPoint;
import tn.economic.system.enums.ProduitType;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AnnoncePublicRepository {

  public List<AnnonceProduitView> listAllActive() {
    String sql = """
      SELECT a.ID,
             a.USER_ID,
             a.PRODUIT_ID,
             p.TYPE AS PRODUIT_TYPE,
             a.TITRE,
             a.DESCRIPTION,
             a.QUALITE_SCORE,
             a.QUALITE_VERDICT,
             a.IMAGE_URL,
             a.PRIX_VENTE,
             s.QTY_ON_HAND
      FROM FOURNISSEUR_ANNONCE a
      JOIN PRODUIT p ON p.IDPRODUIT = a.PRODUIT_ID
      LEFT JOIN FOURNISSEUR_STOCK s
        ON s.USER_ID = a.USER_ID AND s.PRODUIT_ID = a.PRODUIT_ID
      WHERE a.ACTIVE = 1
      ORDER BY a.CREATED_AT DESC
    """;

    List<AnnonceProduitView> out = new ArrayList<>();

    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {

      while (rs.next()) {
        AnnonceProduitView v = new AnnonceProduitView();
        v.id = rs.getLong("ID");
        v.produitId = rs.getLong("PRODUIT_ID");
        v.produitType = rs.getString("PRODUIT_TYPE");
        v.titre = rs.getString("TITRE");
        v.description = rs.getString("DESCRIPTION");

        Object qs = rs.getObject("QUALITE_SCORE");
        v.qualiteScore = (qs == null) ? null : ((Number) qs).intValue();

        v.qualiteVerdict = rs.getString("QUALITE_VERDICT");
        v.imageUrl = rs.getString("IMAGE_URL");
        v.prixVente = rs.getBigDecimal("PRIX_VENTE");

        Object qty = rs.getObject("QTY_ON_HAND");
        v.qtyOnHand = (qty == null) ? null : ((Number) qty).intValue();
  v.userId = rs.getLong("USER_ID");
        out.add(v);
      }

      return out;

    } catch (SQLException e) {
      throw new DataAccessException("Erreur list annonces actives", e);
    }
  }

  public List<AnnonceProduitView> listByProduitType(ProduitType t) {
  String sql = """
   SELECT
  a.ID,
  a.USER_ID,
  a.PRODUIT_ID,
  p.TYPE AS PRODUIT_TYPE,
  a.TITRE,
  a.DESCRIPTION,
  a.QUALITE_SCORE,
  a.QUALITE_VERDICT,
  a.IMAGE_URL,
  a.PRIX_VENTE,
  s.QTY_ON_HAND
FROM FOURNISSEUR_ANNONCE a
JOIN PRODUIT p ON p.IDPRODUIT = a.PRODUIT_ID
LEFT JOIN FOURNISSEUR_STOCK s
  ON s.USER_ID = a.USER_ID AND s.PRODUIT_ID = a.PRODUIT_ID
WHERE a.ACTIVE = 1
  AND p.TYPE = ?
ORDER BY a.ID DESC
  """;

  List<AnnonceProduitView> out = new ArrayList<>();

  try (Connection c = DBConnection.getConnection();
       PreparedStatement ps = c.prepareStatement(sql)) {

    ps.setString(1, t.name()); 

    try (ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        AnnonceProduitView v = new AnnonceProduitView();
        v.id = rs.getLong("ID");
        v.userId = rs.getLong("USER_ID");
        v.produitId = rs.getLong("PRODUIT_ID");
        v.produitType = rs.getString("PRODUIT_TYPE"); 
        v.titre = rs.getString("TITRE");
        v.description = rs.getString("DESCRIPTION");
        Object qs = rs.getObject("QUALITE_SCORE");
        v.qualiteScore = (qs == null) ? null : ((Number) qs).intValue();
        v.qualiteVerdict = rs.getString("QUALITE_VERDICT");
        v.imageUrl = rs.getString("IMAGE_URL");
        v.prixVente = rs.getBigDecimal("PRIX_VENTE");
        Object qty = rs.getObject("QTY_ON_HAND");
        v.qtyOnHand = (qty == null) ? null : ((Number) qty).intValue();
        out.add(v);
      }
    }

    return out;

  } catch (SQLException e) {
    throw new DataAccessException("Erreur listByProduitType", e);
  }
}
public List<AnnoncesDailyPoint> dailyByTypeAndRegion(String type, LocalDate from, LocalDate to) {
    String sql = """
      SELECT
        TRUNC(a.CREATED_AT) AS DAY_DATE,
        LOWER(TRIM(u.LOCALISATION)) AS REGION,
        COUNT(*) AS ANNONCE_COUNT,
        AVG(a.PRIX_VENTE) AS ANNONCE_PRICE_MEAN,
        MIN(a.PRIX_VENTE) AS ANNONCE_PRICE_MIN,
        MAX(a.PRIX_VENTE) AS ANNONCE_PRICE_MAX,
        AVG(a.QUALITE_SCORE) AS ANNONCE_QUALITY_MEAN,
        SUM(NVL(s.QTY_ON_HAND, 0)) AS ANNONCE_STOCK_SUM
      FROM FOURNISSEUR_ANNONCE a
      JOIN PRODUIT p ON p.IDPRODUIT = a.PRODUIT_ID
      JOIN USERS u ON u.ID = a.USER_ID
      LEFT JOIN FOURNISSEUR_STOCK s
        ON s.USER_ID = a.USER_ID AND s.PRODUIT_ID = a.PRODUIT_ID
      WHERE a.ACTIVE = 1
        AND p.TYPE = ?
        AND a.CREATED_AT >= ?
        AND a.CREATED_AT <  ? + 1
        AND LOWER(TRIM(u.LOCALISATION)) IN ('sfax','sahel','centre','nord','sud')
      GROUP BY TRUNC(a.CREATED_AT), LOWER(TRIM(u.LOCALISATION))
      ORDER BY DAY_DATE, REGION
    """;

    List<AnnoncesDailyPoint> out = new ArrayList<>();
    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {

        ps.setString(1, type);
        ps.setDate(2, java.sql.Date.valueOf(from));
        ps.setDate(3, java.sql.Date.valueOf(to));

        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                AnnoncesDailyPoint p = new AnnoncesDailyPoint();
                java.sql.Date d = rs.getDate("DAY_DATE");
                p.date = (d == null) ? null : d.toLocalDate().toString();
                p.region = rs.getString("REGION");
                p.annonceCount = rs.getInt("ANNONCE_COUNT");

                p.annoncePriceMean = rs.getBigDecimal("ANNONCE_PRICE_MEAN");
                p.annoncePriceMin  = rs.getBigDecimal("ANNONCE_PRICE_MIN");
                p.annoncePriceMax  = rs.getBigDecimal("ANNONCE_PRICE_MAX");

                p.annonceQualityMean = rs.getBigDecimal("ANNONCE_QUALITY_MEAN");
                p.annonceStockSum    = rs.getBigDecimal("ANNONCE_STOCK_SUM");

                out.add(p);
            }
        }
        return out;

    } catch (SQLException e) {
        throw new DataAccessException("Erreur daily aggregation annonces", e);
    }}
}