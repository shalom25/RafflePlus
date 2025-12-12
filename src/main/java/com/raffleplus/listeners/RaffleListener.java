package com.raffleplus.listeners;

import com.raffleplus.RafflePlus;
import com.raffleplus.game.RaffleGame;
import com.raffleplus.managers.GameManager;
import com.raffleplus.managers.GuiManager;
import com.raffleplus.managers.EconomyManager;
import com.raffleplus.managers.LanguageManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.inventory.Inventory;
import org.bukkit.FireworkEffect;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.Color;
import com.cryptomorin.xseries.XSound;

public class RaffleListener implements Listener {

    private final Random random = new Random();
    private final LanguageManager lang = LanguageManager.getInstance();
    private final GuiManager guiManager = new GuiManager();

    // Raffle Event Listener
    public RaffleListener() {
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        String title = event.getView().getTitle();
        // Since titles are dynamic, we check if the title matches what we expect from the CURRENT language
        if (!title.equals(guiManager.getSellerMenuTitle()) && 
            !title.equals(guiManager.getBuyerMenuTitle()) &&
            !title.equals(guiManager.getTicketMenuTitle())) return;

        event.setCancelled(true); // Prevent taking items
        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        int slot = event.getRawSlot();

        if (title.equals(guiManager.getSellerMenuTitle())) {
            handleSellerMenu(player, slot);
        } else if (title.equals(guiManager.getBuyerMenuTitle())) {
            handleBuyerMenu(player, slot, event.getInventory());
        } else if (title.equals(guiManager.getTicketMenuTitle())) {
            handleTicketMenu(player, slot);
        }
    }

    private void handleSellerMenu(Player player, int slot) {
        if (slot == 10) { // Set Price
            GameManager.getInstance().setEditingPrice(player, true);
            player.closeInventory();
            sendMessage(player, lang.getMessage("messages.type-price"));
            playSound(player, "click");
            
        } else if (slot == 16) { // Publish Raffle (Step 1)
            RaffleGame game = GameManager.getInstance().getSetupSession(player);
            if (game != null) {
                if (game.isActive()) {
                    sendMessage(player, lang.getMessage("messages.already-published"));
                    playSound(player, "error");
                    return;
                }

                // Check if player has the item to sell and remove it
                ItemStack prize = game.getPrize();
                if (!takePrize(player, prize)) {
                    sendMessage(player, lang.getMessage("messages.missing-item"));
                    playSound(player, "error");
                    return;
                }

                // Activate Purchase (Allow buying)
                game.setActive(true);
                GameManager.getInstance().addActiveRaffle(game);
                GameManager.getInstance().removeSetupSession(player); // Exit setup mode

                player.closeInventory();
                
                List<String> broadcast = lang.getBroadcastMessage("messages.published-broadcast");
                for (String line : broadcast) {
                    Bukkit.broadcastMessage(line
                        .replace("%item%", game.getPrize().getType().name())
                        .replace("%price%", String.valueOf(game.getTicketPrice())));
                }

                playSound(player, "publish");
            }

        } else if (slot == 14) { // Start Raffle (Step 2: Generate Number)
            RaffleGame game = null;
            // Find active raffle owned by player
            for (RaffleGame g : GameManager.getInstance().getActiveRaffles()) {
                if (g.getSeller().getUniqueId().equals(player.getUniqueId())) {
                    game = g;
                    break;
                }
            }
            
            if (game == null) {
                // Fallback: Check if they are still in setup
                game = GameManager.getInstance().getSetupSession(player);
                if (game != null && !game.isActive()) {
                     sendMessage(player, lang.getMessage("messages.must-publish"));
                     playSound(player, "error");
                     return;
                }
                if (game == null) {
                     sendMessage(player, lang.getMessage("messages.no-raffle-start"));
                     return;
                }
            }

            // If game is active (Published), allow Start
            if (game.getWinningNumber() != -1) {
                sendMessage(player, lang.getMessage("messages.winner-generated").replace("%number%", String.valueOf(game.getWinningNumber())));
                playSound(player, "click");
                return;
            }

            // Ensure tickets have been sold
            if (game.getTickets().isEmpty()) {
                sendMessage(player, lang.getMessage("messages.no-tickets-sold"));
                playSound(player, "error");
                return;
            }

            // Generate Winning Number from SOLD tickets only
            List<Integer> soldTickets = new java.util.ArrayList<>(game.getTickets().keySet());
            int winningNumber = soldTickets.get(random.nextInt(soldTickets.size()));
            
            game.setWinningNumber(winningNumber);

            List<String> endedMsg = lang.getBroadcastMessage("messages.raffle-ended");
            for (String line : endedMsg) {
                Bukkit.broadcastMessage(line.replace("%number%", String.valueOf(winningNumber)));
            }

            playSound(player, "win");

            // Guaranteed Winner Logic
            java.util.UUID winnerUUID = game.getTickets().get(winningNumber);
            Player winner = Bukkit.getPlayer(winnerUUID);
            String winnerName = (winner != null) ? winner.getName() : "Offline Player";

            List<String> winnerMsg = lang.getBroadcastMessage("messages.we-have-winner");
            for (String line : winnerMsg) {
                Bukkit.broadcastMessage(line
                    .replace("%winner%", winnerName)
                    .replace("%number%", String.valueOf(winningNumber)));
            }

            // Give Prize
            if (winner != null) {
                winner.getInventory().addItem(game.getPrize());
                playSound(winner, "win");
                spawnFirework(winner);
            } else {
                broadcastMessage(lang.getMessage("messages.winner-offline"));
                // NOTE: In a real production plugin, you would save pending prizes to a database/file.
                // For this request, we keep it simple as per instructions.
            }

            // End Raffle
            game.setActive(false);
            GameManager.getInstance().removeActiveRaffle(game);
            
            player.closeInventory();
        }
    }

    private void handleBuyerMenu(Player player, int slot, Inventory inv) {
        // Parse page
        int page = 0;
        ItemStack info = inv.getItem(22);
        if (info != null && info.hasItemMeta()) {
             String name = ChatColor.stripColor(info.getItemMeta().getDisplayName());
             // "Page 1/5"
             // Try to parse number even if localized "Página 1/5"
             // We just look for first number
             Pattern p = Pattern.compile("(\\d+)/(\\d+)");
             Matcher m = p.matcher(name);
             if (m.find()) {
                 try {
                    page = Integer.parseInt(m.group(1)) - 1;
                 } catch (NumberFormatException e) {}
             }
        }
        
        if (slot == 18) { // Prev
            guiManager.openBuyerMenu(player, page - 1);
        } else if (slot == 26) { // Next
            guiManager.openBuyerMenu(player, page + 1);
        } else if (slot < 18) { // Item
             // Calculate index
             int itemsPerPage = 18;
             int index = (page * itemsPerPage) + slot;
             List<RaffleGame> raffles = GameManager.getInstance().getActiveRaffles();
             if (index >= 0 && index < raffles.size()) {
                 RaffleGame game = raffles.get(index);
                 guiManager.openTicketPurchaseMenu(player, game);
             }
        }
    }

    private void handleTicketMenu(Player player, int slot) {
        RaffleGame game = GameManager.getInstance().getViewingRaffle(player);
        if (game == null || !game.isActive()) {
            player.closeInventory();
            sendMessage(player, lang.getMessage("messages.raffle-inactive"));
            return;
        }

        if (slot == 11) { // Buy Ticket
            // Check sold out
            if (game.getTickets().size() >= 100) {
                sendMessage(player, lang.getMessage("messages.sold-out"));
                playSound(player, "error");
                return;
            }

            int ticketNumber = random.nextInt(100) + 1;
            while (!game.isNumberAvailable(ticketNumber) && game.getTickets().size() < 100) {
                 ticketNumber = random.nextInt(100) + 1;
            }
            
            // Economy Check
            Economy econ = EconomyManager.getEconomy();
            if (econ != null) {
                double price = game.getTicketPrice();
                if (!econ.has(player, price)) {
                    sendMessage(player, lang.getMessage("messages.not-enough-money").replace("%price%", String.valueOf(price)));
                    playSound(player, "error");
                    return;
                }
                
                // Transaction
                econ.withdrawPlayer(player, price);
                econ.depositPlayer(game.getSeller(), price);
            }

            game.buyTicket(ticketNumber, player);
            GameManager.getInstance().saveRaffleState(game);
            sendMessage(player, lang.getMessage("messages.ticket-purchased").replace("%number%", String.valueOf(ticketNumber)));
            playSound(player, "buy");
            
            // Check Win Condition
            if (game.getWinningNumber() != -1 && ticketNumber == game.getWinningNumber()) {
                player.closeInventory();
                
                List<String> winnerSelf = lang.getBroadcastMessage("messages.winner-self");
                for (String line : winnerSelf) {
                    Bukkit.broadcastMessage(line
                        .replace("%player%", player.getName())
                        .replace("%number%", String.valueOf(ticketNumber)));
                }
                
                // Give Prize
                player.getInventory().addItem(game.getPrize());
                
                // End Raffle
                game.setActive(false);
                GameManager.getInstance().removeActiveRaffle(game);
                
                playSound(player, "win");
                spawnFirework(player);
            } else {
                player.closeInventory();
            }

        } else if (slot == 15) { // Info
            String soundName = RafflePlus.getInstance().getConfig().getString("sounds.click");
            if (soundName != null && !soundName.isEmpty()) {
                XSound.matchXSound(soundName).ifPresent(xSound -> xSound.play(player));
            }
        }
    }

    @SuppressWarnings("deprecation")
    private boolean takePrize(Player player, ItemStack prize) {
        // 1. Try standard storage removal (works for stacks in main inventory)
        if (player.getInventory().containsAtLeast(prize, prize.getAmount())) {
            player.getInventory().removeItem(prize);
            return true;
        }

        // 2. If amount is 1, check armor and offhand slots manually
        // containsAtLeast does NOT check armor or offhand in most versions
        if (prize.getAmount() == 1) {
            // Check Armor
            ItemStack[] armor = player.getInventory().getArmorContents();
            for (int i = 0; i < armor.length; i++) {
                if (armor[i] != null && armor[i].isSimilar(prize)) {
                    armor[i] = null; // Remove
                    player.getInventory().setArmorContents(armor);
                    return true;
                }
            }

            // Check Offhand (1.9+)
            try {
                ItemStack offhand = player.getInventory().getItemInOffHand();
                if (offhand != null && offhand.isSimilar(prize)) {
                    player.getInventory().setItemInOffHand(null);
                    return true;
                }
            } catch (NoSuchMethodError e) {
                // Ignore if server is < 1.9
            }
            
            // Check Main Hand explicitly (sometimes containsAtLeast is quirky with held items)
            try {
                ItemStack main = player.getInventory().getItemInMainHand();
                if (main != null && main.isSimilar(prize)) {
                    player.getInventory().setItemInMainHand(null);
                    return true;
                }
            } catch (NoSuchMethodError e) {
                 ItemStack main = player.getInventory().getItemInHand();
                 if (main != null && main.isSimilar(prize)) {
                    player.getInventory().setItemInHand(null);
                    return true;
                }
            }
        }
        
        return false;
    }

    private void spawnFirework(Player player) {
        Firework fw = player.getWorld().spawn(player.getLocation(), Firework.class);
        FireworkMeta fwm = fw.getFireworkMeta();
        
        FireworkEffect effect = FireworkEffect.builder()
                .withColor(Color.YELLOW)
                .withFade(Color.ORANGE)
                .with(FireworkEffect.Type.BALL_LARGE)
                .trail(true)
                .flicker(true)
                .build();
        
        fwm.addEffect(effect);
        fwm.setPower(1);
        fw.setFireworkMeta(fwm);
    }

    private void playSound(Player player, String configKey) {
        String soundName = RafflePlus.getInstance().getConfig().getString("sounds." + configKey);
        if (soundName == null || soundName.isEmpty()) return;
        
        // Use XSound for cross-version compatibility (1.8 - 1.21)
        XSound.matchXSound(soundName).ifPresent(xSound -> xSound.play(player));
    }

    private void sendMessage(Player player, String message) {
        player.sendMessage(message);
    }

    private void broadcastMessage(String message) {
        Bukkit.broadcastMessage(message);
    }
}
