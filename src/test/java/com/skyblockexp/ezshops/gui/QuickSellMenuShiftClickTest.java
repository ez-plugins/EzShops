package com.skyblockexp.ezshops.gui;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.bootstrap.CoreShopComponent;
import com.skyblockexp.ezshops.gui.quicksell.QuickSellMenu;
import com.skyblockexp.ezshops.shop.ShopTransactionService;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class QuickSellMenuShiftClickTest extends AbstractEzShopsTest {

    @Test
    void confirm_sells_items_present_only_in_gui_not_in_player_inventory() throws Exception {
        Economy econ = mock(Economy.class);
        when(econ.format(anyDouble())).thenReturn("$0.00");
        when(econ.depositPlayer(any(Player.class), anyDouble()))
                .thenReturn(new EconomyResponse(0.0, 1000.0, EconomyResponse.ResponseType.SUCCESS, "ok"));
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        CoreShopComponent core = plugin.getCoreShopComponent();
        assertNotNull(core);

        Field quickSellField = CoreShopComponent.class.getDeclaredField("quickSellMenu");
        quickSellField.setAccessible(true);
        QuickSellMenu quickSellMenu = (QuickSellMenu) quickSellField.get(core);
        assertNotNull(quickSellMenu);

        Player player = server.addPlayer("shift-click-player");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        quickSellMenu.open(player);

        Inventory guiInv = player.getOpenInventory().getTopInventory();
        assertNotNull(guiInv);

        player.getInventory().remove(Material.DIAMOND);
        assertEquals(0, countMaterial(player, Material.DIAMOND));

        guiInv.setItem(0, new ItemStack(Material.DIAMOND, 16));

        Method handleConfirm = QuickSellMenu.class.getDeclaredMethod("handleConfirm", Player.class, Inventory.class);
        handleConfirm.setAccessible(true);
        handleConfirm.invoke(quickSellMenu, player, guiInv);

        verify(econ, atLeastOnce()).depositPlayer(eq(player), anyDouble());

        ItemStack remaining = guiInv.getItem(0);
        assertTrue(remaining == null || remaining.getType() == Material.AIR,
                "GUI slot should be empty after a successful sale");
    }

    @Test
    void confirm_sells_multiple_stacks_in_gui_at_once() throws Exception {
        Economy econ = mock(Economy.class);
        when(econ.format(anyDouble())).thenReturn("$0.00");
        when(econ.depositPlayer(any(Player.class), anyDouble()))
                .thenReturn(new EconomyResponse(0.0, 1000.0, EconomyResponse.ResponseType.SUCCESS, "ok"));
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        CoreShopComponent core = plugin.getCoreShopComponent();
        Field quickSellField = CoreShopComponent.class.getDeclaredField("quickSellMenu");
        quickSellField.setAccessible(true);
        QuickSellMenu quickSellMenu = (QuickSellMenu) quickSellField.get(core);
        assertNotNull(quickSellMenu);

        Player player = server.addPlayer("multi-stack-player");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        quickSellMenu.open(player);
        Inventory guiInv = player.getOpenInventory().getTopInventory();

        player.getInventory().remove(Material.DIAMOND);
        guiInv.setItem(0, new ItemStack(Material.DIAMOND, 8));
        guiInv.setItem(5, new ItemStack(Material.DIAMOND, 16));

        Method handleConfirm = QuickSellMenu.class.getDeclaredMethod("handleConfirm", Player.class, Inventory.class);
        handleConfirm.setAccessible(true);
        handleConfirm.invoke(quickSellMenu, player, guiInv);

        verify(econ, atLeast(2)).depositPlayer(eq(player), anyDouble());

        ItemStack slot0 = guiInv.getItem(0);
        ItemStack slot5 = guiInv.getItem(5);
        assertTrue(slot0 == null || slot0.getType() == Material.AIR, "Slot 0 should be empty");
        assertTrue(slot5 == null || slot5.getType() == Material.AIR, "Slot 5 should be empty");
    }

    /**
     * When items ARE present in the GUI but sellDirect fails for a real reason (e.g. the economy
     * rejects the deposit), handleConfirm must show the actual failure reason, not the misleading
     * "No items to sell." message.
     *
     * <p>This is a regression test for the secondary symptom of GitHub issue
     * "sell GUI sometimes fails to detect items added with shift-click":
     * after a first successful sale drives dynamic pricing down to $0, the second
     * attempt returns an invalidSellPrice failure and the player would incorrectly
     * see "Nothing to sell" even though items are clearly visible in the GUI.
     */
    @Test
    void confirm_shows_sell_failure_reason_not_nothing_to_sell_when_economy_rejects_deposit() throws Exception {
        Economy econ = mock(Economy.class);
        when(econ.format(anyDouble())).thenReturn("$0.00");
        // Economy deliberately fails — simulates the deposit being rejected
        when(econ.depositPlayer(any(Player.class), anyDouble()))
                .thenReturn(new EconomyResponse(0.0, 0.0, EconomyResponse.ResponseType.FAILURE, "Bank offline"));
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        CoreShopComponent core = plugin.getCoreShopComponent();
        assertNotNull(core);

        Field quickSellField = CoreShopComponent.class.getDeclaredField("quickSellMenu");
        quickSellField.setAccessible(true);
        QuickSellMenu quickSellMenu = (QuickSellMenu) quickSellField.get(core);
        assertNotNull(quickSellMenu);

        Player player = server.addPlayer("economy-fail-player");
        player.addAttachment(plugin, ShopTransactionService.PERMISSION_SELL, true);
        quickSellMenu.open(player);

        Inventory guiInv = player.getOpenInventory().getTopInventory();
        assertNotNull(guiInv);

        // Items are in the GUI only (simulates shift-click)
        player.getInventory().remove(Material.DIAMOND);
        guiInv.setItem(0, new ItemStack(Material.DIAMOND, 16));

        Method handleConfirm = QuickSellMenu.class.getDeclaredMethod("handleConfirm", Player.class, Inventory.class);
        handleConfirm.setAccessible(true);
        handleConfirm.invoke(quickSellMenu, player, guiInv);

        // Economy was reached — items in the GUI were found and a sell was attempted
        verify(econ, atLeastOnce()).depositPlayer(eq(player), anyDouble());

        // The item must still be in the GUI slot (sell failed → not cleared)
        ItemStack remaining = guiInv.getItem(0);
        assertNotNull(remaining, "GUI slot should still have the item after a failed sell");
        assertNotEquals(Material.AIR, remaining.getType(), "GUI slot should still have the item after a failed sell");

        // The player must NOT see "No items to sell." — that message is only for an actually empty GUI.
        // The real failure reason (transaction failed) should be shown instead.
        String message = ((org.mockbukkit.mockbukkit.entity.PlayerMock) player).nextMessage();
        assertNotNull(message, "Player should have received an error message");
        assertFalse(message.contains("No items to sell"),
                "Expected the sell-failure reason to be shown, but got 'No items to sell': " + message);
    }

    private static int countMaterial(Player player, Material material) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && stack.getType() == material) {
                count += stack.getAmount();
            }
        }
        return count;
    }
}
