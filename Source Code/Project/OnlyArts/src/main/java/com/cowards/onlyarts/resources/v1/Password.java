package com.cowards.onlyarts.resources.v1;

import com.cowards.onlyarts.repositories.token.TokenDTO;
import com.cowards.onlyarts.repositories.token.TokenERROR;
import com.cowards.onlyarts.repositories.user.UserDTO;
import com.cowards.onlyarts.repositories.user.UserERROR;
import com.cowards.onlyarts.services.TokenDAO;
import com.cowards.onlyarts.services.UserDAO;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("v1/password")
public class Password {

    private static final UserDAO userDao = UserDAO.getInstance();
    private static final TokenDAO tokenDao = TokenDAO.getInstance();

    @POST
    @Path("change")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changePassword(@HeaderParam("authtoken") String tokenString,
            @HeaderParam("oldpassword") String oldPw, UserDTO user) {
        try {
            TokenDTO token = tokenDao.getToken(tokenString);
            String newPw = user.getPassword();
            user = userDao.getUserById(token.getUserId());
            if (!user.getPassword().equals(oldPw)) {
                throw new UserERROR("Old password does not match in the system");
            }
            userDao.changePassword(user.getUserId(), newPw);
            return Response.status(Response.Status.OK)
                    .build();
        } catch (UserERROR | TokenERROR ex) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ex)
                    .build();
        }
    }

    @POST
    @Path("getresettoken")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getResetToken(String email) {
        try {
            UserDTO user = userDao.getUserByEmail(email);
            String tokenString = tokenDao.addResetPasswordToken(user.getUserId());
            return Response.status(Response.Status.ACCEPTED)
                    .entity(tokenString)
                    .build();
        } catch (UserERROR ex) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(ex)
                    .build();
        }
    }

    @POST
    @Path("resetpassword/{resettoken}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response resetPassword(@PathParam("resettoken") String resetToken,
            String newPw) {
        try {
            TokenDTO token = tokenDao.getToken(resetToken);
            UserDTO user = userDao.getUserById(token.getUserId());
            userDao.changePassword(user.getUserId(), newPw);
            return Response.status(204).build();
        } catch (TokenERROR ex) {
            return Response.status(401)
                    .entity(ex)
                    .build();
        } catch (UserERROR ex) {
            return Response.status(404)
                    .entity(ex)
                    .build();
        }
    }

}
