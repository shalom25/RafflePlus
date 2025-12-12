package com.raffleplus.managers;

import com.raffleplus.RafflePlus;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {

    // Economy Management Wrapper
    private static Economy econ = null;

    public static boolean setupEconomy() {
        if (RafflePlus.getInstance().getServer().getPluginManager().getPlugin("Vault") == null &&
            RafflePlus.getInstance().getServer().getPluginManager().getPlugin("Vault2") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = RafflePlus.getInstance().getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public static Economy getEconomy() {
        return econ;
    }

    public static boolean hasEconomy() {
        return econ != null;
    }
}
