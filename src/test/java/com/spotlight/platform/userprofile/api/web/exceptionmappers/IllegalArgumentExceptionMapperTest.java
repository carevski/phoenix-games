package com.spotlight.platform.userprofile.api.web.exceptionmappers;

import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DropwizardExtensionsSupport.class)
class IllegalArgumentExceptionMapperTest {

    private static final ResourceExtension EXT = ResourceExtension.builder()
            .addResource(new IllegalArgumentExceptionMapperTest.MockResource())
            .setRegisterDefaultExceptionMappers(false)
            .addProvider(new IllegalArgumentExceptionMapper())
            .build();

    private Client client;

    @BeforeEach
    void setUp() {
        client = EXT.client();
    }

    @Test
    void badPostBodyValues_resultIs400() {
        Response response = client.target(IllegalArgumentExceptionMapperTest.MockResource.RESOURCE_URLS.THROW_EXCEPTION).request().post(Entity.json("{}"));

        assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    }

    @Path("/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static class MockResource {
        public static class RESOURCE_URLS {
            public static final String THROW_EXCEPTION = "/throwIllegalArgumentException";
        }

        @POST
        @Path(IllegalArgumentExceptionMapperTest.MockResource.RESOURCE_URLS.THROW_EXCEPTION)
        public void throwException() {
            throw new IllegalArgumentException("bad request");
        }
    }

}