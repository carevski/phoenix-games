package com.spotlight.platform.userprofile.api.web.modules;

import com.google.inject.AbstractModule;

import com.google.inject.Provides;
import com.spotlight.platform.userprofile.api.core.profile.UserProfileService;
import com.spotlight.platform.userprofile.api.core.profile.persistence.UserProfileDao;
import com.spotlight.platform.userprofile.api.core.profile.persistence.UserProfileDaoInMemory;
import com.spotlight.platform.userprofile.api.model.commands.*;

import javax.inject.Named;
import javax.inject.Singleton;

public class ProfileModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(UserProfileDao.class).to(UserProfileDaoInMemory.class).in(Singleton.class);
        bind(UserProfileService.class).in(Singleton.class);
        bind(CommandFactory.class).in(Singleton.class);
    }

    @Provides
    @Named("collect")
    UserCommand collectUserCommand(UserProfileDao userProfileDao) {
        return new CollectUserCommand(userProfileDao);
    }

    @Provides
    @Named("increment")
    UserCommand incrementUserCommand(UserProfileDao userProfileDao) {
        return new IncrementUserCommand(userProfileDao);
    }

    @Provides
    @Named("replace")
    UserCommand replaceUserCommand(UserProfileDao userProfileDao) {
        return new ReplaceUserCommand(userProfileDao);
    }

}
