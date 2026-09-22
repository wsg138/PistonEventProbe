package com.enthusia.pistoneventprobe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.bukkit.command.PluginCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

final class PistonEventProbeCommandTest {
    private static final String TEST_PLUGIN_YML = """
            name: PistonEventProbe
            version: '1.0.0-test'
            main: com.enthusia.pistoneventprobe.PistonEventProbePlugin
            api-version: '1.21'
            commands:
              pistonprobe:
                description: Controls the piston event diagnostic probe.
                usage: /pistonprobe <start [count]|continuous|stop|status|listeners>
                permission: pistonprobe.admin
            permissions:
              pistonprobe.admin:
                description: Allows use of the piston event diagnostic probe.
                default: op
            """;

    private ServerMock server;
    private PistonEventProbePlugin plugin;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        plugin = MockBukkit.loadWith(
                PistonEventProbePlugin.class,
                new ByteArrayInputStream(TEST_PLUGIN_YML.getBytes(StandardCharsets.UTF_8)));
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void commandMetadataRetainsAdminPermission() {
        PluginCommand command = plugin.getCommand("pistonprobe");
        assertNotNull(command);
        assertEquals("pistonprobe.admin", command.getPermission());
    }

    @Test
    void nonAdminCannotChangeOrInspectProbe() {
        var player = server.addPlayer();
        PluginCommand command = plugin.getCommand("pistonprobe");
        assertNotNull(command);

        assertTrue(plugin.onCommand(player, command, "pistonprobe", new String[] {"status"}));
        assertEquals("You do not have permission to use this command.", player.nextMessage());
    }

    @Test
    void statusReportsStoppedByDefault() {
        var player = admin();

        server.dispatchCommand(player, "pistonprobe status");

        assertEquals("Piston probe status: stopped.", player.nextMessage());
    }

    @Test
    void startWithoutCountArmsTenCaptures() {
        var player = admin();

        server.dispatchCommand(player, "pistonprobe start");
        assertEquals("Piston probe armed for the next 10 piston extension event(s).", player.nextMessage());

        server.dispatchCommand(player, "pistonprobe status");
        assertEquals("Piston probe status: armed for 10 more event(s).", player.nextMessage());
    }

    @Test
    void startClampsCountIntoOneToOneHundredRange() {
        var player = admin();

        server.dispatchCommand(player, "pistonprobe start 0");
        assertEquals("Piston probe armed for the next 1 piston extension event(s).", player.nextMessage());
        server.dispatchCommand(player, "pistonprobe status");
        assertEquals("Piston probe status: armed for 1 more event(s).", player.nextMessage());

        server.dispatchCommand(player, "pistonprobe start 999");
        assertEquals("Piston probe armed for the next 100 piston extension event(s).", player.nextMessage());
        server.dispatchCommand(player, "pistonprobe status");
        assertEquals("Piston probe status: armed for 100 more event(s).", player.nextMessage());
    }

    @Test
    void invalidCountDoesNotArmProbe() {
        var player = admin();

        server.dispatchCommand(player, "pistonprobe start nope");
        assertEquals("Count must be a number from 1 to 100.", player.nextMessage());

        server.dispatchCommand(player, "pistonprobe status");
        assertEquals("Piston probe status: stopped.", player.nextMessage());
    }

    @Test
    void continuousAndStopTransitionsAreObservable() {
        var player = admin();

        server.dispatchCommand(player, "pistonprobe continuous");
        assertEquals("Piston probe enabled continuously. Use /pistonprobe stop when finished.", player.nextMessage());
        server.dispatchCommand(player, "pistonprobe status");
        assertEquals("Piston probe status: continuous capture enabled.", player.nextMessage());

        server.dispatchCommand(player, "pistonprobe stop");
        assertEquals("Piston probe stopped.", player.nextMessage());
        server.dispatchCommand(player, "pistonprobe status");
        assertEquals("Piston probe status: stopped.", player.nextMessage());
    }

    @Test
    void unknownSubcommandReturnsUsageWithoutChangingState() {
        var player = admin();

        server.dispatchCommand(player, "pistonprobe wat");
        assertEquals("Usage: /pistonprobe <start [count]|continuous|stop|status|listeners>", player.nextMessage());
        server.dispatchCommand(player, "pistonprobe status");
        assertEquals("Piston probe status: stopped.", player.nextMessage());
    }

    private org.mockbukkit.mockbukkit.entity.PlayerMock admin() {
        var player = server.addPlayer();
        player.setOp(true);
        return player;
    }
}
