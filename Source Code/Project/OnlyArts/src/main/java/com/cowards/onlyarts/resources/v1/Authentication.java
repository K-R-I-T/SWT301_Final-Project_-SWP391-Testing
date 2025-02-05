package com.cowards.onlyarts.resources.v1;

import com.cowards.onlyarts.repositories.token.TokenDTO;
import com.cowards.onlyarts.repositories.token.TokenERROR;
import com.cowards.onlyarts.repositories.user.UserDTO;
import com.cowards.onlyarts.repositories.user.UserERROR;
import com.cowards.onlyarts.services.TokenDAO;
import com.cowards.onlyarts.services.UserDAO;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("v1/authentication")
public class Authentication {

    private static final UserDAO userDao = UserDAO.getInstance();
    private static final TokenDAO tokenDao = TokenDAO.getInstance();

    @POST
    @Path("login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(UserDTO user) {
        try {
            UserDTO loginUser = userDao.getUserByEmail(user.getEmail());
            if (!user.getPassword().equals(loginUser.getPassword())) {
                throw new UserERROR("Password does not match in the system");
            }
            String tokenString = tokenDao.addLoginToken(loginUser.getUserId());
            if (!loginUser.isOnline()) {
                userDao.changeStatus(loginUser.getUserId(),
                        loginUser.getStatus(),
                        0b001);
            }
            return Response.status(Response.Status.OK)
                    .entity(tokenString)
                    .build();
        } catch (UserERROR ex) {
            return Response.status(Response.Status.NOT_ACCEPTABLE)
                    .entity(ex)
                    .build();
        }
    }

    @DELETE
    @Path("logout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout(@HeaderParam("authtoken") String tokenString) {
        try {
            TokenDTO token = tokenDao.getToken(tokenString);
            tokenDao.deactivateToken(tokenString);
            UserDTO user = userDao.getUserById(token.getUserId());
            if (user.isOnline()) {
                userDao.changeStatus(user.getUserId(),
                        user.getStatus(),
                        0b001);
            }
            return Response.status(Response.Status.NO_CONTENT)
                    .build();
        } catch (TokenERROR | UserERROR ex) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ex)
                    .build();
        }
    }

    @GET
    @Path("account")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccountInfo(@HeaderParam("authtoken") String tokenString) {
        try {
            TokenDTO token = tokenDao.getToken(tokenString);
            if (!token.isLoginToken()) {
                throw new TokenERROR("Invalid authentication token");
            } else if (!token.isValid()) {
                throw new TokenERROR("Token is invalidate");
            } else if (token.isExpired()) {
                throw new TokenERROR("Token is expired");
            }
            UserDTO user = userDao.getUserById(token.getUserId());
            if (!user.isOnline()) {
                userDao.changeStatus(user.getUserId(),
                        user.getStatus(),
                        0b001);
            }
            return Response.status(Response.Status.ACCEPTED)
                    .entity(user)
                    .build();
        } catch (TokenERROR ex) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ex)
                    .build();
        } catch (UserERROR ex) {
            return Response.status(404).entity(ex).build();
        }
    }

    @POST
    @Path("register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(UserDTO user) {
        try {
            userDao.getUserByEmail(user.getEmail());
            return Response.status(Response.Status.CONFLICT)
                    .entity(new UserERROR("This email already exist in the system"))
                    .build();
        } catch (UserERROR ex) {
            String userId = userDao.addNewUser(user).getUserId();
            String authtoken = tokenDao.addLoginToken(userId);
            return Response.status(Response.Status.CREATED)
                    .entity(authtoken)
                    .build();
        }
    }
}
