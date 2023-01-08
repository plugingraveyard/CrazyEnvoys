package me.badbones69.crazyenvoys.multisupport;

import me.badbones69.crazyenvoys.CrazyEnvoys;
import me.badbones69.crazyenvoys.api.CrazyManager;

public enum Support {
    
    HOLOGRAPHIC_DISPLAYS("HolographicDisplays"),
    DECENT_HOLOGRAMS("DecentHolograms"),
    PLACEHOLDER_API("PlaceholderAPI"),
    MVDW_PLACEHOLDER_API("MVdWPlaceholderAPI"),
    WORLD_GUARD("WorldGuard"),
    WORLD_EDIT("WorldEdit"),
    CMI("CMI-Disabled"); // Disabled till I can figure out how to make it work.
    
    private final String name;

    private final CrazyEnvoys plugin = CrazyEnvoys.getPlugin();
    
    Support(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean isPluginLoaded() {
        if (plugin.getServer().getPluginManager().getPlugin(name) != null) {
            return plugin.isEnabled();
        }

        return false;
    }
}