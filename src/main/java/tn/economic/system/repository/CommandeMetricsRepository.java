package tn.economic.system.repository;

import tn.economic.system.ModelException.DataAccessException;
import tn.economic.system.connection.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CommandeMetricsRepository {

  public static class TrustMetrics {
    public final int total;
    public final int delivered;
    public final double deliveredRate;
    public final int trustScore;

    public TrustMetrics(int total, int delivered) {
      this.total = total;
      this.delivered = delivered;
      this.deliveredRate = total == 0 ? 0.0 : (double) delivered / (double) total;
      this.trustScore = (int) Math.round(this.deliveredRate * 100.0);
    }
  }

  public TrustMetrics trustByFournisseur(long fournisseurUserId) {
    String sql = """
      SELECT
        COUNT(*) AS TOTAL,
        SUM(CASE WHEN STATUS='DELIVERED' THEN 1 ELSE 0 END) AS DELIVERED
      FROM COMMANDE
      WHERE FOURNISSEUR_USER_ID = ?
    """;

    try (Connection c = DBConnection.getConnection();
         PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setLong(1, fournisseurUserId);
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        int total = rs.getInt("TOTAL");
        int delivered = rs.getInt("DELIVERED");
        return new TrustMetrics(total, delivered);
      }
    } catch (SQLException e) {
      throw new DataAccessException("Erreur trust metrics", e);
    }
  }
}