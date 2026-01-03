
package tn.economic.system.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;

public class JwtUtil {

    private static final Key SECRET_KEY =
            Keys.hmacShaKeyFor("MySuperSecretKeyForJWT1234567890".getBytes());

    private static final long EXPIRATION_TIME = 60 * 60 * 1000; // 1 heure

    public static String generateToken(String email, String role) {

        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public static Claims validateToken(String token) throws JwtException {

        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // --- Added helper methods ---
    public static String getEmailFromToken(String token) throws JwtException {
        Claims claims = validateToken(token);
        return claims.getSubject(); // email stored in "sub"
    }

    public static String getRoleFromToken(String token) throws JwtException {
        Claims claims = validateToken(token);
        Object role = claims.get("role");
        return role != null ? role.toString() : null;
    }
}
