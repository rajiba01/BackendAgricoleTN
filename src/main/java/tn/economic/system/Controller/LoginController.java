package tn.economic.system.Controller;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tn.economic.system.models.Auth;

import tn.economic.system.services.UserService;

import java.util.Map;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class LoginController {

    private final UserService userService = new UserService();

    @POST
    @Path("/login")
    public Response login(Auth auth) {
       // User user = userService.findByEmail(auth.getEmail());

        String token = userService.loginAndGenerateToken(auth);

        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Email ou mot de passe incorrect")
                    .build();
        }

        return Response.ok(
                Map.of(
                        "token", token,
                        "type", "Bearer"
                )
        ).build();
    }
}

