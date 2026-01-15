package com.skyblockexp.ezshops.shop.sign;

import com.skyblockexp.ezshops.common.CompatibilityUtil;
import com.skyblockexp.ezshops.common.EconomyUtils;
import com.skyblockexp.ezshops.shop.ShopPrice;
import com.skyblockexp.ezshops.shop.ShopPricingManager;
import com.skyblockexp.ezshops.config.ShopSignConfiguration;
import com.skyblockexp.ezshops.shop.ShopSignListener;
import com.skyblockexp.ezshops.shop.ShopTransactionService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Responsible for turning a {@link SignShopPlan} into placed sign shops in the world.
 */
public final class SignShopGenerator {

    private static final int MAX_TARGET_DISTANCE = 5;

    private final JavaPlugin plugin;
    private final ShopPricingManager pricingManager;
    private final ShopTransactionService transactionService;
    private final ShopSignConfiguration signConfiguration;
    private final NamespacedKey actionKey;
    private final NamespacedKey materialKey;
    private final NamespacedKey amountKey;

    public SignShopGenerator(JavaPlugin plugin, ShopPricingManager pricingManager,
            ShopTransactionService transactionService, ShopSignConfiguration signConfiguration) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.pricingManager = Objects.requireNonNull(pricingManager, "pricingManager");
        this.transactionService = Objects.requireNonNull(transactionService, "transactionService");
        this.signConfiguration = Objects.requireNonNull(signConfiguration, "signConfiguration");
        this.actionKey = new NamespacedKey(plugin, "shop_sign_action");
        this.materialKey = new NamespacedKey(plugin, "shop_sign_material");
        this.amountKey = new NamespacedKey(plugin, "shop_sign_amount");
    }

    /**
     * Attempts to generate the configured sign shop layout in front of the player.
     */
    public GenerationResult generate(Player player, SignShopPlan plan) {
        if (player == null) {
            return GenerationResult.failure("Only players can generate sign shops.");
        }
        if (plan == null || plan.isEmpty()) {
            return GenerationResult.failure("Add at least one item to the sign shop plan.");
        }

        Block target = player.getTargetBlockExact(MAX_TARGET_DISTANCE);
        if (target == null) {
            return GenerationResult.failure("Look at a block within range to use as the backing for the first sign shop.");
        }

        BlockFace facing = normalizeHorizontal(player.getFacing().getOppositeFace());
        BlockFace lateral = resolveLateral(facing, plan.direction());
        int horizontalSeparation = Math.max(1, plan.spacing() + 1);
        int verticalSeparation = Math.max(1, plan.rowSpacing() + 1);
        List<ItemStack> entries = plan.items();
        int rows = Math.max(1, plan.rows());
        int columns = Math.max(1, (int) Math.ceil(entries.size() / (double) rows));

        List<Block> backingBlocks = new ArrayList<>();
        List<Block> signBlocks = new ArrayList<>();
        List<Integer> rowIndices = new ArrayList<>();

        int itemIndex = 0;
        for (int row = 0; row < rows && itemIndex < entries.size(); row++) {
            Block rowOrigin = target.getRelative(BlockFace.UP, row * verticalSeparation);
            for (int column = 0; column < columns && itemIndex < entries.size(); column++) {
                Block backingBlock = rowOrigin.getRelative(lateral, column * horizontalSeparation);
                backingBlocks.add(backingBlock);
                signBlocks.add(backingBlock.getRelative(facing));
                rowIndices.add(row);
                itemIndex++;
            }
        }

        if (signBlocks.isEmpty()) {
            return GenerationResult.failure(
                    "No sign shops were generated. Ensure there is space in front of you and that the items are valid.");
        }

        if (!ensureSignSpace(plan.signMaterial(), signBlocks.toArray(new Block[0]))) {
            return GenerationResult.failure(
                    "There isn't enough empty space in front of the backing blocks for the planned signs.");
        }

        int created = 0;
        Set<Integer> rowsUsed = new HashSet<>();
        for (int index = 0; index < signBlocks.size(); index++) {
            ItemStack item = entries.get(index);

            Block backingBlock = backingBlocks.get(index);
            placeBackground(backingBlock, plan.backgroundBlock());

            Block signBlock = signBlocks.get(index);
            Sign sign = placeSign(signBlock, facing, plan.signMaterial());
            if (sign == null) {
                continue;
            }

            int amount = Math.max(1, Math.min(64, item.getAmount()));
            Material material = item.getType();
            double totalPrice = resolveTotalPrice(plan.action(), material, amount);
            applySignText(sign, plan.action(), material, amount, totalPrice);
            storeSignData(sign, plan.action(), material, amount);
            created++;
            rowsUsed.add(rowIndices.get(index));
        }

        if (created <= 0) {
            return GenerationResult.failure("No sign shops were generated. Ensure there is space in front of you and that the items are valid.");
        }

        String extension = plan.direction() == SignShopPlan.LayoutDirection.RIGHT ? "right" : "left";
        int usedRows = Math.max(1, rowsUsed.size());
        String message = String.format(Locale.ENGLISH, "Created %d sign shop%s across %d row%s extending to the %s.",
                created, created == 1 ? "" : "s", usedRows, usedRows == 1 ? "" : "s", extension);
        return GenerationResult.success(message);
    }

    public boolean formatExistingSign(Sign sign, ShopSignListener.SignAction action, Material material, int amount) {
        if (sign == null || action == null || material == null) {
            return false;
        }
        if (!isActionSupported(action, material)) {
            return false;
        }
        int sanitizedAmount = Math.max(1, Math.min(64, amount));
        double totalPrice = resolveTotalPrice(action, material, sanitizedAmount);
        applySignText(sign, action, material, sanitizedAmount, totalPrice);
        storeSignData(sign, action, material, sanitizedAmount);
        return true;
    }

    /**
     * Returns {@code true} if the given material has a configured price for the provided action.
     */
    public boolean isActionSupported(ShopSignListener.SignAction action, Material material) {
        if (material == null) {
            return false;
        }
        Optional<ShopPrice> priceLookup = pricingManager.getPrice(material);
        if (priceLookup.isEmpty()) {
            return false;
        }
        ShopPrice price = priceLookup.get();
        double unit = action == ShopSignListener.SignAction.BUY ? price.buyPrice() : price.sellPrice();
        return unit >= 0.0d;
    }

    void placeBackground(Block backingBlock, Material backgroundMaterial) {
        if (backgroundMaterial == null || backingBlock == null) {
            return;
        }
        if (backingBlock.getType() != backgroundMaterial) {
            backingBlock.setType(backgroundMaterial, false);
        }
    }

    private boolean ensureSignSpace(Material signMaterial, Block... signBlocks) {
        if (signBlocks == null || signBlocks.length == 0) {
            return false;
        }
        Material targetMaterial = signMaterial == null ? Material.OAK_WALL_SIGN : signMaterial;
        for (Block signBlock : signBlocks) {
            if (signBlock == null) {
                return false;
            }
            Material type = signBlock.getType();
            if (type != Material.AIR && type != Material.CAVE_AIR && type != Material.VOID_AIR
                    && type != targetMaterial) {
                return false;
            }
        }
        return true;
    }

    private Sign placeSign(Block signBlock, BlockFace facing, Material signMaterial) {
        Material materialToUse = signMaterial == null ? Material.OAK_WALL_SIGN : signMaterial;
        signBlock.setType(materialToUse, false);
        BlockData data = signBlock.getBlockData();
        if (data instanceof WallSign wallSign) {
            wallSign.setFacing(facing);
            signBlock.setBlockData(wallSign, false);
        } else if (data instanceof Directional directional) {
            directional.setFacing(facing);
            signBlock.setBlockData(directional, false);
        }
        BlockState state = signBlock.getState();
        if (state instanceof Sign sign) {
            return sign;
        }
        return null;
    }

    private double resolveTotalPrice(ShopSignListener.SignAction action, Material material, int amount) {
        Optional<ShopPrice> priceLookup = pricingManager.getPrice(material);
        if (priceLookup.isEmpty()) {
            return -1.0d;
        }
        ShopPrice price = priceLookup.get();
        double unit = action == ShopSignListener.SignAction.BUY ? price.buyPrice() : price.sellPrice();
        if (unit < 0) {
            return -1.0d;
        }
        return EconomyUtils.normalizeCurrency(unit * Math.max(1, amount));
    }

    private void applySignText(Sign sign, ShopSignListener.SignAction action, Material material, int amount,
            double totalPrice) {
        sign.setLine(0, signConfiguration.headerText());
        sign.setLine(1, signConfiguration.formatActionLine(action, amount));
        sign.setLine(2, signConfiguration
                .formatItemLine(ShopTransactionService.friendlyMaterialName(material)));
        if (totalPrice < 0) {
            sign.setLine(3, signConfiguration.unavailableLine());
        } else {
            sign.setLine(3, signConfiguration
                    .formatPriceLine(transactionService.formatCurrency(totalPrice)));
        }
        sign.update(true);
    }

    private void storeSignData(Sign sign, ShopSignListener.SignAction action, Material material, int amount) {
        PersistentDataContainer container = CompatibilityUtil.getPersistentDataContainer(sign);
        CompatibilityUtil.set(container, actionKey, PersistentDataType.STRING, action.name());
        CompatibilityUtil.set(container, materialKey, PersistentDataType.STRING, material.name());
        CompatibilityUtil.set(container, amountKey, PersistentDataType.INTEGER, Math.max(1, amount));
        sign.update(true);
    }

    static BlockFace resolveLateral(BlockFace facing, SignShopPlan.LayoutDirection direction) {
        BlockFace normalized = normalizeHorizontal(facing);
        return direction == SignShopPlan.LayoutDirection.RIGHT ? rotateCounterClockwise(normalized)
                : rotateClockwise(normalized);
    }

    private static BlockFace normalizeHorizontal(BlockFace face) {
        if (face == null) {
            return BlockFace.SOUTH;
        }
        int x = face.getModX();
        int z = face.getModZ();
        if (Math.abs(x) >= Math.abs(z)) {
            return x >= 0 ? BlockFace.EAST : BlockFace.WEST;
        }
        return z >= 0 ? BlockFace.SOUTH : BlockFace.NORTH;
    }

    private static BlockFace rotateClockwise(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> BlockFace.EAST;
        };
    }

    private static BlockFace rotateCounterClockwise(BlockFace face) {
        return switch (face) {
            case NORTH -> BlockFace.WEST;
            case WEST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.EAST;
            case EAST -> BlockFace.NORTH;
            default -> BlockFace.WEST;
        };
    }

    /**
     * Represents the result of attempting to generate sign shops.
     */
    public record GenerationResult(boolean success, String message) {

        public static GenerationResult success(String message) {
            return new GenerationResult(true, message);
        }

        public static GenerationResult failure(String message) {
            return new GenerationResult(false, message);
        }
    }
}
