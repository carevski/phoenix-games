package com.spotlight.platform.userprofile.api.web.exceptionmappers;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ErrorBean (
        @JsonProperty String message,
        @JsonProperty String type,
        @JsonProperty int errorCode
){}
