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
import java.util.List;
import java.util.Map;

public class ReplaceUserCommand implements UserCommand {

    private static final Logger log = LoggerFactory.getLogger(ReplaceUserCommand.class);
    private final UserProfileDao userProfileDao;
    private CommandEntity commandData;

    @Inject
    public ReplaceUserCommand(UserProfileDao userProfileDao) {
        this.userProfileDao = userProfileDao;
    }

    @Override
    public void setCommandData(CommandEntity commandData) {
        this.commandData = commandData;
    }
    @Override
    public void execute() {
        UserProfile userProfile = resolveUser(commandData.getUserId());
        replace(userProfile, commandData.getProperties());
        userProfileDao.put(userProfile);
    }

    private UserProfile resolveUser(UserId userId) {
        return userProfileDao
                .get(userId)
                .orElseGet(() -> new UserProfile(userId, Instant.now(), new HashMap<>()));
    }

    private void replace(UserProfile userProfile, Map<String, Object> properties) {
        properties.forEach((key, value) -> {
            if(!(value instanceof Integer) && !(value instanceof List)) {
                log.error("Property value is not a integer/list type, key={}, value={}", key, value);
                throw new IllegalArgumentException("Property value is not a integer or list type");
            }
            UserProfilePropertyName propName = UserProfilePropertyName.valueOf(key);
            UserProfilePropertyValue newValue = UserProfilePropertyValue.valueOf(value);
            userProfile.userProfileProperties().put(propName, newValue);
        });
    }
}
