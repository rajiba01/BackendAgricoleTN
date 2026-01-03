package tn.economic.system.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Logger;

public class PasswordHashUtil {

    private static final Logger LOGGER = Logger.getLogger(PasswordHashUtil.class.getName());
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;

    // 🔹 Hasher un mot de passe
    public static String hashPassword(String password) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);

            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = skf.generateSecret(spec).getEncoded();

            return ITERATIONS + ":" +
                    Base64.getEncoder().encodeToString(salt) + ":" +
                    Base64.getEncoder().encodeToString(hash);

        } catch (Exception e) {
            LOGGER.severe("Password hashing failed: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static boolean verifyPassword(String inputPassword, String storedPassword) {
        try {
            String[] parts = storedPassword.split(":");

            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] hashDb = Base64.getDecoder().decode(parts[2]);

            PBEKeySpec spec = new PBEKeySpec(
                    inputPassword.toCharArray(),
                    salt,
                    iterations,
                    hashDb.length * 8
            );

            SecretKeyFactory skf =
                    SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            byte[] hashInput = skf.generateSecret(spec).getEncoded();

            return MessageDigest.isEqual(hashDb, hashInput);

        } catch (Exception e) {
            return false;
        }
    }

}

