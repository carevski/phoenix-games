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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectUserCommand implements UserCommand {

    private static final Logger log = LoggerFactory.getLogger(CollectUserCommand.class);
    private final UserProfileDao userProfileDao;
    private CommandWS commandData;

    @Inject
    public CollectUserCommand(UserProfileDao userProfileDao) {
        this.userProfileDao = userProfileDao;
    }

    @Override
    public void setCommandData(CommandWS commandData) {
        this.commandData = commandData;
    }

    @Override
    public void process() {
        UserProfile userProfile = resolveUser(commandData.getUserId());
        collect(userProfile, commandData.getProperties());
        userProfileDao.put(userProfile);
    }

    private UserProfile resolveUser(UserId userId) {
        return userProfileDao
                .get(userId)
                .orElseGet(() -> new UserProfile(userId, Instant.now(), new HashMap<>()));
    }

    @SuppressWarnings("unchecked") //unfortunate
    private void collect(UserProfile userProfile, Map<String, Object> properties) {
        properties.forEach((key, value) -> {
            if (!(value instanceof List)) {
                log.error("Property value is not a list type, key={}, value={}", key, value);
                throw new IllegalArgumentException("Property value is not a list type");
            }
            UserProfilePropertyName propName = UserProfilePropertyName.valueOf(key);
            UserProfilePropertyValue propValue = userProfile.userProfileProperties()
                    .getOrDefault(propName, UserProfilePropertyValue.valueOf(new ArrayList<>()));

            if (!(propValue.getValue() instanceof List)) {
                throw new IllegalArgumentException("Existing property for collection is not of type List");
            }

            List<Object> clone = new ArrayList<>((List<Object>) propValue.getValue());
            clone.addAll((List<Object>) value);

            UserProfilePropertyValue newValue = UserProfilePropertyValue.valueOf(clone);

            userProfile.userProfileProperties().put(propName, newValue);
        });
    }
}
