package com.spotlight.platform.userprofile.api.model.commands;

import com.spotlight.platform.userprofile.api.core.profile.persistence.UserProfileDao;
import com.spotlight.platform.userprofile.api.model.profile.UserProfile;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserId;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserProfilePropertyName;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserProfilePropertyValue;
import com.spotlight.platform.userprofile.api.web.resources.CommandEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IncrementUserCommandTest {
    private final UserProfileDao userProfileDaoMock = mock(UserProfileDao.class);

    private final IncrementUserCommand testee = new IncrementUserCommand(userProfileDaoMock);

    @BeforeEach
    void beforeEach() {
        Mockito.reset(userProfileDaoMock);
    }

    @Test
    void testMissingCommandData() {
        testee.setCommandData(null);
        assertThrows(IllegalStateException.class, testee::execute, "Command data are not set");
    }

    @Test
    void testMissconfiguredCommand() {
        CommandEntity commandEntity = new CommandEntity(
                UserId.valueOf("SomeID"),"somethingelse", Map.of()
        );
        testee.setCommandData(commandEntity);
        assertThrows(IllegalStateException.class, testee::execute, "Missconfigured command");
    }

    @Test
    void testIncrement_forMissingUser() {
        doReturn(Optional.empty()).when(userProfileDaoMock).get(any());
        UserId userId = UserId.valueOf("SomeID");
        CommandEntity commandEntity = new CommandEntity(userId,"increment", Map.of("prop1", 1));

        testee.setCommandData(commandEntity);
        testee.execute();

        verify(userProfileDaoMock).get(any());
        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileDaoMock).put(captor.capture());

        assertThat(captor.getValue().userId()).isEqualTo(userId);
        assertThat(captor.getValue().latestUpdateTime()).isNotNull();
        assertThat(captor.getValue().userProfileProperties().get(key("prop1")).getValue()).isEqualTo(1);

        verifyNoMoreInteractions(userProfileDaoMock);
    }

    @Test
    void testIncrement_forExistingUser() {
        UserId userId = UserId.valueOf("SomeID");
        UserProfile existingUser = new UserProfile(userId, Instant.now(), Map.of(key("prop1"), value(1)));
        doReturn(Optional.of(existingUser)).when(userProfileDaoMock).get(any());
        CommandEntity commandEntity = new CommandEntity(userId,"increment", Map.of("prop1", 5, "prop2", -3));

        testee.setCommandData(commandEntity);
        testee.execute();

        verify(userProfileDaoMock).get(any());
        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileDaoMock).put(captor.capture());

        assertThat(captor.getValue()).isNotEqualTo(existingUser);
        assertThat(captor.getValue().userId()).isEqualTo(userId);
        assertThat(captor.getValue().latestUpdateTime()).isNotNull();
        assertThat(captor.getValue().latestUpdateTime()).isNotEqualTo(existingUser.latestUpdateTime());
        assertThat(captor.getValue().userProfileProperties().get(key("prop1")).getValue()).isEqualTo(6);
        assertThat(captor.getValue().userProfileProperties().get(key("prop2")).getValue()).isEqualTo(-3);

        verifyNoMoreInteractions(userProfileDaoMock);
    }

    @Test
    void testIncrement_badArgumentInCommandData() {
        UserId userId = UserId.valueOf("SomeID");
        UserProfile existingUser = new UserProfile(userId, Instant.now(), Map.of(key("prop2"), value(4)));
        doReturn(Optional.of(existingUser)).when(userProfileDaoMock).get(any());
        CommandEntity commandEntity = new CommandEntity(userId,"increment", Map.of("prop2", List.of("first")));

        testee.setCommandData(commandEntity);
        assertThrows(IllegalArgumentException.class, testee::execute, "Property value is not a list type");

        verify(userProfileDaoMock).get(any());
        verifyNoMoreInteractions(userProfileDaoMock);
    }

    @Test
    void testIncrement_badArgumentInUserProps() {
        UserId userId = UserId.valueOf("SomeID");
        UserProfile existingUser = new UserProfile(userId, Instant.now(), Map.of(key("prop2"), value(List.of("first"))));
        doReturn(Optional.of(existingUser)).when(userProfileDaoMock).get(any());
        CommandEntity commandEntity = new CommandEntity(userId,"increment", Map.of("prop2", 4));

        testee.setCommandData(commandEntity);
        assertThrows(IllegalArgumentException.class, testee::execute, "Existing property for collection is not of type List");

        verify(userProfileDaoMock).get(any());
        verifyNoMoreInteractions(userProfileDaoMock);
    }

    private UserProfilePropertyValue value(Integer value) {
        return UserProfilePropertyValue.valueOf(value);
    }

    private UserProfilePropertyValue value(List<String> value) {
        return UserProfilePropertyValue.valueOf(value);
    }

    private UserProfilePropertyName key(String key) {
        return UserProfilePropertyName.valueOf(key);
    }
}