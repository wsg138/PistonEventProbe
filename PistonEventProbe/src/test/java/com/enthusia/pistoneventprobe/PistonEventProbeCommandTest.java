package com.enthusia.pistoneventprobe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class PistonEventProbeCommandTest {
    private PistonEventProbePlugin plugin;
    private Command command;
    private CommandSender sender;
    private List<String> messages;

    @BeforeEach
    void setUp() throws ReflectiveOperationException {
        plugin = mock(PistonEventProbePlugin.class, CALLS_REAL_METHODS);
        doReturn(Logger.getLogger("PistonEventProbeCommandTest")).when(plugin).getLogger();

        Field eventIds = PistonEventProbePlugin.class.getDeclaredField("eventIds");
        eventIds.setAccessible(true);
        eventIds.set(plugin, new IdentityHashMap<>());

        command = mock(Command.class);
        sender = mock(CommandSender.class);
        messages = new ArrayList<>();

        when(sender.hasPermission("pistonprobe.admin")).thenReturn(true);
        when(sender.getName()).thenReturn("TestSender");
        doAnswer(invocation -> {
            messages.add(invocation.getArgument(0, String.class));
            return null;
        }).when(sender).sendMessage(anyString());
    }

    @Test
    void nonAdminCannotChangeOrInspectProbe() {
        when(sender.hasPermission("pistonprobe.admin")).thenReturn(false);

        assertTrue(execute("status"));
        assertEquals("You do not have permission to use this command.", lastMessage());
    }

    @Test
    void statusReportsStoppedByDefault() {
        assertTrue(execute("status"));
        assertEquals("Piston probe status: stopped.", lastMessage());
    }

    @Test
    void emptyArgumentsAlsoReportStoppedStatus() {
        assertTrue(execute());
        assertEquals("Piston probe status: stopped.", lastMessage());
    }

    @Test
    void startWithoutCountArmsTenCaptures() {
        assertTrue(execute("start"));
        assertEquals("Piston probe armed for the next 10 piston extension event(s).", lastMessage());

        assertTrue(execute("status"));
        assertEquals("Piston probe status: armed for 10 more event(s).", lastMessage());
    }

    @Test
    void startClampsCountIntoOneToOneHundredRange() {
        assertTrue(execute("start", "0"));
        assertEquals("Piston probe armed for the next 1 piston extension event(s).", lastMessage());
        assertTrue(execute("status"));
        assertEquals("Piston probe status: armed for 1 more event(s).", lastMessage());

        assertTrue(execute("start", "999"));
        assertEquals("Piston probe armed for the next 100 piston extension event(s).", lastMessage());
        assertTrue(execute("status"));
        assertEquals("Piston probe status: armed for 100 more event(s).", lastMessage());
    }

    @Test
    void negativeCountClampsToOne() {
        assertTrue(execute("start", "-25"));
        assertEquals("Piston probe armed for the next 1 piston extension event(s).", lastMessage());
    }

    @Test
    void invalidCountDoesNotArmProbe() {
        assertTrue(execute("start", "nope"));
        assertEquals("Count must be a number from 1 to 100.", lastMessage());

        assertTrue(execute("status"));
        assertEquals("Piston probe status: stopped.", lastMessage());
    }

    @Test
    void continuousAndStopTransitionsAreObservable() {
        assertTrue(execute("continuous"));
        assertEquals("Piston probe enabled continuously. Use /pistonprobe stop when finished.", lastMessage());
        assertTrue(execute("status"));
        assertEquals("Piston probe status: continuous capture enabled.", lastMessage());

        assertTrue(execute("stop"));
        assertEquals("Piston probe stopped.", lastMessage());
        assertTrue(execute("status"));
        assertEquals("Piston probe status: stopped.", lastMessage());
    }

    @Test
    void startAfterContinuousDisablesContinuousMode() {
        assertTrue(execute("continuous"));
        assertTrue(execute("start", "3"));
        assertTrue(execute("status"));
        assertEquals("Piston probe status: armed for 3 more event(s).", lastMessage());
    }

    @Test
    void unknownSubcommandReturnsUsageWithoutChangingState() {
        assertTrue(execute("wat"));
        assertEquals("Usage: /pistonprobe <start [count]|continuous|stop|status|listeners>", lastMessage());
        assertTrue(execute("status"));
        assertEquals("Piston probe status: stopped.", lastMessage());
    }

    private boolean execute(String... args) {
        return plugin.onCommand(sender, command, "pistonprobe", args);
    }

    private String lastMessage() {
        return messages.get(messages.size() - 1);
    }
}
