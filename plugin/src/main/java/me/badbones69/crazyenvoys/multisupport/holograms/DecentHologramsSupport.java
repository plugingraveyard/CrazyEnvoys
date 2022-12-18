package me.badbones69.crazyenvoys.multisupport.holograms;

import me.badbones69.crazyenvoys.Methods;
import me.badbones69.crazyenvoys.api.interfaces.HologramController;
import me.badbones69.crazyenvoys.api.objects.Tier;
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import org.bukkit.block.Block;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

public class DecentHologramsSupport implements HologramController {
    
    private static final HashMap<Block, Hologram> holograms = new HashMap<>();
    
    public void createHologram(Block block, Tier tier) {
        double height = tier.getHoloHeight();
        Hologram hologram = DHAPI.createHologram(ThreadLocalRandom.current().nextInt() + "", block.getLocation().add(.5, height, .5));
        tier.getHoloMessage().forEach(line -> DHAPI.addHologramLine(hologram, Methods.color(line)));
        holograms.put(block, hologram);
    }
    
    public void removeHologram(Block block) {
        if (!holograms.containsKey(block)) return;
        Hologram hologram = holograms.get(block);
        holograms.remove(block);
        hologram.delete();
    }
    
    public void removeAllHolograms() {
        holograms.forEach((key, value) -> value.delete());
        holograms.clear();
    }
    
    @Override public String getPluginName() {
        return null;
    }
    
}