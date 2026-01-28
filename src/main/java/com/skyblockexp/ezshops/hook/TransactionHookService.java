package com.skyblockexp.ezshops.hook;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public class TransactionHookService {
    private final Plugin plugin;
    private final PlaceholderApiAdapter papiAdapter;
    private final String playerNoPermissionMessage;

    public TransactionHookService(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        // Attempt to initialize a PlaceholderAPI adapter if the plugin exists
        PlaceholderApiAdapter adapter = null;
        try {
            if (plugin.getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
                adapter = new PlaceholderApiAdapter(plugin);
                if (!adapter.isAvailable()) {
                    adapter = null;
                }
            }
        } catch (Throwable ignored) {
            adapter = null;
        }
        this.papiAdapter = adapter;
        String configured = null;
        try {
            Object cfg = plugin.getClass().getMethod("getConfig").invoke(plugin);
            if (cfg instanceof org.bukkit.configuration.file.FileConfiguration fileConfig) {
                configured = fileConfig.getString("hooks.player-lacking-permission-message", null);
                if (configured != null) {
                    try {
                        configured = com.skyblockexp.ezshops.config.ConfigTranslator.resolve(configured, null);
                    } catch (Throwable ignored) {
                        // ignore translator failures and keep raw value
                    }
                }
            }
        } catch (Throwable ignored) {
            configured = null;
        }
        this.playerNoPermissionMessage = configured;
    }

    public void executeHooks(Player player, List<String> commands, boolean runAsConsole, java.util.Map<String, String> tokens) {
        if (commands == null || commands.isEmpty()) return;
        List<String> formatted = new ArrayList<>(commands.size());
        for (String cmd : commands) {
            String replaced = replaceTokens(player, cmd, tokens);
            if (papiAdapter != null && player != null) {
                replaced = papiAdapter.apply(player, replaced);
            }
            formatted.add(replaced);
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            boolean effectiveConsole = runAsConsole;
            if (!effectiveConsole) {
                // require explicit permission to run commands as the player
                if (player == null || !player.hasPermission("ezshops.hooks.use")) {
                    plugin.getLogger().warning("Player does not have permission 'ezshops.hooks.use' — falling back to console for hook commands.");
                    // notify player if configured
                    if (player != null && playerNoPermissionMessage != null && !playerNoPermissionMessage.isBlank()) {
                        String msg = replaceTokens(player, playerNoPermissionMessage, tokens);
                        if (papiAdapter != null) {
                            msg = papiAdapter.apply(player, msg);
                        }
                        final String colored = org.bukkit.ChatColor.translateAlternateColorCodes('&', msg);
                        try {
                            player.sendMessage(colored);
                        } catch (Throwable ignored) {
                            // ignore messaging failures
                        }
                    }
                    effectiveConsole = true;
                }
            }

            for (String finalCmd : formatted) {
                try {
                    if (effectiveConsole) {
                        CommandSender console = plugin.getServer().getConsoleSender();
                        plugin.getServer().dispatchCommand(console, finalCmd);
                    } else {
                        plugin.getServer().dispatchCommand(player, finalCmd);
                    }
                } catch (Throwable ex) {
                    plugin.getLogger().log(Level.WARNING, "Failed to dispatch hook command: " + ex.getMessage(), ex);
                }
            }
        });
    }

    private String replaceTokens(Player player, String input, java.util.Map<String, String> tokens) {
        if (input == null) return null;
        String out = input;
        if (tokens != null) {
            for (java.util.Map.Entry<String, String> e : tokens.entrySet()) {
                out = out.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
            }
        }
        if (player != null) {
            out = out.replace("{player}", player.getName());
        }
        return out;
    }
}
