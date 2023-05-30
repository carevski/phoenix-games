package com.spotlight.platform.userprofile.api.web.exceptionmappers;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {
    @Override
    public Response toResponse(IllegalArgumentException exception) {
        return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(new ErrorBean(exception.getMessage(),
                        exception.getClass().getSimpleName(),
                        Response.Status.BAD_REQUEST.getStatusCode()))
                .build();
    }
}
