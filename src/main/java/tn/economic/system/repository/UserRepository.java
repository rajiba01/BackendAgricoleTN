package tn.economic.system.repository;
import tn.economic.system.models.User;
public interface UserRepository {
    void save(User user);
    User findByEmail(String email);

}
