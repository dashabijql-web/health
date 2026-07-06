package com.xzkj.health.handler.watch;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WatchLoginProtocolHandlerTest {

    @Test
    void loginConfigurationCommandsUseCompactNoStarFormat() {
        String imei = "123456789012345";
        String[] commands = WatchLoginProtocolHandler.buildInitialConfigurationCommands(imei);

        assertEquals(3, commands.length);
        assertCompactNoStarCommands(imei, commands);

        assertTrue(commands[0].startsWith("IWBP33," + imei + ","));
        assertTrue(commands[1].startsWith("IWBP86," + imei + ","));
        assertTrue(commands[2].startsWith("IWBP87," + imei + ","));
    }

    @Test
    void monitoringCommandsPollHealthAndLocationOnly() {
        String imei = "123456789012345";
        String[] commands = WatchLoginProtocolHandler.buildMonitoringCommands(imei);

        assertEquals(5, commands.length);
        assertCompactNoStarCommands(imei, commands);

        assertTrue(commands[0].startsWith("IWBPXL," + imei + ","));
        assertTrue(commands[1].startsWith("IWBPXY," + imei + ","));
        assertTrue(commands[2].startsWith("IWBPXZ," + imei + ","));
        assertTrue(commands[3].startsWith("IWBPXT," + imei + ","));
        assertTrue(commands[4].startsWith("IWBP16," + imei + ","));
    }

    private static void assertCompactNoStarCommands(String imei, String[] commands) {
        for (String command : commands) {
            assertTrue(command.startsWith("IWBP"));
            assertFalse(command.contains("*"));
            assertTrue(command.endsWith("#"));
            assertTrue(command.contains("," + imei + ","));
        }
    }
}
