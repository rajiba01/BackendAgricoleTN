package tn.economic.system.repository;

import tn.economic.system.ModelException.DataAccessException;
import tn.economic.system.connection.DBConnection;
import tn.economic.system.enums.Role;
import tn.economic.system.models.User;
import tn.economic.system.services.UserValidation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;



public class UserRep implements UserRepository {

    @Override
    public void save(User user) {
        executeInsert(user);
    }

    private void executeInsert(User user) {
// NEW: validate + normalize localisation (region)
        Role role = user.getRole();
        UserValidation.validateLocalisationForRole(role, user.getLocalisation());
        String loc = UserValidation.normalizeRegion(user.getLocalisation());
        String sql = """
            INSERT INTO USERS (NOM, PRENOM, ROLE, TEL, LOCALISATION, EMAIL, MDP)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, user.getNom());
            ps.setString(2, user.getPrenom());
            ps.setString(3, user.getRole() != null ? user.getRole().name() : null);
            ps.setInt(4, user.getTel());
            ps.setString(5, loc);
            ps.setString(6, user.getEmail());

            ps.setString(7, user.getMdp());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new DataAccessException("Erreur lors de l'insertion de l'utilisateur", e);
        }
    }
    public User findByEmail(String email) throws DataAccessException {
        String sql = "SELECT ID, NOM, PRENOM, ROLE, TEL, LOCALISATION, EMAIL, MDP FROM USERS WHERE EMAIL = ?";
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User user = new User();
                user.setId(rs.getLong("ID"));
                user.setNom(rs.getString("NOM"));
                user.setPrenom(rs.getString("PRENOM"));
                String roleStr = rs.getString("ROLE");
                user.setRole(roleStr != null ? Role.valueOf(roleStr) : null);
                user.setTel(rs.getInt("TEL"));
                 user.setLocalisation(UserValidation.normalizeRegion(rs.getString("LOCALISATION")));
                user.setEmail(rs.getString("EMAIL"));
                user.setMdp(rs.getString("MDP")); // mot de passe haché
                return user;
            }
            return null;
        } catch (SQLException e) {
            throw new DataAccessException("Erreur lors de la récupération de l'utilisateur par email", e);
        }
    }
}
