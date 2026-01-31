package com.skyblockexp.ezshops.hook;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import org.bukkit.Server;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TransactionHookFeatureTest extends AbstractEzShopsTest {

    // lightweight Plugin implementations are created inline in tests below

    @Test
    void executeHooks_dispatchesConsoleCommand_whenRunAsConsole() throws Exception {
        // register a mock Vault provider and create a lightweight Plugin mock
        loadProviderPlugin(Mockito.mock(net.milkbowl.vault.economy.Economy.class));
        org.bukkit.plugin.Plugin plugin = Mockito.mock(org.bukkit.plugin.Plugin.class);

        // spy the server and replace Bukkit's server reference to intercept dispatches
        Server original = this.server;
        Server spyServer = Mockito.spy(original);

            // use the server's real console sender and scheduler

        // replace Bukkit.server via reflection
        java.lang.reflect.Field serverField = org.bukkit.Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, spyServer);

        // plugin mock should return the spy server and a test logger
        when(plugin.getServer()).thenReturn(spyServer);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("test-transaction-hooks"));

        TransactionHookService svc = new TransactionHookService(plugin);

        Player player = Mockito.mock(Player.class);
        when(player.getName()).thenReturn("tester");

        Map<String, String> tokens = new HashMap<>();
        tokens.put("amount", "3");

        List<String> cmds = List.of("say hello {player} {amount}");

        svc.executeHooks(player, cmds, true, tokens);

        // process one scheduler tick so the scheduled hook runnable executes
        ((org.mockbukkit.mockbukkit.scheduler.BukkitSchedulerMock) spyServer.getScheduler()).performOneTick();

        verify(spyServer).dispatchCommand(org.mockito.ArgumentMatchers.any(), eq("say hello tester 3"));

        // restore original server
        serverField.set(null, original);
    }

    @Test
    void executeHooks_dispatchesPlayerCommand_whenPlayerHasPermission_andRunAsConsoleFalse() throws Exception {
        loadProviderPlugin(Mockito.mock(net.milkbowl.vault.economy.Economy.class));
        org.bukkit.plugin.Plugin plugin = Mockito.mock(org.bukkit.plugin.Plugin.class);

        Server original = this.server;
        Server spyServer = Mockito.spy(original);

        // use the server's real console sender and scheduler

        java.lang.reflect.Field serverField = org.bukkit.Bukkit.class.getDeclaredField("server");
        serverField.setAccessible(true);
        serverField.set(null, spyServer);

        // plugin mock should return the spy server and a test logger
        when(plugin.getServer()).thenReturn(spyServer);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("test-transaction-hooks"));
        TransactionHookService svc = new TransactionHookService(plugin);

        Player player = Mockito.mock(Player.class);
        when(player.getName()).thenReturn("alice");
        when(player.hasPermission("ezshops.hooks.use")).thenReturn(true);

        Map<String, String> tokens = Collections.emptyMap();
        List<String> cmds = List.of("msg {player} hello");

        svc.executeHooks(player, cmds, false, tokens);

        ((org.mockbukkit.mockbukkit.scheduler.BukkitSchedulerMock) spyServer.getScheduler()).performOneTick();

        verify(spyServer).dispatchCommand(org.mockito.ArgumentMatchers.any(), eq("msg alice hello"));

        serverField.set(null, original);
    }
}
