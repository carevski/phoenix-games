package com.spotlight.platform.userprofile.api.model.commands;

import com.spotlight.platform.userprofile.api.core.profile.persistence.UserProfileDao;
import com.spotlight.platform.userprofile.api.model.profile.UserProfile;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserId;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserProfilePropertyName;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserProfilePropertyValue;
import com.spotlight.platform.userprofile.api.web.resources.CommandEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class CollectUserCommand implements UserCommand {

    private static final Logger log = LoggerFactory.getLogger(CollectUserCommand.class);
    private final UserProfileDao userProfileDao;
    private CommandEntity commandData;

    @Inject
    public CollectUserCommand(UserProfileDao userProfileDao) {
        this.userProfileDao = userProfileDao;
    }

    @Override
    public void setCommandData(CommandEntity commandData) {
        this.commandData = commandData;
    }

    @Override
    public void execute() {
        validateCommandData();
        UserProfile userProfile = resolveUser(commandData.userId());
        HashMap<UserProfilePropertyName, UserProfilePropertyValue> userPropertiesClone =
                new HashMap<>(userProfile.userProfileProperties());
        if (collect(userPropertiesClone, commandData.properties())) {
            //since UserProfile is immutable we are creating a new instance
            //each time we update but also update the latestUpdateTime
            userProfileDao.put(new UserProfile(commandData.userId(), Instant.now(), userPropertiesClone));
        }
    }

    private void validateCommandData() {
        if (null == commandData) {
            throw new IllegalStateException("Command data are not set");
        }
        if (!"collect".equalsIgnoreCase(commandData.type())) {
            throw new IllegalStateException("Missconfigured command");
        }
    }

    private UserProfile resolveUser(UserId userId) {
        return userProfileDao
                .get(userId)
                .orElseGet(() -> new UserProfile(userId, Instant.now(), new HashMap<>()));
    }

    @SuppressWarnings("unchecked") //unfortunate, but to eliminate it I would've had to do deeper changes to the model
    private boolean collect(
            Map<UserProfilePropertyName, UserProfilePropertyValue> userProperties,
            Map<String, Object> properties
    ) {
        final AtomicBoolean updated = new AtomicBoolean(false);
        properties.forEach((key, value) -> {
            if (!(value instanceof List)) {
                log.error("Property value is not a list type, key={}, value={}", key, value);
                throw new IllegalArgumentException("Property value is not a list type");
            }
            UserProfilePropertyName propName = UserProfilePropertyName.valueOf(key);
            UserProfilePropertyValue propValue = userProperties
                    .getOrDefault(propName, UserProfilePropertyValue.valueOf(new ArrayList<>()));

            if (!(propValue.getValue() instanceof List)) {
                throw new IllegalArgumentException("Existing property for collection is not of type List");
            }

            List<Object> clone = new ArrayList<>((List<Object>) propValue.getValue());
            clone.addAll((List<Object>) value);

            UserProfilePropertyValue newValue = UserProfilePropertyValue.valueOf(clone);
            updated.set(true);
            userProperties.put(propName, newValue);
        });
        return updated.get();
    }
}
