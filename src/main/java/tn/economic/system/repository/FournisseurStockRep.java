

package tn.economic.system.repository;

import tn.economic.system.ModelException.DataAccessException;
import tn.economic.system.connection.DBConnection;

import java.sql.*;

public class FournisseurStockRep implements FournisseurStockRepository {

    @Override
    public void adjust(Long userId, Long produitId, int deltaQty) throws DataAccessException {
        String sql =
                "MERGE INTO FOURNISSEUR_STOCK s " +
                "USING (SELECT ? AS USER_ID, ? AS PRODUIT_ID FROM dual) d " +
                "ON (s.USER_ID = d.USER_ID AND s.PRODUIT_ID = d.PRODUIT_ID) " +
                "WHEN MATCHED THEN UPDATE SET s.QTY_ON_HAND = s.QTY_ON_HAND + ?, s.UPDATED_AT = SYSTIMESTAMP " +
                "WHEN NOT MATCHED THEN INSERT (USER_ID, PRODUIT_ID, QTY_ON_HAND, UPDATED_AT) VALUES (?, ?, ?, SYSTIMESTAMP)";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setLong(2, produitId);
            ps.setInt(3, deltaQty);

            ps.setLong(4, userId);
            ps.setLong(5, produitId);
            ps.setInt(6, deltaQty);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Erreur adjust stock", e);
        }
    }

    @Override
    public Integer getQty(Long userId, Long produitId) throws DataAccessException {
        String sql = "SELECT QTY_ON_HAND FROM FOURNISSEUR_STOCK WHERE USER_ID = ? AND PRODUIT_ID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, userId);
            ps.setLong(2, produitId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt("QTY_ON_HAND");
                return null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Erreur getQty stock", e);
        }
    }
}