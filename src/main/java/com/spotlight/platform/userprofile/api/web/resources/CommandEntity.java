package com.spotlight.platform.userprofile.api.web.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.spotlight.platform.userprofile.api.model.profile.primitives.UserId;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Map;

public record CommandEntity(
        @JsonProperty
        @NotNull
        UserId userId,
        @JsonProperty
        @NotEmpty
        String type,
        @JsonProperty
        @NotEmpty
        Map<String, Object> properties
) {
}


