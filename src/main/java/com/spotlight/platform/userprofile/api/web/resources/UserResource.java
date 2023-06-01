package com.spotlight.platform.userprofile.api.web.resources;

import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.spotlight.platform.userprofile.api.core.profile.UserProfileService;
import com.spotlight.platform.userprofile.api.model.commands.UserCommand;
import com.spotlight.platform.userprofile.api.model.profile.UserProfile;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserId;
import io.dropwizard.core.cli.Command;
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

    private final Injector injector;

    @Inject
    public UserResource(UserProfileService userProfileService, Injector injector) {
        this.userProfileService = userProfileService;
        this.injector = injector;
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
        return userProfileService.get(commandEntity.getUserId());
    }

    @POST
    @Path("commands")
    public void executeCommands(@Valid List<CommandEntity> commands) {
        for (CommandEntity command : commands) {
            buildCommand(command).execute();
        }
    }

    private UserCommand buildCommand(CommandEntity commandEntity) {
        UserCommand command = instance(commandEntity.getType());
        command.setCommandData(commandEntity);
        return command;
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
