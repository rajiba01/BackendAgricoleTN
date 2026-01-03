package tn.economic.system.services;

import tn.economic.system.connection.DBConnection;
import tn.economic.system.models.User;
import tn.economic.system.repository.UserRep;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class FournisseurStockService {

    private final UserRep userRep = new UserRep();

    public void adjustStockByEmail(String email, Long produitId, int deltaQty) throws Exception {
        User u = userRep.findByEmail(email);
        if (u == null) throw new IllegalStateException("Utilisateur introuvable");

        String sql =
            "MERGE INTO FOURNISSEUR_STOCK s " +
            "USING (SELECT ? AS USER_ID, ? AS PRODUIT_ID FROM dual) d " +
            "ON (s.USER_ID = d.USER_ID AND s.PRODUIT_ID = d.PRODUIT_ID) " +
            "WHEN MATCHED THEN UPDATE SET s.QTY_ON_HAND = s.QTY_ON_HAND + ?, s.UPDATED_AT = SYSTIMESTAMP " +
            "WHEN NOT MATCHED THEN INSERT (USER_ID, PRODUIT_ID, QTY_ON_HAND, UPDATED_AT) VALUES (?, ?, ?, SYSTIMESTAMP)";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, u.getId());
            ps.setLong(2, produitId);
            ps.setInt(3, deltaQty);

            ps.setLong(4, u.getId());
            ps.setLong(5, produitId);
            ps.setInt(6, deltaQty);

            ps.executeUpdate();
        }
    }
}