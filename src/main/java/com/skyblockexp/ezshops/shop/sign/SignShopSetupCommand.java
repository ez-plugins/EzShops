
package com.skyblockexp.ezshops.shop.sign;

import com.skyblockexp.ezshops.config.ShopMessageConfiguration;
import com.skyblockexp.ezshops.shop.ShopSignListener;
import com.skyblockexp.ezshops.shop.ShopTransactionService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * Opens the sign shop setup GUI for authorized players and provides legacy sign conversion helpers.
 */
public final class SignShopSetupCommand implements CommandExecutor, TabCompleter {

    public static final String PERMISSION = "ezshops.shop.sign.setup";
    private static final int DEFAULT_RADIUS = 8;
    private static final int MIN_RADIUS = 1;
    private static final int MAX_RADIUS = 12;
    private static final int MAX_PREVIEW = 10;

    private final SignShopSetupMenu menu;
    private final SignShopScanner scanner;
    private final ShopMessageConfiguration.CommandMessages.SignShopScanCommandMessages messages;
    private final ShopMessageConfiguration.SignMessages signMessages;

    public SignShopSetupCommand(SignShopSetupMenu menu, SignShopScanner scanner,
            ShopMessageConfiguration.CommandMessages.SignShopScanCommandMessages messages,
            ShopMessageConfiguration.SignMessages signMessages) {
        this.menu = menu;
        this.scanner = scanner;
        this.messages = Objects.requireNonNull(messages, "messages");
        this.signMessages = Objects.requireNonNull(signMessages, "signMessages");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            if (args != null && args.length > 0) {
                sender.sendMessage(messages.playersOnly());
            } else {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            }
            return true;
        }

        if (args != null && args.length > 0) {
            String subcommand = args[0].toLowerCase(Locale.ENGLISH);
            switch (subcommand) {
                case "scan" -> handleScan(player, label, args);
                case "confirm" -> handleConfirm(player);
                case "cancel" -> handleCancel(player);
                default -> player.sendMessage(messages.unknown(label));
            }
            return true;
        }

        if (!player.hasPermission(PERMISSION)) {
            player.sendMessage(ChatColor.RED + "You do not have permission to use the sign shop setup menu.");
            return true;
        }
        if (menu == null) {
            player.sendMessage(ChatColor.RED + "The sign shop setup menu is currently unavailable.");
            return true;
        }
        menu.open(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if (player.hasPermission(SignShopScanner.PERMISSION)) {
                options.add("scan");
                options.add("confirm");
                options.add("cancel");
            }
            return filter(options, args[0]);
        }
        if (args.length == 2 && "scan".equalsIgnoreCase(args[0])
                && player.hasPermission(SignShopScanner.PERMISSION)) {
            return filter(List.of("5", "8", "10", Integer.toString(DEFAULT_RADIUS)), args[1]);
        }
        return Collections.emptyList();
    }

    private void handleScan(Player player, String label, String[] args) {
        if (scanner == null) {
            player.sendMessage(messages.scannerUnavailable());
            return;
        }
        if (!player.hasPermission(SignShopScanner.PERMISSION)) {
            player.sendMessage(messages.noPermission());
            return;
        }
        int radius = DEFAULT_RADIUS;
        if (args.length >= 2) {
            try {
                radius = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                player.sendMessage(messages.invalidRadius(MIN_RADIUS, MAX_RADIUS));
                return;
            }
        }
        if (radius < MIN_RADIUS || radius > MAX_RADIUS) {
            player.sendMessage(messages.invalidRadius(MIN_RADIUS, MAX_RADIUS));
            return;
        }

        SignShopScanner.ScanResult result = scanner.scan(player, radius);
        if (result.isEmpty()) {
            player.sendMessage(messages.noSigns(radius));
            return;
        }
        if (result.overwritten()) {
            player.sendMessage(messages.pendingReplaced());
        }
        player.sendMessage(messages.found(result.signs().size(), radius));
        int shown = 0;
        for (SignShopScanner.DetectedSign detected : result.signs()) {
            if (shown >= MAX_PREVIEW) {
                break;
            }
            String actionLabel = detected.action() == ShopSignListener.SignAction.BUY
                    ? signMessages.actionLabelBuy() : signMessages.actionLabelSell();
            Location location = detected.location();
            String itemName = ShopTransactionService.friendlyMaterialName(detected.material());
            player.sendMessage(messages.entry(actionLabel, detected.amount(), itemName,
                    location.getBlockX(), location.getBlockY(), location.getBlockZ()));
            shown++;
        }
        int remaining = result.signs().size() - shown;
        if (remaining > 0) {
            player.sendMessage(messages.more(remaining));
        }
        if (result.limited()) {
            player.sendMessage(messages.limit(SignShopScanner.MAX_RESULTS));
        }
        player.sendMessage(messages.confirmHint(label));
    }

    private void handleConfirm(Player player) {
        if (scanner == null) {
            player.sendMessage(messages.scannerUnavailable());
            return;
        }
        if (!player.hasPermission(SignShopScanner.PERMISSION)) {
            player.sendMessage(messages.noPermission());
            return;
        }
        SignShopScanner.ConfirmationResult result = scanner.confirm(player);
        switch (result.status()) {
            case NO_PENDING -> player.sendMessage(messages.noPending());
            case EXPIRED -> player.sendMessage(messages.expired());
            case SUCCESS -> player.sendMessage(messages.converted(result.converted()));
            case PARTIAL -> player.sendMessage(messages.convertedPartial(result.converted(), result.failed()));
            case NONE -> player.sendMessage(messages.convertedNone());
        }
    }

    private void handleCancel(Player player) {
        boolean menuCancelled = menu != null && menu.cancel(player);
        if (menuCancelled) {
            player.sendMessage(ChatColor.YELLOW + "Closed the sign shop planner.");
        }
        if (scanner == null) {
            if (!menuCancelled) {
                player.sendMessage(messages.scannerUnavailable());
            }
            return;
        }
        if (!player.hasPermission(SignShopScanner.PERMISSION)) {
            player.sendMessage(messages.noPermission());
            return;
        }
        if (scanner.cancel(player)) {
            player.sendMessage(messages.cancelled());
        } else {
            player.sendMessage(messages.noPending());
        }
    }

    private List<String> filter(List<String> options, String token) {
        if (options.isEmpty()) {
            return Collections.emptyList();
        }
        if (token == null || token.isEmpty()) {
            return options;
        }
        String lower = token.toLowerCase(Locale.ENGLISH);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ENGLISH).startsWith(lower)) {
                matches.add(option);
            }
        }
        return matches;
    }
}
