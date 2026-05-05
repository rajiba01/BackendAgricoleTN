package tn.economic.system.Controller;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import tn.economic.system.models.User;

import tn.economic.system.services.UserService;

@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserController  {

    @POST
    public Response addUser(User dto) {
        UserService userService = new UserService();
        userService.createUser(dto);
        return Response.status(Response.Status.CREATED).build();
    }

}