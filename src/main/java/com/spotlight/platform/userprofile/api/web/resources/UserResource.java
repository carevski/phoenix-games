package com.spotlight.platform.userprofile.api.web.resources;

import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.spotlight.platform.userprofile.api.core.profile.UserProfileService;
import com.spotlight.platform.userprofile.api.model.commands.UserCommand;
import com.spotlight.platform.userprofile.api.model.profile.UserProfile;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/users/{userId}")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {
    private static final Logger log = LoggerFactory.getLogger(UserResource.class);

    private final UserProfileService userProfileService;

    private final Injector injector;

    @Inject
    public UserResource(UserProfileService userProfileService, Injector injector) {
        this.userProfileService = userProfileService;
        this.injector = injector;
    }

    @Path("profile")
    @GET
    public UserProfile getUserProfile(@Valid @PathParam("userId") UserId userId) {
        return userProfileService.get(userId);
    }

    @POST
    @Path("command")
    public UserProfile executeCommand(@Valid @PathParam("userId") UserId userId, @Valid CommandWS commandEntity) {
        if (!userId.equals(commandEntity.getUserId())) {
            log.error("Path parameter userId does not match command userId. path userId:{}, command userId:{}", userId, commandEntity.getUserId());
            throw new BadRequestException("Path parameter userId does not match command userId");
        }
        UserCommand userCommand = instance(commandEntity.getType());
        userCommand.setCommandData(commandEntity);
        userCommand.process();
        return userProfileService.get(userId);
    }

    /**
     * This builds an instance of the given type of command.
     * <p>
     * Here we are leveraging guice's named beans to that
     * we make adding a command as easy as possible
     *
     * @param type the type of the command
     * @return instance of the given command type
     */
    private UserCommand instance(String type) {
        try {
            return injector.getInstance(Key.get(UserCommand.class, Names.named(type)));
        } catch (ConfigurationException configurationException) {
            log.error("Command for type: {} does not exist", type);
            throw new BadRequestException("Command type is not supported");
        }
    }
}
