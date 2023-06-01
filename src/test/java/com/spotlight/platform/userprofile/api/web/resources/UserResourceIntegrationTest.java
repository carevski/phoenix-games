package com.spotlight.platform.userprofile.api.web.resources;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import com.spotlight.platform.userprofile.api.core.profile.persistence.UserProfileDao;
import com.spotlight.platform.userprofile.api.model.commands.*;
import com.spotlight.platform.userprofile.api.model.profile.UserProfile;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserId;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserProfileFixtures;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserProfilePropertyName;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserProfilePropertyValue;
import com.spotlight.platform.userprofile.api.web.UserProfileApiApplication;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.util.annotation.Name;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import ru.vyarus.dropwizard.guice.test.ClientSupport;
import ru.vyarus.dropwizard.guice.test.jupiter.ext.TestDropwizardAppExtension;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.client.Entity;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Execution(ExecutionMode.SAME_THREAD)
class UserResourceIntegrationTest {
    @RegisterExtension
    static TestDropwizardAppExtension APP = TestDropwizardAppExtension.forApp(UserProfileApiApplication.class)
            .randomPorts()
            .hooks(builder -> builder.modulesOverride(new AbstractModule() {
                @Provides
                @Singleton
                public UserProfileDao getUserProfileDao() {
                    return mock(UserProfileDao.class);
                }

                @Provides
                @Singleton
                public CommandFactory commandFactory() {
                    return mock(CommandFactory.class);
                }
            }))
            .randomPorts()
            .create();

    @BeforeEach
    void beforeEach(UserProfileDao userProfileDao, CommandFactory commandFactory) {
        reset(userProfileDao, commandFactory);
    }

    @Nested
    @DisplayName("getUserProfile")
    class GetUserProfile {
        private static final String USER_ID_PATH_PARAM = "userId";
        private static final String URL = "/users/{%s}/profile".formatted(USER_ID_PATH_PARAM);

        @Test
        void existingUser_correctObjectIsReturned(ClientSupport client, UserProfileDao userProfileDao) {
            when(userProfileDao.get(any(UserId.class))).thenReturn(Optional.of(UserProfileFixtures.USER_PROFILE));

            var response = client.targetRest().path(URL).resolveTemplate(USER_ID_PATH_PARAM, UserProfileFixtures.USER_ID).request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
            assertThatJson(response.readEntity(UserProfile.class)).isEqualTo(UserProfileFixtures.SERIALIZED_USER_PROFILE);
        }

        @Test
        void nonExistingUser_returns404(ClientSupport client, UserProfileDao userProfileDao) {
            when(userProfileDao.get(any(UserId.class))).thenReturn(Optional.empty());

            var response = client.targetRest().path(URL).resolveTemplate(USER_ID_PATH_PARAM, UserProfileFixtures.USER_ID).request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatus.NOT_FOUND_404);
        }

        @Test
        void validationFailed_returns400(ClientSupport client) {
            var response = client.targetRest()
                    .path(URL)
                    .resolveTemplate(USER_ID_PATH_PARAM, UserProfileFixtures.INVALID_USER_ID)
                    .request()
                    .get();

            assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);
        }

        @Test
        void unhandledExceptionOccured_returns500(ClientSupport client, UserProfileDao userProfileDao) {
            when(userProfileDao.get(any(UserId.class))).thenThrow(new RuntimeException("Some unhandled exception"));

            var response = client.targetRest().path(URL).resolveTemplate(USER_ID_PATH_PARAM, UserProfileFixtures.USER_ID).request().get();

            assertThat(response.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR_500);
        }
    }


    @Nested
    @DisplayName("executeCommand")
    class ExecuteCommand {
        private static final String URL = "/users/command";

        @Test
        void validate_missingData_userId(ClientSupport client) {
            Entity<CommandEntity> entity = Entity.entity(new CommandEntity(null, "resolve", Map.of("test", 4)), "application/json");
            var response = client.targetRest(URL).request().post(entity);

            assertThat(response.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY_422);
        }

        @Test
        void validate_missingData_type(ClientSupport client) {
            Entity<CommandEntity> entity = Entity.entity(new CommandEntity(UserId.valueOf("identite"), null, Map.of("test", 4)), "application/json");
            var response = client.targetRest(URL).request().post(entity);

            assertThat(response.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY_422);
        }

        @Test
        void test_missingCommandType(ClientSupport client, CommandFactory commandFactory) {
            doThrow(new BadRequestException("Command type is not supported")).when(commandFactory).instance(any());
            Entity<CommandEntity> entity = Entity.entity(new CommandEntity(UserId.valueOf("identite"), "doesNotExist", Map.of("test", 4)), "application/json");
            var response = client.targetRest(URL).request().post(entity);

            assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST_400);
        }

        @Test
        void test_increment(ClientSupport client, UserProfileDao userProfileDao, CommandFactory commandFactory) {
            UserId userId = UserId.valueOf("identite");
            IncrementUserCommand incrementUserCommand = mock(IncrementUserCommand.class);
            doNothing().when(incrementUserCommand).setCommandData(any());
            doNothing().when(incrementUserCommand).execute();
            doReturn(incrementUserCommand).when(commandFactory).instance(eq("increment"));

            doReturn(Optional.of(new UserProfile(userId, Instant.now(), Map.of(UserProfilePropertyName.valueOf("test"), UserProfilePropertyValue.valueOf(4))))).when(userProfileDao).get(any(UserId.class));

            Entity<CommandEntity> entity = Entity.entity(new CommandEntity(userId, "increment", Map.of("test", 4)), "application/json");
            var response = client.targetRest(URL).request().post(entity);

            verify(commandFactory).instance(any());
            verify(incrementUserCommand).setCommandData(any());
            verify(incrementUserCommand).execute();

            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK_200);
        }
    }

    @Nested
    @DisplayName("executeCommands")
    class ExecuteCommands {
        private static final String URL = "/users/commands";

        @Test
        void test_multiple_commands(ClientSupport client, UserProfileDao userProfileDao, CommandFactory commandFactory) {
            UserId userId = UserId.valueOf("identite");
            IncrementUserCommand incrementUserCommand = mock(IncrementUserCommand.class);
            doNothing().when(incrementUserCommand).setCommandData(any());
            doNothing().when(incrementUserCommand).execute();

            ReplaceUserCommand replaceUserCommand = mock(ReplaceUserCommand.class);
            doNothing().when(replaceUserCommand).setCommandData(any());
            doNothing().when(replaceUserCommand).execute();
            doReturn(incrementUserCommand).when(commandFactory).instance(eq("increment"));
            doReturn(replaceUserCommand).when(commandFactory).instance(eq("replace"));

            CommandEntity commandOne = new CommandEntity(userId, "increment", Map.of("test", 4));
            CommandEntity commandTwo = new CommandEntity(userId, "replace", Map.of("test", 6));
            Entity<List<CommandEntity>> entity = Entity.entity(List.of(commandOne, commandTwo), "application/json");
            var response = client.targetRest(URL).request().post(entity);

            verify(commandFactory, times(2)).instance(any());
            verify(incrementUserCommand).setCommandData(any());
            verify(incrementUserCommand).execute();
            verify(replaceUserCommand).setCommandData(any());
            verify(replaceUserCommand).execute();

            verifyNoMoreInteractions(commandFactory, incrementUserCommand, replaceUserCommand);
            assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);
        }
    }
}