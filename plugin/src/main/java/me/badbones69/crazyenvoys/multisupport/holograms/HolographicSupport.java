package me.badbones69.crazyenvoys.multisupport.holograms;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import me.badbones69.crazyenvoys.Methods;
import me.badbones69.crazyenvoys.api.CrazyManager;
import me.badbones69.crazyenvoys.api.interfaces.HologramController;
import me.badbones69.crazyenvoys.api.objects.Tier;
import org.bukkit.block.Block;
import java.util.HashMap;

public class HolographicSupport implements HologramController {
    
    private final CrazyManager crazyManager = CrazyManager.getInstance();
    private final HashMap<Block, Hologram> holograms = new HashMap<>();

    public void createHologram(Block block, Tier tier) {
        double height = tier.getHoloHeight();
        Hologram hologram = HologramsAPI.createHologram(crazyManager.getPlugin(), block.getLocation().add(.5, height, .5));

        tier.getHoloMessage().stream().map(Methods::color).forEach(hologram::appendTextLine);

        holograms.put(block, hologram);
    }

    public void removeHologram(Block block) {
        if (holograms.containsKey(block)) {
            Hologram hologram = holograms.get(block);
            holograms.remove(block);
            hologram.delete();
        }
    }

    public void removeAllHolograms() {
        holograms.keySet().forEach(block -> holograms.get(block).delete());
        holograms.clear();
    }

    public String getPluginName() {
        return "HolographicDisplays";
    }
}