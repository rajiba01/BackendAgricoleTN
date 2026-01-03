

package tn.economic.system.repository;

import tn.economic.system.ModelException.DataAccessException;
import tn.economic.system.connection.DBConnection;
import tn.economic.system.models.FournisseurProfile;

import java.sql.*;

public class FournisseurProfileRep implements FournisseurProfileRepository {

    @Override
    public void upsert(FournisseurProfile p) throws DataAccessException {
        // MERGE: si profile existe -> update, sinon insert
        String sql =
                "MERGE INTO FOURNISSEUR_PROFILE fp " +
                "USING (SELECT ? AS USER_ID FROM dual) d " +
                "ON (fp.USER_ID = d.USER_ID) " +
                "WHEN MATCHED THEN UPDATE SET fp.SOCIETE = ?, fp.TEL = ?, fp.ADRESSE = ? " +
                "WHEN NOT MATCHED THEN INSERT (USER_ID, SOCIETE, TEL, ADRESSE) VALUES (?, ?, ?, ?)";

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, p.getUserId());

            ps.setString(2, p.getSociete());
            ps.setString(3, p.getTel());
            ps.setString(4, p.getAdresse());

            ps.setLong(5, p.getUserId());
            ps.setString(6, p.getSociete());
            ps.setString(7, p.getTel());
            ps.setString(8, p.getAdresse());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Erreur upsert fournisseur profile", e);
        }
    }

    @Override
    public FournisseurProfile findByUserId(Long userId) throws DataAccessException {
        String sql = "SELECT ID, USER_ID, SOCIETE, TEL, ADRESSE, CREATED_AT FROM FOURNISSEUR_PROFILE WHERE USER_ID = ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                FournisseurProfile p = new FournisseurProfile();
                p.setId(rs.getLong("ID"));
                p.setUserId(rs.getLong("USER_ID"));
                p.setSociete(rs.getString("SOCIETE"));
                p.setTel(rs.getString("TEL"));
                p.setAdresse(rs.getString("ADRESSE"));
                Timestamp t = rs.getTimestamp("CREATED_AT");
                if (t != null) p.setCreatedAt(new java.util.Date(t.getTime()));
                return p;
            }

        } catch (SQLException e) {
            throw new DataAccessException("Erreur find fournisseur profile", e);
        }
    }
}