package com.raffleplus.managers;

import com.raffleplus.RafflePlus;
import com.raffleplus.utils.ItemStackSerializer;
import com.raffleplus.game.RaffleGame;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DatabaseManager {

    // Database Management Class
    private Connection connection;
    private final RafflePlus plugin;
    private String host, database, username, password;
    private int port;
    private boolean enabled;

    public DatabaseManager(RafflePlus plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        this.enabled = plugin.getConfig().getBoolean("mysql.enabled", false);
        this.host = plugin.getConfig().getString("mysql.host", "localhost");
        this.port = plugin.getConfig().getInt("mysql.port", 3306);
        this.database = plugin.getConfig().getString("mysql.database", "raffleplus");
        this.username = plugin.getConfig().getString("mysql.username", "root");
        this.password = plugin.getConfig().getString("mysql.password", "");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void connect() throws SQLException {
        if (!enabled) return;
        if (connection != null && !connection.isClosed()) return;

        synchronized (this) {
            if (connection != null && !connection.isClosed()) return;
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                try {
                    Class.forName("com.mysql.jdbc.Driver");
                } catch (ClassNotFoundException e2) {
                    plugin.getLogger().severe("MySQL Driver not found!");
                    e2.printStackTrace();
                    return;
                }
            }
            
            String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";
            connection = DriverManager.getConnection(url, username, password);
            createTables();
        }
    }

    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connect();
        }
        return connection;
    }

    private void createTables() {
        String rafflesTable = "CREATE TABLE IF NOT EXISTS raffleplus_raffles (" +
                "id VARCHAR(36) PRIMARY KEY, " +
                "seller_uuid VARCHAR(36), " +
                "prize_item TEXT, " +
                "price DOUBLE, " +
                "active BOOLEAN, " +
                "winning_number INT)";

        String ticketsTable = "CREATE TABLE IF NOT EXISTS raffleplus_tickets (" +
                "raffle_id VARCHAR(36), " +
                "ticket_number INT, " +
                "buyer_uuid VARCHAR(36), " +
                "PRIMARY KEY (raffle_id, ticket_number), " +
                "FOREIGN KEY (raffle_id) REFERENCES raffleplus_raffles(id) ON DELETE CASCADE)";

        try (Statement stmt = getConnection().createStatement()) {
            stmt.executeUpdate(rafflesTable);
            stmt.executeUpdate(ticketsTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveRaffle(RaffleGame raffle) {
        if (!enabled) return;
        
        // Save Raffle
        String sql = "INSERT INTO raffleplus_raffles (id, seller_uuid, prize_item, price, active, winning_number) VALUES (?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE prize_item=?, price=?, active=?, winning_number=?";
        
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, raffle.getRaffleId().toString());
            ps.setString(2, raffle.getSellerUuid().toString());
            String itemBase64 = ItemStackSerializer.toBase64(raffle.getPrize());
            ps.setString(3, itemBase64);
            ps.setDouble(4, raffle.getTicketPrice());
            ps.setBoolean(5, raffle.isActive());
            ps.setInt(6, raffle.getWinningNumber());
            
            // Update part
            ps.setString(7, itemBase64);
            ps.setDouble(8, raffle.getTicketPrice());
            ps.setBoolean(9, raffle.isActive());
            ps.setInt(10, raffle.getWinningNumber());
            
            ps.executeUpdate();
            
            // Save Tickets
            saveTickets(raffle);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void saveTickets(RaffleGame raffle) {
         if (!enabled) return;
         String sql = "INSERT IGNORE INTO raffleplus_tickets (raffle_id, ticket_number, buyer_uuid) VALUES (?, ?, ?)";
         try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
             for (Map.Entry<Integer, UUID> entry : raffle.getTickets().entrySet()) {
                 ps.setString(1, raffle.getRaffleId().toString());
                 ps.setInt(2, entry.getKey());
                 ps.setString(3, entry.getValue().toString());
                 ps.addBatch();
             }
             ps.executeBatch();
         } catch (SQLException e) {
             e.printStackTrace();
         }
    }
    
    public void deleteRaffle(UUID raffleId) {
        if (!enabled) return;
        String sql = "DELETE FROM raffleplus_raffles WHERE id=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, raffleId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<RaffleGame> loadActiveRaffles() {
        List<RaffleGame> raffles = new ArrayList<>();
        if (!enabled) return raffles;
        
        String sql = "SELECT * FROM raffleplus_raffles WHERE active=true";
        try (PreparedStatement ps = getConnection().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                try {
                    UUID id = UUID.fromString(rs.getString("id"));
                    UUID sellerId = UUID.fromString(rs.getString("seller_uuid"));
                    ItemStack prize = ItemStackSerializer.fromBase64(rs.getString("prize_item"));
                    double price = rs.getDouble("price");
                    boolean active = rs.getBoolean("active");
                    int winningNumber = rs.getInt("winning_number");
                    
                    OfflinePlayer seller = Bukkit.getOfflinePlayer(sellerId);
                    String sellerName = (seller != null && seller.getName() != null) ? seller.getName() : "Unknown";
                    
                    RaffleGame game = new RaffleGame(id, sellerId, sellerName, prize);
                    game.setTicketPrice(price);
                    game.setActive(active);
                    game.setWinningNumber(winningNumber);
                    
                    // Load tickets
                    loadTickets(game);
                    
                    raffles.add(game);
                } catch (Exception ex) {
                    plugin.getLogger().warning("Failed to load a raffle: " + ex.getMessage());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return raffles;
    }

    private void loadTickets(RaffleGame game) {
        String sql = "SELECT ticket_number, buyer_uuid FROM raffleplus_tickets WHERE raffle_id=?";
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            ps.setString(1, game.getRaffleId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int ticketNum = rs.getInt("ticket_number");
                    UUID buyerUuid = UUID.fromString(rs.getString("buyer_uuid"));
                    game.getTickets().put(ticketNum, buyerUuid);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
