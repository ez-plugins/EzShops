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
