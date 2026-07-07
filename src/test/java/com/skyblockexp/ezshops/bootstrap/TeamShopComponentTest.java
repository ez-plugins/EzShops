package com.skyblockexp.ezshops.bootstrap;

import com.skyblockexp.ezshops.EzShopsPlugin;
import com.skyblockexp.ezshops.teams.TeamMarketManager;
import com.skyblockexp.ezshops.teams.TeamStockManager;
import com.skyblockexp.ezshops.teams.TeamTreasury;
import com.skyblockexp.ezshops.teams.TeamsIntegration;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamShopComponentTest {

    @Test
    void enable_skips_when_config_disables_teams_integration() {
        Economy economy = mock(Economy.class);
        TeamShopComponent component = new TeamShopComponent(economy);

        EzShopsPlugin plugin = mock(EzShopsPlugin.class);
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("teams-integration.enabled", false);
        when(plugin.getConfig()).thenReturn(cfg);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("TeamShopComponentTest"));

        component.enable(plugin);

        assertFalse(component.isEnabled());
        assertNull(component.getTeamsIntegration());
        assertNull(component.getTeamStockManager());
        assertNull(component.getTeamTreasury());
        assertNull(component.getTeamMarketManager());
    }

    @Test
    void disable_clears_state_and_disables_market_manager() throws Exception {
        Economy economy = mock(Economy.class);
        TeamShopComponent component = new TeamShopComponent(economy);

        TeamMarketManager marketManager = mock(TeamMarketManager.class);
        setField(component, "teamMarketManager", marketManager);
        setField(component, "teamsIntegration", mock(TeamsIntegration.class));
        setField(component, "teamStockManager", mock(TeamStockManager.class));
        setField(component, "teamTreasury", mock(TeamTreasury.class));
        setField(component, "enabled", true);

        component.disable();

        verify(marketManager).onDisable();
        assertFalse(component.isEnabled());
        assertNull(component.getTeamsIntegration());
        assertNull(component.getTeamStockManager());
        assertNull(component.getTeamTreasury());
        assertNull(component.getTeamMarketManager());
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
