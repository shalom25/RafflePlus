package com.raffleplus.managers;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.raffleplus.game.RaffleGame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GameManager {

    // Singleton Game Manager
    private static GameManager instance;
    private final List<RaffleGame> activeRaffles;
    // Store players who are currently setting up a raffle
    private final Map<UUID, RaffleGame> setupSessions;
    // Store players who are typing price in chat
    private final Map<UUID, RaffleGame> priceEditors;
    // Store which raffle a player is currently viewing in the purchase menu
    private final Map<UUID, RaffleGame> viewingRaffle;
    
    private DatabaseManager dbManager;

    private GameManager() {
        this.activeRaffles = new ArrayList<>();
        this.setupSessions = new HashMap<>();
        this.priceEditors = new HashMap<>();
        this.viewingRaffle = new HashMap<>();
    }

    public static synchronized GameManager getInstance() {
        if (instance == null) {
            instance = new GameManager();
        }
        return instance;
    }

    public void setDatabaseManager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }
    
    public void loadRafflesFromDb() {
        if (dbManager != null && dbManager.isEnabled()) {
            List<RaffleGame> loaded = dbManager.loadActiveRaffles();
            this.activeRaffles.clear();
            this.activeRaffles.addAll(loaded);
        }
    }

    public List<RaffleGame> getActiveRaffles() {
        return activeRaffles;
    }

    public RaffleGame getActiveRaffle(Player player) {
        for (RaffleGame game : activeRaffles) {
            if (game.getSellerUuid().equals(player.getUniqueId())) {
                return game;
            }
        }
        return null;
    }

    public void addActiveRaffle(RaffleGame raffle) {
        this.activeRaffles.add(raffle);
        if (dbManager != null) {
            dbManager.saveRaffle(raffle);
        }
    }

    public void removeActiveRaffle(RaffleGame raffle) {
        this.activeRaffles.remove(raffle);
        if (dbManager != null) {
             // We delete it from DB to keep it lightweight as requested
             dbManager.deleteRaffle(raffle.getRaffleId());
        }
    }
    
    public void saveRaffleState(RaffleGame raffle) {
        if (dbManager != null) {
            dbManager.saveRaffle(raffle);
        }
    }

    public void startSetup(Player player, ItemStack item) {
        RaffleGame game = new RaffleGame(player, item);
        setupSessions.put(player.getUniqueId(), game);
    }

    public RaffleGame getSetupSession(Player player) {
        return setupSessions.get(player.getUniqueId());
    }

    public void removeSetupSession(Player player) {
        setupSessions.remove(player.getUniqueId());
    }

    public void setEditingPrice(Player player, boolean editing) {
        if (editing) {
            RaffleGame game = setupSessions.get(player.getUniqueId());
            if (game != null) {
                priceEditors.put(player.getUniqueId(), game);
            }
        } else {
            priceEditors.remove(player.getUniqueId());
        }
    }

    public boolean isEditingPrice(Player player) {
        return priceEditors.containsKey(player.getUniqueId());
    }

    public void setViewingRaffle(Player player, RaffleGame game) {
        if (game == null) {
            viewingRaffle.remove(player.getUniqueId());
        } else {
            viewingRaffle.put(player.getUniqueId(), game);
        }
    }

    public RaffleGame getViewingRaffle(Player player) {
        return viewingRaffle.get(player.getUniqueId());
    }
}
