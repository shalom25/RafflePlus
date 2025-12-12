package com.raffleplus.game;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RaffleGame {
    // Core Game Class
    private final UUID raffleId;
    private final UUID sellerUuid;
    private final String sellerName;
    private final ItemStack prize;
    private double ticketPrice;
    private final Map<Integer, UUID> tickets; // Ticket Number -> Buyer UUID
    private boolean active;
    private int winningNumber;

    public RaffleGame(Player seller, ItemStack prize) {
        this(UUID.randomUUID(), seller.getUniqueId(), seller.getName(), prize);
    }

    public RaffleGame(UUID raffleId, UUID sellerUuid, String sellerName, ItemStack prize) {
        this.raffleId = raffleId;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.prize = prize.clone();
        this.ticketPrice = 0.0;
        this.tickets = new HashMap<>();
        this.active = false;
        this.winningNumber = -1;
    }

    public UUID getRaffleId() {
        return raffleId;
    }

    public OfflinePlayer getSeller() {
        return Bukkit.getOfflinePlayer(sellerUuid);
    }

    public UUID getSellerUuid() {
        return sellerUuid;
    }

    public String getSellerName() {
        return sellerName;
    }

    public ItemStack getPrize() {
        return prize;
    }

    public double getTicketPrice() {
        return ticketPrice;
    }

    public void setTicketPrice(double ticketPrice) {
        this.ticketPrice = ticketPrice;
    }

    public Map<Integer, UUID> getTickets() {
        return tickets;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isNumberAvailable(int number) {
        return !tickets.containsKey(number);
    }

    public void buyTicket(int number, Player buyer) {
        tickets.put(number, buyer.getUniqueId());
    }

    public int getWinningNumber() {
        return winningNumber;
    }

    public void setWinningNumber(int winningNumber) {
        this.winningNumber = winningNumber;
    }
}
