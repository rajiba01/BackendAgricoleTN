package tn.economic.system.repository;

import tn.economic.system.ModelException.DataAccessException;
import tn.economic.system.connection.DBConnection;
import tn.economic.system.dto.AchatReceipt;

import java.sql.*;
import java.time.Instant;

public class AchatRepository {

    public AchatReceipt acheterProduitEtRetournerReceipt(Long idProduit, int qteDemandee) {
        String selectSql = "SELECT TYPE, QUANTITE, PRIX FROM PRODUIT WHERE IDPRODUIT = ? FOR UPDATE";

        // IMPORTANT: cette version suppose que ta table ACHAT a:
        // ID_ACHAT (PK) + DATE_ACHAT (timestamp) remplis par trigger/default
        String insertSql = """
            INSERT INTO ACHAT (ID_PRODUIT, QUANTITE, PRIX_UNITAIRE, TOTAL)
            VALUES (?, ?, ?, ?)
        """;

        // Pour récupérer l'ID généré: on utilise RETURN_GENERATED_KEYS (marche si driver Oracle le supporte)
        // Sinon, on utilisera "RETURNING ID_ACHAT INTO ?" (voir note plus bas)
        String updateStockSql = "UPDATE PRODUIT SET QUANTITE = QUANTITE - ? WHERE IDPRODUIT = ?";

        Connection connection = null;
        try {
            connection = DBConnection.getConnection();
            connection.setAutoCommit(false);

            String typeProduit;
            int stock;
            float prix;

            // 1) Lire stock + prix (lock row)
            try (PreparedStatement ps = connection.prepareStatement(selectSql)) {
                ps.setLong(1, idProduit);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        throw new DataAccessException("Produit introuvable (IDPRODUIT=" + idProduit + ")");
                    }
                    typeProduit = rs.getString("TYPE");
                    stock = rs.getInt("QUANTITE");
                    prix = rs.getFloat("PRIX");
                }
            }

            // 2) Vérifier quantité
            if (qteDemandee <= 0) throw new DataAccessException("Quantité invalide");
            if (stock < qteDemandee) throw new DataAccessException("Stock insuffisant");

            float total = prix * qteDemandee;

            // 3) Insérer achat + récupérer ID
            long idAchat;
            try (PreparedStatement ps = connection.prepareStatement(insertSql, new String[]{"ID_ACHAT"})) {
                ps.setLong(1, idProduit);
                ps.setInt(2, qteDemandee);
                ps.setFloat(3, prix);
                ps.setFloat(4, total);

                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys != null && keys.next()) {
                        idAchat = keys.getLong(1);
                    } else {
                        // fallback si driver ne retourne pas les keys
                        // (dans ce cas, on peut faire un SELECT via séquence/RETURNING)
                        throw new SQLException("Impossible de récupérer ID_ACHAT (generated keys vides).");
                    }
                }
            }

            // 4) Décrémenter stock
            try (PreparedStatement ps = connection.prepareStatement(updateStockSql)) {
                ps.setInt(1, qteDemandee);
                ps.setLong(2, idProduit);
                int updated = ps.executeUpdate();
                if (updated != 1) {
                    throw new DataAccessException("Échec mise à jour stock (updated=" + updated + ")");
                }
            }

            connection.commit();

            // 5) Date achat: si tu as DATE_ACHAT en base, tu peux la relire.
            // Ici on met "now" (simple). Pro: relire la vraie valeur en DB.
            Instant dateAchat = Instant.now();

            return new AchatReceipt(
                    idAchat,
                    idProduit,
                    typeProduit,
                    qteDemandee,
                    prix,
                    total,
                    dateAchat
            );

        } catch (SQLException e) {
            if (connection != null) {
                try { connection.rollback(); } catch (SQLException ignored) {}
            }
            throw new DataAccessException("Erreur lors de l'achat: " + e.getMessage(), e);
        } finally {
            if (connection != null) {
                try { connection.setAutoCommit(true); connection.close(); } catch (SQLException ignored) {}
            }
        }
    }

    // Tu peux garder ton ancienne méthode si tu veux
    public void acheterProduit(Long idProduit, int qteDemandee) {
        acheterProduitEtRetournerReceipt(idProduit, qteDemandee);
    }
}