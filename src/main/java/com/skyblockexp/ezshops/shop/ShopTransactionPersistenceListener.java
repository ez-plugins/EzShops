package com.skyblockexp.ezshops.shop;

import com.skyblockexp.ezshops.event.ShopPurchaseEvent;
import com.skyblockexp.ezshops.event.ShopSaleEvent;
import com.skyblockexp.ezshops.repository.transaction.TransactionRecord;
import com.skyblockexp.ezshops.repository.transaction.TransactionRepository;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.UUID;

/** Listens to in-game shop transactions and persists them via a repository. */
public final class ShopTransactionPersistenceListener implements Listener {

    private final TransactionRepository repository;

    public ShopTransactionPersistenceListener(TransactionRepository repository) {
        this.repository = repository;
    }

    @EventHandler
    public void onSale(ShopSaleEvent ev) {
        try {
            YamlConfiguration c = new YamlConfiguration();
            c.set("item", ev.getItem());
            String yaml = c.saveToString();
            TransactionRecord rec = new TransactionRecord(System.currentTimeMillis(), TransactionRecord.Type.SALE,
                    ev.getPlayer() != null ? ev.getPlayer().getUniqueId() : null,
                    yaml, ev.getAmount(), ev.getTotal());
            repository.record(rec);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @EventHandler
    public void onPurchase(ShopPurchaseEvent ev) {
        try {
            YamlConfiguration c = new YamlConfiguration();
            c.set("item", ev.getItem());
            String yaml = c.saveToString();
            TransactionRecord rec = new TransactionRecord(System.currentTimeMillis(), TransactionRecord.Type.PURCHASE,
                    ev.getPlayer() != null ? ev.getPlayer().getUniqueId() : null,
                    yaml, ev.getAmount(), ev.getTotal());
            repository.record(rec);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
