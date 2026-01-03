
package tn.economic.system.security;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import tn.economic.system.enums.Role;
import tn.economic.system.util.JwtUtil;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext ctx) {
        String path = ctx.getUriInfo().getPath();

        // Public endpoints
        if (path.startsWith("auth/login") || (path.startsWith("users") && ctx.getMethod().equals("POST"))) {
            return;
        }

        String header = ctx.getHeaderString("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("Missing token").build());
            return;
        }

        String token = header.substring("Bearer ".length()).trim();
        try {
            String email = JwtUtil.getEmailFromToken(token);
            String roleStr = JwtUtil.getRoleFromToken(token);
            Role role = roleStr != null ? Role.valueOf(roleStr) : Role.CLIENT;

            ctx.setProperty("auth", new AuthContext(email, role));
        } catch (Exception e) {
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("Invalid token").build());
        }
    }
}