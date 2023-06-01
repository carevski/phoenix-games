package com.spotlight.platform.userprofile.api.model.commands;

import com.spotlight.platform.userprofile.api.web.resources.CommandEntity;

public interface UserCommand {

    void setCommandData(CommandEntity commandData);

    void execute();
}
