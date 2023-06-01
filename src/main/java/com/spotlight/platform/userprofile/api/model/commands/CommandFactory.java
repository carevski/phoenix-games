package com.spotlight.platform.userprofile.api.model.commands;

import com.google.inject.ConfigurationException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;

/**
 * This code is extracted into separate class becuase this is much easier to mock then the Injector.
 *
 * For this kind of small use case Factory pattern is an overkill, also the guice DI system is already
 * factory pattern.
 */
public class CommandFactory {

    private static final Logger log = LoggerFactory.getLogger(CommandFactory.class);

    private final Injector injector;


    @Inject
    public CommandFactory(Injector injector) {
        this.injector = injector;
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
    public UserCommand instance(String type) {
        try {
            return injector.getInstance(Key.get(UserCommand.class, Names.named(type)));
        } catch (ConfigurationException configurationException) {
            log.error("Command for type: {} does not exist", type);
            throw new BadRequestException("Command type is not supported");
        }
    }
}
