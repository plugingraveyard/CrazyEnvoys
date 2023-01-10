package me.badbones69.crazyenvoys.multisupport;

import be.maximvdw.placeholderapi.PlaceholderAPI;
import me.badbones69.crazyenvoys.CrazyEnvoys;
import me.badbones69.crazyenvoys.api.CrazyManager;
import me.badbones69.crazyenvoys.api.FileManager;

public class MVdWPlaceholderAPISupport {

    private static final CrazyEnvoys plugin = CrazyEnvoys.getPlugin();
    private static final CrazyManager crazyManager = plugin.getCrazyManager();
    
    public static void registerPlaceholders() {
        PlaceholderAPI.registerPlaceholder(plugin, plugin.getName().toLowerCase() + "_cooldown", e -> {
            if (crazyManager.isEnvoyActive()) {
                return FileManager.Files.MESSAGES.getFile().getString("Messages.Hologram-Placeholders.On-Going");
            } else {
                return crazyManager.getNextEnvoyTime();
            }
        });
        
        PlaceholderAPI.registerPlaceholder(plugin, plugin.getName().toLowerCase() + "_time_left", e -> {
            if (crazyManager.isEnvoyActive()) {
                return crazyManager.getEnvoyRunTimeLeft();
            } else {
                return FileManager.Files.MESSAGES.getFile().getString("Messages.Hologram-Placeholders.Not-Running");
            }
        });

        PlaceholderAPI.registerPlaceholder(plugin, plugin.getName().toLowerCase() + "_crates_left", e -> crazyManager.getActiveEnvoys().size() + "");
    }
}