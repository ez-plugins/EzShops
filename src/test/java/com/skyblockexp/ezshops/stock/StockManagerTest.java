package com.skyblockexp.ezshops.stock;

import com.skyblockexp.ezshops.AbstractEzShopsTest;
import com.skyblockexp.ezshops.EzShopsPlugin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StockManagerTest extends AbstractEzShopsTest {

    @Test
    void player_stock_add_remove_and_query() throws Exception {
        EzShopsPlugin plugin = loadPlugin(EzShopsPlugin.class);

        org.bukkit.entity.Player player = server.addPlayer("stockplayer");

        // start clean by removing any existing file
        java.io.File data = plugin.getDataFolder();
        java.io.File playerFile = new java.io.File(data, "player-stocks/" + player.getUniqueId() + ".yml");
        if (playerFile.exists()) playerFile.delete();

        boolean added = StockManager.addPlayerStock(player, "DIAMOND", 5);
        assertTrue(added);
        int amt = StockManager.getPlayerStockAmount(player, "DIAMOND");
        assertEquals(5, amt);

        boolean removedTooMuch = StockManager.removePlayerStock(player, "DIAMOND", 10);
        assertFalse(removedTooMuch);

        boolean removed = StockManager.removePlayerStock(player, "DIAMOND", 5);
        assertTrue(removed);
        int after = StockManager.getPlayerStockAmount(player, "DIAMOND");
        assertEquals(0, after);

        // owned stocks empty
        java.util.List<String> owned = StockManager.getPlayerOwnedStocks(player);
        assertTrue(owned.isEmpty());
    }
}
