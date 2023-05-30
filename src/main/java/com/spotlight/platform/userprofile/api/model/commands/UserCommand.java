package com.spotlight.platform.userprofile.api.model.commands;

import com.spotlight.platform.userprofile.api.web.resources.CommandWS;

public interface UserCommand {

    void setCommandData(CommandWS commandData);

    void process();
}
