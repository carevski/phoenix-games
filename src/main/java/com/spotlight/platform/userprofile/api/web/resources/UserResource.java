package com.spotlight.platform.userprofile.api.web.resources;

import com.spotlight.platform.userprofile.api.core.profile.UserProfileService;
import com.spotlight.platform.userprofile.api.model.commands.CommandFactory;
import com.spotlight.platform.userprofile.api.model.commands.UserCommand;
import com.spotlight.platform.userprofile.api.model.profile.UserProfile;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/users")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserResource {
    private static final Logger log = LoggerFactory.getLogger(UserResource.class);

    private final UserProfileService userProfileService;

    private final CommandFactory commandFactory;

    @Inject
    public UserResource(UserProfileService userProfileService, CommandFactory commandFactory) {
        this.userProfileService = userProfileService;
        this.commandFactory = commandFactory;
    }

    @GET
    @Path("{userId}/profile")
    public UserProfile getUserProfile(@Valid @PathParam("userId") UserId userId) {
        return userProfileService.get(userId);
    }

    @POST
    @Path("command")
    public UserProfile executeCommand(@Valid CommandEntity commandEntity) {
        buildCommand(commandEntity).execute();
        return userProfileService.get(commandEntity.userId());
    }

    @POST
    @Path("commands")
    public void executeCommands(@Valid List<CommandEntity> commands) {
        for (CommandEntity command : commands) {
            buildCommand(command).execute();
        }
    }

    private UserCommand buildCommand(CommandEntity commandEntity) {
        UserCommand command = commandFactory.instance(commandEntity.type());
        command.setCommandData(commandEntity);
        return command;
    }
}
