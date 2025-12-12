package com.raffleplus.managers;

import com.raffleplus.game.RaffleGame;
import com.cryptomorin.xseries.XMaterial;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GuiManager {

    // GUI Manager Class
    private final LanguageManager lang = LanguageManager.getInstance();

    public String getSellerMenuTitle() {
        return lang.getString("menu-title.seller");
    }

    public String getBuyerMenuTitle() {
        return lang.getString("menu-title.buyer");
    }

    public String getTicketMenuTitle() {
        return lang.getString("menu-title.ticket");
    }

    // --- SELLER MENU ---
    public void openSellerMenu(Player player) {
        RaffleGame game = GameManager.getInstance().getSetupSession(player);
        if (game == null) {
            game = GameManager.getInstance().getActiveRaffle(player);
        }
        if (game == null) return;

        Inventory inv = Bukkit.createInventory(null, 27, getSellerMenuTitle());

        // Borders
        addGreenBorders(inv);

        // 1. The Item to Raffle (Center)
        ItemStack displayItem = game.getPrize().clone();
        inv.setItem(12, displayItem);

        // 2. Set Price Button (Left)
        ItemStack priceBtn = XMaterial.GOLD_INGOT.parseItem();
        if (priceBtn == null) priceBtn = XMaterial.GOLD_BLOCK.parseItem();
        ItemMeta priceMeta = priceBtn.getItemMeta();
        priceMeta.setDisplayName(lang.getString("gui.set-price.name"));
        List<String> priceLore = new ArrayList<>();
        priceLore.add(lang.getString("gui.set-price.lore-current").replace("%price%", String.valueOf(game.getTicketPrice())));
        priceLore.add(lang.getString("gui.set-price.lore-click"));
        priceMeta.setLore(priceLore);
        priceBtn.setItemMeta(priceMeta);
        inv.setItem(10, priceBtn);

        // 3. Publish Button (Slot 17) - Step 1
        ItemStack pubBtn = XMaterial.EMERALD_BLOCK.parseItem();
        ItemMeta pubMeta = pubBtn.getItemMeta();
        pubMeta.setDisplayName(lang.getString("gui.publish.name"));
        List<String> pubLore = new ArrayList<>();
        pubLore.add(lang.getString("gui.publish.lore-step1"));
        if (game.isActive()) {
             pubLore.add(lang.getString("gui.publish.lore-status"));
        } else {
             pubLore.add(lang.getString("gui.publish.lore-click"));
        }
        pubMeta.setLore(pubLore);
        pubBtn.setItemMeta(pubMeta);
        inv.setItem(16, pubBtn);

        // 4. Start Button (Slot 15) - Step 2
        ItemStack startBtn = XMaterial.REDSTONE_BLOCK.parseItem();
        ItemMeta startMeta = startBtn.getItemMeta();
        startMeta.setDisplayName(lang.getString("gui.start.name"));
        List<String> startLore = new ArrayList<>();
        startLore.add(lang.getString("gui.start.lore-step2"));
        if (!game.isActive()) {
            startLore.add(lang.getString("gui.start.lore-must-publish"));
        } else if (game.getWinningNumber() != -1) {
            startLore.add(lang.getString("gui.start.lore-started"));
            startBtn = XMaterial.REDSTONE_LAMP.parseItem();
        } else {
            startLore.add(lang.getString("gui.start.lore-ready"));
        }
        startMeta.setLore(startLore);
        startBtn.setItemMeta(startMeta);
        inv.setItem(14, startBtn);

        player.openInventory(inv);
    }

    // --- BUYER MENU (LIST) ---
    public void openBuyerMenu(Player player, int page) {
        List<RaffleGame> raffles = GameManager.getInstance().getActiveRaffles();
        if (raffles.isEmpty()) {
            player.sendMessage(lang.getMessage("messages.no-active-raffle"));
            return;
        }

        Inventory inv = Bukkit.createInventory(null, 27, getBuyerMenuTitle());
        
        // Pagination Logic
        int itemsPerPage = 18; // 2 rows of 9
        int totalPages = (int) Math.ceil((double) raffles.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1;
        
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        int startIndex = page * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, raffles.size());

        for (int i = startIndex; i < endIndex; i++) {
            RaffleGame game = raffles.get(i);
            ItemStack item;

            if (game.getTickets().size() >= 100) {
                 item = XMaterial.BARRIER.parseItem();
                 ItemMeta meta = item.getItemMeta();
                 String name = lang.getString("gui.sold-out.name");
                 meta.setDisplayName(name);
                 item.setItemMeta(meta);
             } else {
                item = game.getPrize().clone();
                ItemMeta meta = item.getItemMeta();
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add(" ");
                lore.add(lang.getString("gui.buyer-item.price").replace("%price%", String.valueOf(game.getTicketPrice())));
                lore.add(lang.getString("gui.buyer-item.seller").replace("%seller%", game.getSeller().getName()));
                lore.add(lang.getString("gui.buyer-item.click"));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            
            // Map list index to inventory slot
            inv.setItem(i - startIndex, item);
        }

        // Navigation Buttons (Row 3: 18-26)
        if (page > 0) {
            ItemStack prev = XMaterial.ARROW.parseItem();
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.setDisplayName(lang.getString("gui.prev-page"));
            prev.setItemMeta(prevMeta);
            inv.setItem(18, prev);
        }

        if (page < totalPages - 1) {
            ItemStack next = XMaterial.ARROW.parseItem();
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.setDisplayName(lang.getString("gui.next-page"));
            next.setItemMeta(nextMeta);
            inv.setItem(26, next);
        }

        // Page Info
        ItemStack info = XMaterial.PAPER.parseItem();
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(lang.getString("gui.page-info")
                .replace("%page%", String.valueOf(page + 1))
                .replace("%total%", String.valueOf(totalPages)));
        info.setItemMeta(infoMeta);
        inv.setItem(22, info);

        fillEmptySlots(inv);
        player.openInventory(inv);
    }

    // --- TICKET PURCHASE MENU ---
    public void openTicketPurchaseMenu(Player player, RaffleGame game) {
        if (game == null || !game.isActive()) {
             player.sendMessage(lang.getMessage("messages.raffle-inactive"));
             player.closeInventory();
             return;
        }

        // Track what they are viewing
        GameManager.getInstance().setViewingRaffle(player, game);
        
        Inventory inv = Bukkit.createInventory(null, 27, getTicketMenuTitle());
        
        // Borders
        addGreenBorders(inv);

        // 1. Prize (Center)
        ItemStack displayItem = game.getPrize().clone();
        ItemMeta meta = displayItem.getItemMeta();
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        lore.add(" ");
        lore.add(lang.getString("gui.ticket-info.price-per-ticket").replace("%price%", String.valueOf(game.getTicketPrice())));
        lore.add(lang.getString("gui.ticket-info.seller").replace("%seller%", game.getSeller().getName()));
        meta.setLore(lore);
        displayItem.setItemMeta(meta);
        inv.setItem(13, displayItem);

        // 2. Buy Button (or Sold Out)
        if (game.getTickets().size() >= 100) {
             ItemStack soldOut = XMaterial.BARRIER.parseItem();
             ItemMeta soldMeta = soldOut.getItemMeta();
             String name = lang.getString("gui.sold-out.name");
             soldMeta.setDisplayName(name);
             soldOut.setItemMeta(soldMeta);
             inv.setItem(11, soldOut);
        } else {
             ItemStack buyButton = XMaterial.PAPER.parseItem();
             ItemMeta buyMeta = buyButton.getItemMeta();
             buyMeta.setDisplayName(lang.getString("gui.buy-button.name"));
             List<String> buyLore = new ArrayList<>();
             buyLore.add(lang.getString("gui.buy-button.lore"));
             buyMeta.setLore(buyLore);
             buyButton.setItemMeta(buyMeta);
             inv.setItem(11, buyButton);
        }

        player.openInventory(inv);
    }

    private void addGreenBorders(Inventory inv) {
        ItemStack pane = XMaterial.GREEN_STAINED_GLASS_PANE.parseItem();
        if (pane == null) pane = XMaterial.GLASS_PANE.parseItem();
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);

        int[] borders = {0,1,2,3,4,5,6,7,8, 18,19,20,21,22,23,24,25,26};
        for (int slot : borders) {
            if (inv.getItem(slot) == null) {
                inv.setItem(slot, pane);
            }
        }
    }

    private void fillEmptySlots(Inventory inv) {
        ItemStack pane = XMaterial.GRAY_STAINED_GLASS_PANE.parseItem();
        if (pane == null) pane = XMaterial.GLASS_PANE.parseItem();
        ItemMeta meta = pane.getItemMeta();
        meta.setDisplayName(" ");
        pane.setItemMeta(meta);

        for (int i = 0; i < inv.getSize(); i++) {
            if (inv.getItem(i) == null) {
                inv.setItem(i, pane);
            }
        }
    }
}
