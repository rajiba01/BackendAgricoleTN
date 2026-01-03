package tn.economic.system.services;

import tn.economic.system.models.Auth;
import tn.economic.system.models.User;

public interface IUserService {
    void createUser(User user);
    String loginAndGenerateToken(Auth auth);
  //  User findByEmail(String email);
}
