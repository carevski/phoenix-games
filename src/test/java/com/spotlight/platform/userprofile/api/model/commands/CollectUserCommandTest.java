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
import static org.mockito.Mockito.*;

class CollectUserCommandTest {

    private final UserProfileDao userProfileDaoMock = mock(UserProfileDao.class);

    private final CollectUserCommand testee = new CollectUserCommand(userProfileDaoMock);

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
    void testCollect_forMissingUser() {
        doReturn(Optional.empty()).when(userProfileDaoMock).get(any());
        UserId userId = UserId.valueOf("SomeID");
        CommandEntity commandEntity = new CommandEntity(userId,"collect", Map.of("prop1", List.of("first")));

        testee.setCommandData(commandEntity);
        testee.execute();

        verify(userProfileDaoMock).get(any());
        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileDaoMock).put(captor.capture());

        assertThat(captor.getValue().userId()).isEqualTo(userId);
        assertThat(captor.getValue().latestUpdateTime()).isNotNull();
        assertThat(captor.getValue().userProfileProperties().get(key("prop1")).getValue()).asList().contains("first");

        verifyNoMoreInteractions(userProfileDaoMock);
    }

    @Test
    void testCollect_forExistingUser() {
        UserId userId = UserId.valueOf("SomeID");
        UserProfile existingUser = new UserProfile(userId, Instant.now(), Map.of(key("prop1"), value(List.of("first"))));
        doReturn(Optional.of(existingUser)).when(userProfileDaoMock).get(any());
        CommandEntity commandEntity = new CommandEntity(userId,"collect", Map.of("prop1", List.of("second")));

        testee.setCommandData(commandEntity);
        testee.execute();

        verify(userProfileDaoMock).get(any());
        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileDaoMock).put(captor.capture());

        assertThat(captor.getValue()).isNotEqualTo(existingUser);
        assertThat(captor.getValue().userId()).isEqualTo(userId);
        assertThat(captor.getValue().latestUpdateTime()).isNotNull();
        assertThat(captor.getValue().latestUpdateTime()).isNotEqualTo(existingUser.latestUpdateTime());
        assertThat(captor.getValue().userProfileProperties().get(key("prop1")).getValue()).asList().containsExactly("first", "second");

        verifyNoMoreInteractions(userProfileDaoMock);
    }

    @Test
    void testCollect_badArgumentInCommandData() {
        UserId userId = UserId.valueOf("SomeID");
        UserProfile existingUser = new UserProfile(userId, Instant.now(), Map.of(key("prop2"), value(List.of("first"))));
        doReturn(Optional.of(existingUser)).when(userProfileDaoMock).get(any());
        CommandEntity commandEntity = new CommandEntity(userId,"collect", Map.of("prop2", 4.0));

        testee.setCommandData(commandEntity);
        assertThrows(IllegalArgumentException.class, testee::execute, "Property value is not a list type");

        verify(userProfileDaoMock).get(any());
        verifyNoMoreInteractions(userProfileDaoMock);
    }

    @Test
    void testCollect_badArgumentInUserProps() {
        UserId userId = UserId.valueOf("SomeID");
        UserProfile existingUser = new UserProfile(userId, Instant.now(), Map.of(key("prop2"), value(4)));
        doReturn(Optional.of(existingUser)).when(userProfileDaoMock).get(any());
        CommandEntity commandEntity = new CommandEntity(userId,"collect", Map.of("prop2", List.of("second")));

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