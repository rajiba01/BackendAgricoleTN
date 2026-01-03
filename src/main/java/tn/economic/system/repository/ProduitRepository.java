package tn.economic.system.repository;

import tn.economic.system.ModelException.DataAccessException;
import tn.economic.system.connection.DBConnection;
import tn.economic.system.enums.ProduitType;
import tn.economic.system.models.Produits;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProduitRepository implements IProduitRepository {
    @Override
    public  void save(Produits produits) {
        String sql = """
            INSERT INTO PRODUIT (TYPE, QUANTITE, PRIX)
            VALUES (?, ?, ?)
        """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, produits.getType().name());
            ps.setInt(2, produits.getQuantite());
            ps.setFloat(3, produits.getPrix());


            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Erreur lors de l'insertion de l'utilisateur", e);
        }
    }

    public List<Produits> findAll() {
        List<Produits> produits = new ArrayList<>();
        String sql = "SELECT idProduit, type, quantite, prix FROM produit";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Produits p = new Produits();
                p.setIdProduit(rs.getLong("idProduit"));

                // Convertir String en Enum en sécurisant
                String typeStr = rs.getString("type");
                try {
                    p.setType(ProduitType.valueOf(typeStr));
                } catch (IllegalArgumentException e) {
                    System.err.println("Type invalide en base : " + typeStr);
                    p.setType(null);
                }

                p.setQuantite(rs.getInt("quantite"));
                p.setPrix(rs.getFloat("prix"));
                produits.add(p);
            }

        } catch (SQLException e) {
            e.printStackTrace();  // Important pour voir la vraie erreur
            throw new DataAccessException("Erreur lors de la récupération des produits", e);
        }

        return produits;
    }
    
public Long findIdByType(ProduitType type) {
  String sql = "SELECT idProduit FROM PRODUIT WHERE TYPE = ? FETCH FIRST 1 ROWS ONLY";

  try (Connection conn = DBConnection.getConnection();
       PreparedStatement ps = conn.prepareStatement(sql)) {

    ps.setString(1, type.name());
    try (ResultSet rs = ps.executeQuery()) {
      if (rs.next()) return rs.getLong("idProduit");
      return null;
    }
  } catch (SQLException e) {
    throw new DataAccessException("Erreur findIdByType", e);
  }

}}
