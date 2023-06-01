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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class IncrementUserCommand implements UserCommand {

    private static final Logger log = LoggerFactory.getLogger(IncrementUserCommand.class);

    private final UserProfileDao userProfileDao;

    private CommandEntity commandData;

    @Inject
    public IncrementUserCommand(UserProfileDao userProfileDao) {
        this.userProfileDao = userProfileDao;
    }

    @Override
    public void setCommandData(CommandEntity commandData) {
        this.commandData = commandData;
    }

    @Override
    public void execute() {
        UserProfile userProfile = resolveUser(commandData.getUserId());
        HashMap<UserProfilePropertyName, UserProfilePropertyValue> userPropertiesClone =
                new HashMap<>(userProfile.userProfileProperties());
        if (increment(userPropertiesClone, commandData.getProperties())) {
            //since UserProfile is immutable we are creating a new instance
            //each time we update but also update the latestUpdateTime
            userProfileDao.put(new UserProfile(commandData.getUserId(), Instant.now(), userPropertiesClone));
        }
    }

    private UserProfile resolveUser(UserId userId) {
        return userProfileDao
                .get(userId)
                .orElseGet(() -> new UserProfile(userId, Instant.now(), new HashMap<>()));
    }

    private boolean increment(
            Map<UserProfilePropertyName, UserProfilePropertyValue> userProperties,
            Map<String, Object> properties
    ) {
        final AtomicBoolean updated = new AtomicBoolean(false);
        properties.forEach((key, value) -> {
            if (!(value instanceof Integer)) {
                log.error("Property value is not a integer type, key={}, value={}", key, value);
                throw new IllegalArgumentException("Property value is not a integer type");
            }
            UserProfilePropertyName propName = UserProfilePropertyName.valueOf(key);
            UserProfilePropertyValue propValue = userProperties.getOrDefault(propName, UserProfilePropertyValue.valueOf(0));

            if (!(propValue.getValue() instanceof Integer)) {
                throw new IllegalArgumentException("Existing property is not of type Integer");
            }

            UserProfilePropertyValue newValue = UserProfilePropertyValue.valueOf(
                    ((Integer) propValue.getValue()) + ((Integer) value)
            );
            updated.set(true);
            userProperties.put(propName, newValue);
        });
        return updated.get();
    }
}
