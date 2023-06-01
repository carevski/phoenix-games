package com.spotlight.platform.userprofile.api.web.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserId;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Map;

public class CommandEntity {

    @JsonProperty
    @NotNull
    private UserId userId;

    @JsonProperty
    @NotEmpty
    private String type;
    @JsonProperty
    @NotEmpty
    private Map<String, Object> properties;

    public UserId getUserId() {
        return userId;
    }

    public void setUserId(UserId userId) {
        this.userId = userId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}
