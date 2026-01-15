
package com.skyblockexp.ezshops.shop.sign;

import com.skyblockexp.ezshops.common.CompatibilityUtil;
import com.skyblockexp.ezshops.shop.ShopPricingManager;
import com.skyblockexp.ezshops.config.ShopSignConfiguration;
import com.skyblockexp.ezshops.shop.ShopSignListener;
import com.skyblockexp.ezshops.shop.ShopTransactionService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Scans for legacy shop signs placed by other plugins and converts them to EzShops formatting.
 */
public final class SignShopScanner {

    public static final String PERMISSION = "ezshops.shop.sign.scan";

    public static final int MAX_RESULTS = 64;
    private static final long PENDING_EXPIRY_MILLIS = TimeUnit.MINUTES.toMillis(2);

    private final ShopPricingManager pricingManager;
    private final SignShopGenerator generator;
    private final ShopSignConfiguration signConfiguration;
    private final NamespacedKey actionKey;
    private final NamespacedKey materialKey;
    private final NamespacedKey amountKey;

    private final Map<UUID, PendingScan> pendingScans = new HashMap<>();

    public SignShopScanner(JavaPlugin plugin, ShopPricingManager pricingManager, SignShopGenerator generator,
            ShopSignConfiguration signConfiguration) {
        this.pricingManager = Objects.requireNonNull(pricingManager, "pricingManager");
        this.generator = Objects.requireNonNull(generator, "generator");
        this.signConfiguration = Objects.requireNonNull(signConfiguration, "signConfiguration");
        this.actionKey = new NamespacedKey(plugin, "shop_sign_action");
        this.materialKey = new NamespacedKey(plugin, "shop_sign_material");
        this.amountKey = new NamespacedKey(plugin, "shop_sign_amount");
    }

    public ScanResult scan(Player player, int radius) {
        if (player == null || radius <= 0) {
            return ScanResult.empty();
        }
        Location center = player.getLocation();
        World world = center.getWorld();
        if (world == null) {
            return ScanResult.empty();
        }

        Map<String, Material> materialLookup = buildMaterialLookup();
        List<DetectedSign> detected = new ArrayList<>();
        boolean limited = false;

        int originX = center.getBlockX();
        int originY = center.getBlockY();
        int originZ = center.getBlockZ();
        int minY = Math.max(world.getMinHeight(), originY - radius);
        int maxY = Math.min(world.getMaxHeight() - 1, originY + radius);

        outer: for (int x = originX - radius; x <= originX + radius; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = originZ - radius; z <= originZ + radius; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    BlockState state = block.getState();
                    if (!(state instanceof Sign sign)) {
                        continue;
                    }
                    if (isEzShopsSign(sign)) {
                        continue;
                    }
                    DetectedSign candidate = detectSign(sign, materialLookup);
                    if (candidate == null) {
                        continue;
                    }
                    if (!generator.isActionSupported(candidate.action(), candidate.material())) {
                        continue;
                    }
                    detected.add(candidate);
                    if (detected.size() >= MAX_RESULTS) {
                        limited = true;
                        break outer;
                    }
                }
            }
        }

        if (detected.isEmpty()) {
            return ScanResult.empty();
        }

        PendingScan pending = new PendingScan(List.copyOf(detected));
        PendingScan previous = pendingScans.put(player.getUniqueId(), pending);
        boolean overwritten = previous != null;

        return new ScanResult(pending.signs(), overwritten, limited);
    }

    public ConfirmationResult confirm(Player player) {
        if (player == null) {
            return ConfirmationResult.nonePending();
        }
        PendingScan pending = pendingScans.remove(player.getUniqueId());
        if (pending == null) {
            return ConfirmationResult.nonePending();
        }
        if (pending.isExpired()) {
            return ConfirmationResult.expired();
        }

        int converted = 0;
        int failed = 0;
        for (DetectedSign detected : pending.signs()) {
            if (convertSign(detected)) {
                converted++;
            } else {
                failed++;
            }
        }
        if (converted <= 0 && failed <= 0) {
            return ConfirmationResult.noneConverted();
        }
        if (converted > 0 && failed == 0) {
            return ConfirmationResult.success(converted);
        }
        if (converted > 0) {
            return ConfirmationResult.partial(converted, failed);
        }
        return ConfirmationResult.noneConverted();
    }

    public boolean cancel(Player player) {
        if (player == null) {
            return false;
        }
        return pendingScans.remove(player.getUniqueId()) != null;
    }

    private boolean convertSign(DetectedSign detected) {
        Location location = detected.location();
        World world = location.getWorld();
        if (world == null) {
            return false;
        }
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return false;
        }
        Block block = world.getBlockAt(location);
        BlockState state = block.getState();
        if (!(state instanceof Sign sign)) {
            return false;
        }
        if (!generator.isActionSupported(detected.action(), detected.material())) {
            return false;
        }
        int amount = Math.max(1, detected.amount());
        return generator.formatExistingSign(sign, detected.action(), detected.material(), amount);
    }

    private boolean isEzShopsSign(Sign sign) {
        PersistentDataContainer container = CompatibilityUtil.getPersistentDataContainer(sign);
        return CompatibilityUtil.hasKey(container, actionKey, PersistentDataType.STRING)
                || CompatibilityUtil.hasKey(container, materialKey, PersistentDataType.STRING)
                || CompatibilityUtil.hasKey(container, amountKey, PersistentDataType.INTEGER);
    }

    private DetectedSign detectSign(Sign sign, Map<String, Material> materialLookup) {
        String[] lines = new String[4];
        for (int i = 0; i < 4; i++) {
            lines[i] = strip(sign.getLine(i));
        }
        if (!isLikelyShopSign(lines)) {
            return null;
        }

        ShopSignListener.SignAction action = detectAction(lines);
        if (action == null) {
            return null;
        }

        Material material = detectMaterial(lines, materialLookup);
        if (material == null) {
            return null;
        }

        int amount = detectAmount(lines);
        if (amount <= 0) {
            amount = 1;
        }

        return new DetectedSign(sign.getLocation(), action, material, amount);
    }

    private Map<String, Material> buildMaterialLookup() {
        Set<Material> configured = pricingManager.getConfiguredMaterials();
        Map<String, Material> lookup = new HashMap<>(configured.size() * 2);
        for (Material material : configured) {
            addMaterialKey(lookup, material.name(), material);
            String friendly = ShopTransactionService.friendlyMaterialName(material);
            addMaterialKey(lookup, friendly, material);
        }
        return lookup;
    }

    private void addMaterialKey(Map<String, Material> lookup, String key, Material material) {
        String normalized = normalizeMaterialKey(key);
        if (!normalized.isEmpty()) {
            lookup.putIfAbsent(normalized, material);
        }
    }

    private Material detectMaterial(String[] lines, Map<String, Material> lookup) {
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line == null || line.isEmpty()) {
                continue;
            }
            Material direct = matchConfiguredMaterial(line);
            if (direct != null) {
                return direct;
            }
            String normalized = normalizeMaterialKey(line);
            Material viaLookup = lookup.get(normalized);
            if (viaLookup != null) {
                return viaLookup;
            }
            Material fromTokens = detectMaterialFromTokens(line, lookup);
            if (fromTokens != null) {
                return fromTokens;
            }
        }
        return null;
    }

    private Material detectMaterialFromTokens(String line, Map<String, Material> lookup) {
        String[] tokens = line.split("[^A-Za-z0-9]+");
        for (int start = 0; start < tokens.length; start++) {
            String token = tokens[start];
            if (token == null || token.isEmpty()) {
                continue;
            }
            Material direct = matchConfiguredMaterial(token);
            if (direct != null) {
                return direct;
            }
            String combined = token;
            for (int end = start + 1; end < tokens.length; end++) {
                String part = tokens[end];
                if (part == null || part.isEmpty()) {
                    continue;
                }
                combined = combined + '_' + part;
                direct = matchConfiguredMaterial(combined);
                if (direct != null) {
                    return direct;
                }
                String normalized = normalizeMaterialKey(combined);
                Material viaLookup = lookup.get(normalized);
                if (viaLookup != null) {
                    return viaLookup;
                }
            }
        }
        return null;
    }

    private Material matchConfiguredMaterial(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        Material material = Material.matchMaterial(token, false);
        if (material != null && pricingManager.isConfigured(material)) {
            return material;
        }
        String normalized = token.replace(' ', '_');
        material = Material.matchMaterial(normalized, false);
        if (material != null && pricingManager.isConfigured(material)) {
            return material;
        }
        normalized = token.replace('_', ' ');
        material = Material.matchMaterial(normalized, false);
        if (material != null && pricingManager.isConfigured(material)) {
            return material;
        }
        normalized = token.toUpperCase(Locale.ENGLISH);
        material = Material.matchMaterial(normalized, false);
        if (material != null && pricingManager.isConfigured(material)) {
            return material;
        }
        normalized = token.toLowerCase(Locale.ENGLISH).replace(' ', '_');
        material = Material.matchMaterial(normalized, false);
        if (material != null && pricingManager.isConfigured(material)) {
            return material;
        }
        return null;
    }

    private ShopSignListener.SignAction detectAction(String[] lines) {
        for (int i = 0; i < lines.length; i++) {
            ShopSignListener.SignAction action = parseActionToken(lines[i]);
            if (action != null) {
                return action;
            }
        }
        return null;
    }

    private ShopSignListener.SignAction parseActionToken(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ENGLISH);
        if (normalized.contains("buy") || normalized.startsWith("[buy")) {
            return ShopSignListener.SignAction.BUY;
        }
        if (normalized.contains("sell") || normalized.startsWith("[sell")) {
            return ShopSignListener.SignAction.SELL;
        }
        if (normalized.equals("b") || normalized.startsWith("b ")) {
            return ShopSignListener.SignAction.BUY;
        }
        if (normalized.equals("s") || normalized.startsWith("s ")) {
            return ShopSignListener.SignAction.SELL;
        }
        return null;
    }

    private int detectAmount(String[] lines) {
        int amount = -1;
        for (int i = lines.length - 1; i >= 1; i--) {
            String line = lines[i];
            if (line == null || line.isBlank()) {
                continue;
            }
            String normalized = line.toLowerCase(Locale.ENGLISH);
            if (containsPriceIndicator(normalized)) {
                continue;
            }
            int value = extractFirstInteger(normalized);
            if (value > 0) {
                if (normalized.contains("x")) {
                    return value;
                }
                if (value <= 64) {
                    return value;
                }
                if (amount < 0) {
                    amount = value;
                }
            }
        }
        return amount;
    }

    private boolean containsPriceIndicator(String line) {
        return line.contains("$") || line.contains("€") || line.contains(":")
                || line.contains("buy") || line.contains("sell") || line.contains("price")
                || line.startsWith("b ") || line.startsWith("s ");
    }

    private int extractFirstInteger(String line) {
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            } else if (digits.length() > 0) {
                break;
            }
        }
        if (digits.length() == 0) {
            return -1;
        }
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private boolean isLikelyShopSign(String[] lines) {
        if (lines == null || lines.length == 0) {
            return false;
        }
        boolean keywordDetected = false;
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            if (signConfiguration.matchesHeader(line)) {
                return true;
            }
            String normalized = line.trim().toLowerCase(Locale.ENGLISH);
            if (!keywordDetected && (normalized.contains("shop") || normalized.contains("buy")
                    || normalized.contains("sell"))) {
                keywordDetected = true;
            }
        }
        return keywordDetected;
    }

    private String strip(String line) {
        if (line == null) {
            return null;
        }
        return ChatColor.stripColor(line);
    }

    private String normalizeMaterialKey(String raw) {
        if (raw == null) {
            return "";
        }
        String stripped = ChatColor.stripColor(raw);
        if (stripped == null) {
            return "";
        }
        String lower = stripped.toLowerCase(Locale.ENGLISH);
        StringBuilder builder = new StringBuilder(lower.length());
        boolean underscore = false;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                builder.append(c);
                underscore = false;
            } else {
                if (!underscore && builder.length() > 0) {
                    builder.append('_');
                    underscore = true;
                }
            }
        }
        int length = builder.length();
        while (length > 0 && builder.charAt(length - 1) == '_') {
            builder.deleteCharAt(length - 1);
            length--;
        }
        if (builder.length() == 0) {
            return "";
        }
        return builder.toString();
    }

    private static final class PendingScan {

        private final List<DetectedSign> signs;
        private final long createdAt;

        private PendingScan(List<DetectedSign> signs) {
            this.signs = signs;
            this.createdAt = System.currentTimeMillis();
        }

        private boolean isExpired() {
            return System.currentTimeMillis() - createdAt > PENDING_EXPIRY_MILLIS;
        }

        private List<DetectedSign> signs() {
            return signs;
        }
    }

    public record DetectedSign(Location location, ShopSignListener.SignAction action, Material material, int amount) {
    }

    public record ScanResult(List<DetectedSign> signs, boolean overwritten, boolean limited) {

        private static final ScanResult EMPTY = new ScanResult(List.of(), false, false);

        public static ScanResult empty() {
            return EMPTY;
        }

        public boolean isEmpty() {
            return signs == null || signs.isEmpty();
        }
    }

    public enum ConfirmationStatus {
        SUCCESS,
        PARTIAL,
        NONE,
        EXPIRED,
        NO_PENDING
    }

    public record ConfirmationResult(ConfirmationStatus status, int converted, int failed) {

        private static final ConfirmationResult NONE = new ConfirmationResult(ConfirmationStatus.NO_PENDING, 0, 0);
        private static final ConfirmationResult EXPIRED_RESULT = new ConfirmationResult(ConfirmationStatus.EXPIRED, 0, 0);
        private static final ConfirmationResult NONE_CONVERTED = new ConfirmationResult(ConfirmationStatus.NONE, 0, 0);

        public static ConfirmationResult nonePending() {
            return NONE;
        }

        public static ConfirmationResult expired() {
            return EXPIRED_RESULT;
        }

        public static ConfirmationResult noneConverted() {
            return NONE_CONVERTED;
        }

        public static ConfirmationResult success(int converted) {
            return new ConfirmationResult(ConfirmationStatus.SUCCESS, converted, 0);
        }

        public static ConfirmationResult partial(int converted, int failed) {
            return new ConfirmationResult(ConfirmationStatus.PARTIAL, converted, failed);
        }
    }
}
