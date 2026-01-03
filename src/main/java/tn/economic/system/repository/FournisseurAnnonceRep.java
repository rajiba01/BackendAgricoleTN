package tn.economic.system.repository;

import tn.economic.system.ModelException.DataAccessException;
import tn.economic.system.connection.DBConnection;
import tn.economic.system.models.FournisseurAnnonce;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FournisseurAnnonceRep implements FournisseurAnnonceRepository {

    @Override
    public Long create(FournisseurAnnonce a) throws DataAccessException {
        String sql = """
            INSERT INTO FOURNISSEUR_ANNONCE
              (USER_ID, PRODUIT_ID, TITRE, DESCRIPTION, QUALITE_SCORE, QUALITE_VERDICT, IMAGE_URL, PRIX_VENTE, ACTIVE)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)
        """;

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, new String[]{"ID"})) {

            ps.setLong(1, a.getUserId());
            ps.setLong(2, a.getProduitId());
            ps.setString(3, a.getTitre());
            ps.setString(4, a.getDescription());

            if (a.getQualiteScore() != null) ps.setInt(5, a.getQualiteScore()); else ps.setNull(5, Types.INTEGER);
            ps.setString(6, a.getQualiteVerdict());
            ps.setString(7, a.getImageUrl());
            ps.setBigDecimal(8, a.getPrixVente());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
            return null;

        } catch (SQLException e) {
            throw new DataAccessException("Erreur create annonce", e);
        }
    }

    @Override
    public List<FournisseurAnnonce> listByUserId(Long userId) throws DataAccessException {
        String sql = """
            SELECT ID, USER_ID, PRODUIT_ID, TITRE, DESCRIPTION, QUALITE_SCORE, QUALITE_VERDICT,
                   IMAGE_URL, PRIX_VENTE, ACTIVE, CREATED_AT
            FROM FOURNISSEUR_ANNONCE
            WHERE USER_ID = ?
            ORDER BY CREATED_AT DESC
        """;

        List<FournisseurAnnonce> out = new ArrayList<>();

        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setLong(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    FournisseurAnnonce a = new FournisseurAnnonce();
                    a.setId(rs.getLong("ID"));
                    a.setUserId(rs.getLong("USER_ID"));
                    a.setProduitId(rs.getLong("PRODUIT_ID"));
                    a.setTitre(rs.getString("TITRE"));
                    a.setDescription(rs.getString("DESCRIPTION"));

                    // FIX: NUMBER -> Integer
                    Object qs = rs.getObject("QUALITE_SCORE");
                    a.setQualiteScore(qs == null ? null : ((Number) qs).intValue());

                    a.setQualiteVerdict(rs.getString("QUALITE_VERDICT"));
                    a.setImageUrl(rs.getString("IMAGE_URL"));
                    a.setPrixVente(rs.getBigDecimal("PRIX_VENTE"));

                    // ACTIVE: utilise getInt
                    a.setActive(rs.getInt("ACTIVE"));

                    // IMPORTANT: push into output list
                    out.add(a);
                }
            }

            return out;

        } catch (SQLException e) {
            throw new DataAccessException("Erreur list annonces", e);
        }
    }
}
