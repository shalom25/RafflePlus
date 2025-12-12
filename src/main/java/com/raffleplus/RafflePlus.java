package com.raffleplus;

import com.raffleplus.commands.RaffleCommand;
import com.raffleplus.listeners.ChatListener;
import com.raffleplus.listeners.RaffleListener;
import com.raffleplus.managers.DatabaseManager;
import com.raffleplus.managers.EconomyManager;
import com.raffleplus.managers.GameManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;

public class RafflePlus extends JavaPlugin {

    // Main Plugin Class
    private static RafflePlus instance;
    private DatabaseManager dbManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Load config
        saveDefaultConfig();

        // Initialize Database
        dbManager = new DatabaseManager(this);
        try {
            dbManager.connect();
            if (dbManager.isEnabled()) {
                getLogger().info("Successfully connected to MySQL database!");
            }
        } catch (SQLException e) {
            getLogger().severe("Could not connect to MySQL database! Disabling database support.");
            e.printStackTrace();
        }
        
        // Pass DB to GameManager and Load Data
        GameManager.getInstance().setDatabaseManager(dbManager);
        GameManager.getInstance().loadRafflesFromDb();

        // Load Language
        com.raffleplus.managers.LanguageManager.getInstance();

        // Setup Economy
        if (!EconomyManager.setupEconomy()) {
            getLogger().severe("Disabled due to no Vault dependency found!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register commands
        if (getCommand("raffle") != null) {
            getCommand("raffle").setExecutor(new RaffleCommand(this));
        }

        // Register listeners
        getServer().getPluginManager().registerEvents(new RaffleListener(), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        getLogger().info("RafflePlus has been enabled!");
    }

    @Override
    public void onDisable() {
        if (dbManager != null) {
            dbManager.disconnect();
        }
        getLogger().info("RafflePlus has been disabled!");
    }

    public void reloadConfig() {
        super.reloadConfig();
        com.raffleplus.managers.LanguageManager.getInstance().reload();
    }

    public static RafflePlus getInstance() {
        return instance;
    }
}
