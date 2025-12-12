package com.raffleplus.listeners;

import com.raffleplus.RafflePlus;
import com.raffleplus.managers.GameManager;
import com.raffleplus.managers.GuiManager;
import com.raffleplus.managers.LanguageManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class ChatListener implements Listener {

    // Chat Event Listener
    private final RafflePlus plugin;
    private final GuiManager guiManager;
    private final LanguageManager lang = LanguageManager.getInstance();

    public ChatListener(RafflePlus plugin) {
        this.plugin = plugin;
        this.guiManager = new GuiManager();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!GameManager.getInstance().isEditingPrice(event.getPlayer())) return;

        event.setCancelled(true);
        String message = event.getMessage();
        
        try {
            double price = Double.parseDouble(message);
            if (price < 0) {
                event.getPlayer().sendMessage(lang.getMessage("messages.price-negative"));
                return;
            }

            // Update price
            GameManager.getInstance().getSetupSession(event.getPlayer()).setTicketPrice(price);
            GameManager.getInstance().setEditingPrice(event.getPlayer(), false);

            event.getPlayer().sendMessage(lang.getMessage("messages.price-set").replace("%price%", String.valueOf(price)));
            
            // Re-open menu (must be sync)
            new BukkitRunnable() {
                @Override
                public void run() {
                    guiManager.openSellerMenu(event.getPlayer());
                }
            }.runTask(plugin);

        } catch (NumberFormatException e) {
            event.getPlayer().sendMessage(lang.getMessage("messages.price-invalid"));
            if (message.equalsIgnoreCase("cancel")) {
                event.getPlayer().sendMessage(lang.getMessage("messages.price-cancel"));
                GameManager.getInstance().setEditingPrice(event.getPlayer(), false);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        guiManager.openSellerMenu(event.getPlayer());
                    }
                }.runTask(plugin);
            }
        }
    }
}
