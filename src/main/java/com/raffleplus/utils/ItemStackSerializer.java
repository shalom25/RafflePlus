package com.raffleplus.utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

public class ItemStackSerializer {

    // Serializer Utility
    public static String toBase64(ItemStack item) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("i", item);
        return config.saveToString();
    }

    public static ItemStack fromBase64(String data) {
        if (data == null || data.isEmpty()) return null;
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.loadFromString(data);
            return config.getItemStack("i");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
