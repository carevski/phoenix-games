package com.spotlight.platform.userprofile.api.model.commands;

import com.google.inject.ConfigurationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.ws.rs.BadRequestException;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CommandFactoryTest {

    private final Injector injector = mock(Injector.class);

    private final CommandFactory testee = new CommandFactory(injector);

    @BeforeEach
    public void beforeEach() {
        Mockito.reset(injector);
    }

    @Test
    void testCommandExists() {
        doReturn(new IncrementUserCommand(null)).when(injector).getInstance(any(Key.class));
        assertThat(testee.instance("increment")).isInstanceOf(IncrementUserCommand.class);
    }

    @Test
    void testCommandDoesNotExist() {
        doThrow(new ConfigurationException(new ArrayList<>())).when(injector).getInstance(any(Key.class));
        assertThrows(BadRequestException.class, () -> testee.instance("increment"), "Command type is not supported");
    }
}