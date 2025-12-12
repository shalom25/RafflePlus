package com.raffleplus.managers;

import com.raffleplus.RafflePlus;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class LanguageManager {

    private static LanguageManager instance;
    private FileConfiguration langConfig;
    private File langFile;

    private LanguageManager() {
        loadLanguage();
    }

    public static synchronized LanguageManager getInstance() {
        if (instance == null) {
            instance = new LanguageManager();
        }
        return instance;
    }

    public void loadLanguage() {
        RafflePlus plugin = RafflePlus.getInstance();
        String lang = plugin.getConfig().getString("language", "en");
        String fileName = "messages_" + lang + ".yml";
        String resourcePath = "languages/" + fileName;

        // Create languages directory if it doesn't exist
        File langFolder = new File(plugin.getDataFolder(), "languages");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        // Pre-generate all supported languages so users can edit them
        String[] supportedLanguages = {"en", "es"};
        for (String support : supportedLanguages) {
            String supportFile = "messages_" + support + ".yml";
            File f = new File(langFolder, supportFile);
            if (!f.exists()) {
                try {
                    plugin.saveResource("languages/" + supportFile, false);
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not save default language file: " + supportFile);
                }
            }
        }

        langFile = new File(langFolder, fileName);
        if (!langFile.exists()) {
            // saveResource saves from JAR path to DataFolder path
            // Since we moved files to resources/languages/, the jar path is languages/messages_xx.yml
            // And it will save to plugins/RafflePlus/languages/messages_xx.yml
            try {
                plugin.saveResource(resourcePath, false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Language file not found in JAR: " + resourcePath);
                // Fallback to en if specific lang not found
                if (!lang.equals("en")) {
                     plugin.getLogger().warning("Falling back to English.");
                     plugin.saveResource("languages/messages_en.yml", false);
                     langFile = new File(langFolder, "messages_en.yml");
                }
            }
        }

        langConfig = YamlConfiguration.loadConfiguration(langFile);
        
        // Load default from jar to check for missing keys
        InputStream defStream = plugin.getResource(resourcePath);
        if (defStream != null) {
            langConfig.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defStream, StandardCharsets.UTF_8)));
        }
    }

    public String getMessage(String path) {
        String msg = langConfig.getString(path);
        if (msg == null) return "Missing message: " + path;
        String prefix = RafflePlus.getInstance().getConfig().getString("messages.prefix", "");
        return ChatColor.translateAlternateColorCodes('&', prefix + msg);
    }

    // For GUI items (no prefix usually)
    public String getString(String path) {
        String msg = langConfig.getString(path);
        if (msg == null) return "Missing string: " + path;
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public List<String> getStringList(String path) {
        List<String> list = langConfig.getStringList(path);
        for (int i = 0; i < list.size(); i++) {
            list.set(i, ChatColor.translateAlternateColorCodes('&', list.get(i)));
        }
        return list;
    }

    public List<String> getBroadcastMessage(String path) {
        List<String> list = langConfig.getStringList(path);
        String prefix = RafflePlus.getInstance().getConfig().getString("messages.prefix", "");
        for (int i = 0; i < list.size(); i++) {
            if (i == 0) {
                list.set(i, ChatColor.translateAlternateColorCodes('&', prefix + list.get(i)));
            } else {
                list.set(i, ChatColor.translateAlternateColorCodes('&', list.get(i)));
            }
        }
        return list;
    }
    
    public void reload() {
        loadLanguage();
    }
}
