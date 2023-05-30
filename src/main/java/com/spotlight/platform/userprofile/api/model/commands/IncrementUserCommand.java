package com.spotlight.platform.userprofile.api.model.commands;

import com.spotlight.platform.userprofile.api.core.profile.persistence.UserProfileDao;
import com.spotlight.platform.userprofile.api.model.profile.UserProfile;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserId;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserProfilePropertyName;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserProfilePropertyValue;
import com.spotlight.platform.userprofile.api.web.resources.CommandWS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class IncrementUserCommand implements UserCommand {

    private static final Logger log = LoggerFactory.getLogger(IncrementUserCommand.class);

    private final UserProfileDao userProfileDao;

    private CommandWS commandData;

    @Inject
    public IncrementUserCommand(UserProfileDao userProfileDao) {
        this.userProfileDao = userProfileDao;
    }

    @Override
    public void setCommandData(CommandWS commandData) {
        this.commandData = commandData;
    }

    @Override
    public void process() {
        UserProfile userProfile = resolveUser(commandData.getUserId());
        increment(userProfile, commandData.getProperties());
        userProfileDao.put(userProfile);
    }

    private UserProfile resolveUser(UserId userId) {
        return userProfileDao
                .get(userId)
                .orElseGet(() -> new UserProfile(userId, Instant.now(), new HashMap<>()));
    }

    private void increment(UserProfile userProfile, Map<String, Object> properties) {
        properties.forEach((key, value) -> {
            if(!(value instanceof Integer)) {
                log.error("Property value is not a integer type, key={}, value={}", key, value);
                throw new IllegalArgumentException("Property value is not a integer type");
            }
            UserProfilePropertyName propName = UserProfilePropertyName.valueOf(key);
            UserProfilePropertyValue propValue = userProfile.userProfileProperties()
                    .getOrDefault(propName, UserProfilePropertyValue.valueOf(0));

            if(!(propValue.getValue() instanceof Integer)) {
                throw new IllegalArgumentException("Existing property is not of type Integer");
            }

            UserProfilePropertyValue newValue = UserProfilePropertyValue.valueOf(
                    ((Integer)propValue.getValue()) + ((Integer)value)
            );

            userProfile.userProfileProperties().put(propName, newValue);
        });
    }
}
