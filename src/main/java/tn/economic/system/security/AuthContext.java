package tn.economic.system.security;

import tn.economic.system.enums.Role;

public class AuthContext {
    private final String email;
    private final Role role;

    public AuthContext(String email, Role role) {
        this.email = email;
        this.role = role;
    }

    public String getEmail() { return email; }
    public Role getRole() { return role; }
}