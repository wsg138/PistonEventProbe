package com.enthusia.pistoneventprobe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

final class PluginDescriptorContractTest {
    @Test
    void productionDescriptorRetainsCommandAndAdminPermission() {
        var stream = PluginDescriptorContractTest.class.getClassLoader().getResourceAsStream("plugin.yml");
        assertNotNull(stream, "plugin.yml must be present on the test runtime classpath");

        var descriptor = YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));

        assertEquals("PistonEventProbe", descriptor.getString("name"));
        assertEquals("com.enthusia.pistoneventprobe.PistonEventProbePlugin", descriptor.getString("main"));
        assertEquals("1.21.11", descriptor.getString("api-version"));
        assertEquals("pistonprobe.admin", descriptor.getString("commands.pistonprobe.permission"));
        assertEquals("op", descriptor.getString("permissions.pistonprobe.admin.default"));
    }
}
