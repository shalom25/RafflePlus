package com.raffleplus.commands;

import com.raffleplus.RafflePlus;
import com.raffleplus.managers.GameManager;
import com.raffleplus.managers.GuiManager;
import com.raffleplus.managers.LanguageManager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RaffleCommand implements CommandExecutor {

    private final RafflePlus plugin;
    private final GuiManager guiManager;
    private final LanguageManager lang = LanguageManager.getInstance();

    public RaffleCommand(RafflePlus plugin) {
        this.plugin = plugin;
        this.guiManager = new GuiManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        // --- RELOAD COMMAND (Console or Player) ---
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("r.reload")) {
                sender.sendMessage(lang.getMessage("messages.no-permission"));
                return true;
            }
            plugin.reloadConfig();
            sender.sendMessage(lang.getMessage("messages.reload-success"));
            return true;
        }

        // --- PLAYER ONLY COMMANDS ---
        if (!(sender instanceof Player)) {
            sender.sendMessage(lang.getMessage("messages.no-console"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0) {
            // /raffle sell -> Opens SELLER menu
            if (args[0].equalsIgnoreCase("sell")) {
                
                if (!player.hasPermission("r.sell")) {
                    player.sendMessage(lang.getMessage("messages.no-permission"));
                    return true;
                }

                // Check if player already has an active raffle
                if (GameManager.getInstance().getActiveRaffle(player) != null) {
                    guiManager.openSellerMenu(player);
                    return true;
                }

                ItemStack itemInHand = getItemInHand(player);

                if (!isValidItem(itemInHand)) {
                    player.sendMessage(lang.getMessage("messages.must-hold-item"));
                    return true;
                }

                // Initialize Setup Session
                GameManager.getInstance().startSetup(player, itemInHand);
                guiManager.openSellerMenu(player);
                return true;
            }
        }

        // /raffle (no args) -> Opens BUYER menu
        if (GameManager.getInstance().getActiveRaffles().isEmpty()) {
            player.sendMessage(lang.getMessage("messages.no-active-raffle"));
            return true;
        }
        guiManager.openBuyerMenu(player, 0);
        return true;
    }

    @SuppressWarnings("deprecation")
    private ItemStack getItemInHand(Player player) {
        try {
            // 1.9+ method
            ItemStack main = player.getInventory().getItemInMainHand();
            // Use XMaterial.matchXMaterial to safely check type, but checking null/amount is simpler first
            if (isValidItem(main)) {
                return main;
            }
            // Check Offhand if main hand is empty
            return player.getInventory().getItemInOffHand();
        } catch (NoSuchMethodError e) {
            // 1.8 method
            return player.getInventory().getItemInHand();
        }
    }

    private boolean isValidItem(ItemStack item) {
        if (item == null) return false;
        if (item.getAmount() <= 0) return false;
        
        // Simple string check for AIR to be robust across versions (1.8 - 1.21)
        String typeName = item.getType().name();
        return !typeName.equals("AIR") && !typeName.endsWith("_AIR");
    }
}
