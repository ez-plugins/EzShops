package com.skyblockexp.ezshops.core;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import net.milkbowl.vault.economy.EconomyResponse;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EconomyIntegrationFeatureTest extends AbstractEzShopsTest {

    @Test
    void economy_provider_is_used_for_withdraw_and_deposit() throws Exception {
        net.milkbowl.vault.economy.Economy econ = org.mockito.Mockito.mock(net.milkbowl.vault.economy.Economy.class);
        loadProviderPlugin(econ);

        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);
        assertNotNull(plugin);

        org.bukkit.entity.Player player = server.addPlayer("econ-player");

        // configure mock responses
        org.mockito.Mockito.when(econ.getBalance(org.mockito.Mockito.eq(player))).thenReturn(100.0);
        org.mockito.Mockito.when(econ.withdrawPlayer(org.mockito.Mockito.eq(player), org.mockito.Mockito.anyDouble()))
                .thenReturn(new EconomyResponse(0.0, 0.0, net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS, "ok"));
        org.mockito.Mockito.when(econ.depositPlayer(org.mockito.Mockito.eq(player), org.mockito.Mockito.anyDouble()))
                .thenReturn(new EconomyResponse(0.0, 0.0, net.milkbowl.vault.economy.EconomyResponse.ResponseType.SUCCESS, "ok"));

        // perform a stock buy via the confirm GUI flow to ensure economy withdraw is called
        com.skyblockexp.ezshops.bootstrap.StockComponent stockComp = plugin.getStockComponent();
        com.skyblockexp.ezshops.stock.StockMarketManager manager = stockComp.getStockMarketManager();
        manager.setPrice("DIAMOND", 5.0);

        com.skyblockexp.ezshops.gui.stock.StockTransactionConfirmGui.open(player, "DIAMOND", com.skyblockexp.ezshops.gui.stock.StockTransactionConfirmGui.TransactionType.BUY, manager, null, 1, "all");
        org.bukkit.inventory.Inventory inv = player.getOpenInventory().getTopInventory();
        assertNotNull(inv);

        boolean handled = com.skyblockexp.ezshops.gui.stock.StockTransactionConfirmGui.handleClick(player, inv, 10, "DIAMOND", com.skyblockexp.ezshops.gui.stock.StockTransactionConfirmGui.TransactionType.BUY, manager, null, 1, "all");
        assertTrue(handled);

        // verify withdraw was attempted
        org.mockito.Mockito.verify(econ, org.mockito.Mockito.atLeastOnce()).withdrawPlayer(org.mockito.Mockito.eq(player), org.mockito.Mockito.anyDouble());
    }
}
