package tn.economic.system.services;

import tn.economic.system.models.Auth;
import tn.economic.system.models.User;
import tn.economic.system.repository.UserRepository;
import tn.economic.system.repository.UserRep;
import tn.economic.system.util.JwtUtil;
import org.mindrot.jbcrypt.BCrypt;

public class UserService implements IUserService {



        private final UserRepository userRepository = new UserRep();

        public void createUser(User user) {

            if (user.getMdp() == null || user.getMdp().length() < 8) {
                throw new IllegalArgumentException("Password too weak");
            }

            // 🔐 HASH UNE SEULE FOIS
            String hashedPassword = BCrypt.hashpw(user.getMdp(), BCrypt.gensalt(12));
            user.setMdp(hashedPassword);

            userRepository.save(user);
        }

        public String loginAndGenerateToken(Auth auth) {

            User user = userRepository.findByEmail(auth.getEmail());
            if (user == null) return null;

            boolean valid = BCrypt.checkpw(auth.getMdp(), user.getMdp());
            System.out.println("Password valid = " + valid);

            if (!valid) return null;

            return JwtUtil.generateToken(
                    user.getEmail(),
                    user.getRole() != null ? user.getRole().name() : null
            );
        }
    }
