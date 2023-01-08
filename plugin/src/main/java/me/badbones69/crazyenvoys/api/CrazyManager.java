package me.badbones69.crazyenvoys.api;

import me.badbones69.crazyenvoys.CrazyEnvoys;
import me.badbones69.crazyenvoys.Methods;
import me.badbones69.crazyenvoys.api.enums.Messages;
import me.badbones69.crazyenvoys.api.events.EnvoyEndEvent;
import me.badbones69.crazyenvoys.api.events.EnvoyStartEvent;
import me.badbones69.crazyenvoys.api.interfaces.HologramController;
import me.badbones69.crazyenvoys.api.objects.EnvoySettings;
import me.badbones69.crazyenvoys.api.objects.Flare;
import me.badbones69.crazyenvoys.api.objects.ItemBuilder;
import me.badbones69.crazyenvoys.api.objects.Prize;
import me.badbones69.crazyenvoys.api.objects.Tier;
import me.badbones69.crazyenvoys.controllers.EditControl;
import me.badbones69.crazyenvoys.controllers.EnvoyControl;
import me.badbones69.crazyenvoys.controllers.FireworkDamageAPI;
import me.badbones69.crazyenvoys.multisupport.Support;
import me.badbones69.crazyenvoys.multisupport.ServerProtocol;
import me.badbones69.crazyenvoys.multisupport.WorldGuardVersion;
import me.badbones69.crazyenvoys.multisupport.WorldGuard_v6;
import me.badbones69.crazyenvoys.multisupport.WorldGuard_v7;
import me.badbones69.crazyenvoys.multisupport.holograms.DecentHologramsSupport;
import me.badbones69.crazyenvoys.multisupport.holograms.HolographicSupport;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class CrazyManager {

    private final CrazyEnvoys plugin = CrazyEnvoys.getPlugin();
    
    private final FileManager fileManager = plugin.getFileManager();

    private final EnvoySettings envoySettings = EnvoySettings.getInstance();
    private BukkitTask runTimeTask;
    private BukkitTask coolDownTask;
    private Calendar nextEnvoy;
    private Calendar envoyTimeLeft;
    private boolean envoyActive = false;
    private boolean autoTimer = true;
    private WorldGuardVersion worldGuardVersion;
    private HologramController hologramController;
    private final List<Material> blacklistedBlocks = new ArrayList<>();
    private final List<UUID> ignoreMessages = new ArrayList<>();
    private final List<Calendar> warnings = new ArrayList<>();
    private final List<Block> spawnLocations = new ArrayList<>();
    private final List<Block> spawnedLocations = new ArrayList<>();
    private final HashMap<Entity, Block> fallingBlocks = new HashMap<>();
    private Location center;
    private String centerString;
    private final HashMap<Block, Tier> activeEnvoys = new HashMap<>();
    private final HashMap<Location, BukkitTask> activeSignals = new HashMap<>();
    private final List<Tier> tiers = new ArrayList<>();
    private final List<Tier> cachedChances = new ArrayList<>();
    private final Random random = new Random();
    
    /**
     * Run this when you need to load the new locations.
     */
    public void load() {
        if (!envoyActive) envoyActive = false;
        
        spawnLocations.clear();
        blacklistedBlocks.clear();
        cachedChances.clear();
        envoySettings.loadSettings();
        FileConfiguration data = FileManager.Files.DATA.getFile();
        envoyTimeLeft = Calendar.getInstance();
        
        List<String> failedLocations = new ArrayList<>();
        
        for (String location : data.getStringList("Locations.Spawns")) {
            try {
                spawnLocations.add(getLocationFromString(location).getBlock());
            } catch (Exception ignore) {
                failedLocations.add(location);
            }
        }
        
        if (fileManager.isLogging() && !failedLocations.isEmpty()) plugin.getLogger().info("Failed to load " + failedLocations.size() + " locations and will reattempt in 10s.");
        
        if (Calendar.getInstance().after(getNextEnvoy())) setEnvoyActive(false);
        
        loadCenter();
        
        if (envoySettings.isEnvoyRunTimerEnabled()) {
            Calendar cal = Calendar.getInstance();

            if (envoySettings.isEnvoyCooldownEnabled()) {
                autoTimer = true;
                cal.setTimeInMillis(data.getLong("Next-Envoy"));

                if (Calendar.getInstance().after(cal)) {
                    cal.setTimeInMillis(getEnvoyCooldown().getTimeInMillis());
                }
            } else {
                autoTimer = false;
                String time = envoySettings.getEnvoyClockTime();
                int hour = Integer.parseInt(time.split(" ")[0].split(":")[0]);
                int min = Integer.parseInt(time.split(" ")[0].split(":")[1]);
                int c = Calendar.AM;

                if (time.split(" ")[1].equalsIgnoreCase("AM")) {
                    c = Calendar.AM;
                } else if (time.split(" ")[1].equalsIgnoreCase("PM")) {
                    c = Calendar.PM;
                }

                cal.set(Calendar.HOUR_OF_DAY, hour);
                cal.getTime(); // Without this makes the hours not change for some reason.
                cal.set(Calendar.MINUTE, min);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.AM_PM, c);

                if (cal.before(Calendar.getInstance())) cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH) + 1);
            }

            nextEnvoy = cal;
            startEnvoyCountDown();
            resetWarnings();
        } else {
            nextEnvoy = Calendar.getInstance();
        }

        //================================== Tiers Load ==================================//
        tiers.clear();
        
        for (FileManager.CustomFile tierFile : fileManager.getCustomFiles()) {
            Tier tier = new Tier(tierFile.getName());
            FileConfiguration file = tierFile.getFile();
            tier.setClaimPermissionToggle(file.getBoolean("Settings.Claim-Permission"));
            tier.setClaimPermission(file.getString("Settings.Claim-Permission-Name"));
            tier.setUseChance(file.getBoolean("Settings.Use-Chance"));
            tier.setSpawnChance(file.getInt("Settings.Spawn-Chance"));
            tier.setBulkToggle(file.getBoolean("Settings.Bulk-Prizes.Toggle"));
            tier.setBulkRandom(file.getBoolean("Settings.Bulk-Prizes.Random"));
            tier.setBulkMax(file.getInt("Settings.Bulk-Prizes.Max-Bulk"));
            tier.setHoloToggle(file.getBoolean("Settings.Hologram-Toggle"));
            tier.setHoloHeight(file.getDouble("Settings.Hologram-Height", 1.5));
            tier.setHoloMessage(file.getStringList("Settings.Hologram"));
            ItemBuilder placedBlock = new ItemBuilder().setMaterial(file.getString("Settings.Placed-Block"));
            tier.setPlacedBlockMaterial(placedBlock.getMaterial());
            tier.setPlacedBlockMetaData(placedBlock.getDamage());
            
            tier.setFireworkToggle(file.getBoolean("Settings.Firework-Toggle"));

            if (file.getStringList("Settings.Firework-Colors").isEmpty()) {
                tier.setFireworkColors(Arrays.asList(Color.GRAY, Color.BLACK, Color.ORANGE));
            } else {
                file.getStringList("Settings.Firework-Colors").forEach(color -> tier.addFireworkColor(Methods.getColor(color)));
            }
            
            tier.setSignalFlareToggle(file.getBoolean("Settings.Signal-Flare.Toggle"));
            tier.setSignalFlareTimer(file.getString("Settings.Signal-Flare.Time"));
            
            if (file.getStringList("Settings.Signal-Flare.Colors").isEmpty()) {
                tier.setSignalFlareColors(Arrays.asList(Color.GRAY, Color.BLACK, Color.ORANGE));
            } else {
                file.getStringList("Settings.Signal-Flare.Colors").forEach(color -> tier.addSignalFlareColor(Methods.getColor(color)));
            }
            
            for (String prizeID : file.getConfigurationSection("Prizes").getKeys(false)) {
                String path = "Prizes." + prizeID + ".";
                int chance = file.getInt(path + "Chance");
                List<String> commands = file.getStringList(path + "Commands");
                List<String> messages = file.getStringList(path + "Messages");
                boolean dropItems = file.getBoolean(path + "Drop-Items");
                List<ItemBuilder> items = ItemBuilder.convertStringList(file.getStringList(path + "Items"));
                tier.addPrize(new Prize(prizeID).setChance(chance).setDropItems(dropItems).setItemBuilders(items).setCommands(commands).setMessages(messages));
            }
            
            tiers.add(tier);
            cleanLocations();

            // Loading the blacklisted blocks.
            if (ServerProtocol.isNewer(ServerProtocol.v1_12_R1)) {
                blacklistedBlocks.add(Material.WATER);
                blacklistedBlocks.add(Material.LILY_PAD);
                blacklistedBlocks.add(Material.LAVA);
                blacklistedBlocks.add(Material.CHORUS_PLANT);
                blacklistedBlocks.add(Material.KELP_PLANT);
                blacklistedBlocks.add(Material.TALL_GRASS);
                blacklistedBlocks.add(Material.CHORUS_FLOWER);
                blacklistedBlocks.add(Material.SUNFLOWER);
                blacklistedBlocks.add(Material.IRON_BARS);
                blacklistedBlocks.add(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);
                blacklistedBlocks.add(Material.IRON_TRAPDOOR);
                blacklistedBlocks.add(Material.OAK_TRAPDOOR);
                blacklistedBlocks.add(Material.OAK_FENCE);
                blacklistedBlocks.add(Material.OAK_FENCE_GATE);
                blacklistedBlocks.add(Material.ACACIA_FENCE);
                blacklistedBlocks.add(Material.BIRCH_FENCE);
                blacklistedBlocks.add(Material.DARK_OAK_FENCE);
                blacklistedBlocks.add(Material.JUNGLE_FENCE);
                blacklistedBlocks.add(Material.NETHER_BRICK_FENCE);
                blacklistedBlocks.add(Material.SPRUCE_FENCE);
                blacklistedBlocks.add(Material.ACACIA_FENCE_GATE);
                blacklistedBlocks.add(Material.BIRCH_FENCE_GATE);
                blacklistedBlocks.add(Material.DARK_OAK_FENCE_GATE);
                blacklistedBlocks.add(Material.JUNGLE_FENCE_GATE);
                blacklistedBlocks.add(Material.SPRUCE_FENCE_GATE);
                blacklistedBlocks.add(Material.GLASS_PANE);
                blacklistedBlocks.add(Material.STONE_SLAB);
            } else {
                blacklistedBlocks.add(Material.WATER);
                blacklistedBlocks.add(Material.matchMaterial("STATIONARY_WATER"));
                blacklistedBlocks.add(Material.matchMaterial("WATER_LILY"));
                blacklistedBlocks.add(Material.LAVA);
                blacklistedBlocks.add(Material.matchMaterial("STATIONARY_LAVA"));
                blacklistedBlocks.add(Material.matchMaterial("CROPS"));
                blacklistedBlocks.add(Material.matchMaterial("LONG_GRASS"));
                blacklistedBlocks.add(Material.matchMaterial("YELLOW_FLOWER"));
                blacklistedBlocks.add(Material.matchMaterial("IRON_FENCE"));
                blacklistedBlocks.add(Material.matchMaterial("IRON_PLATE"));
                blacklistedBlocks.add(Material.IRON_TRAPDOOR);
                blacklistedBlocks.add(Material.matchMaterial("TRAP_DOOR"));
                blacklistedBlocks.add(Material.matchMaterial("FENCE"));
                blacklistedBlocks.add(Material.matchMaterial("FENCE_GATE"));
                blacklistedBlocks.add(Material.ACACIA_FENCE);
                blacklistedBlocks.add(Material.BIRCH_FENCE);
                blacklistedBlocks.add(Material.DARK_OAK_FENCE);
                blacklistedBlocks.add(Material.JUNGLE_FENCE);
                blacklistedBlocks.add(Material.matchMaterial("NETHER_FENCE"));
                blacklistedBlocks.add(Material.SPRUCE_FENCE);
                blacklistedBlocks.add(Material.ACACIA_FENCE_GATE);
                blacklistedBlocks.add(Material.BIRCH_FENCE_GATE);
                blacklistedBlocks.add(Material.DARK_OAK_FENCE_GATE);
                blacklistedBlocks.add(Material.JUNGLE_FENCE_GATE);
                blacklistedBlocks.add(Material.SPRUCE_FENCE_GATE);
                blacklistedBlocks.add(Material.matchMaterial("STAINED_GLASS_PANE"));
                blacklistedBlocks.add(Material.matchMaterial("STONE_SLAB2"));
            }
        }
        
        if (Support.WORLD_GUARD.isPluginLoaded() && Support.WORLD_EDIT.isPluginLoaded()) {
            worldGuardVersion = ServerProtocol.isNewer(ServerProtocol.v1_12_R1) ? new WorldGuard_v7() : new WorldGuard_v6();
        }
        
        if (Support.HOLOGRAPHIC_DISPLAYS.isPluginLoaded()) {
            hologramController = new HolographicSupport();
            plugin.getLogger().info("Loaded" + hologramController.getPluginName() + " hologram hook.");
        } else if (Support.DECENT_HOLOGRAMS.isPluginLoaded()) {
            hologramController = new DecentHologramsSupport();
            plugin.getLogger().info("Loaded" + hologramController.getPluginName() + " hologram hook.");
        } else plugin.getLogger().info("No holograms plugin were found.");
        
        if (!failedLocations.isEmpty()) {
            if (fileManager.isLogging()) plugin.getLogger().info("Attempting to fix " + failedLocations.size() + " locations that failed.");
            int failed = 0;
            int fixed = 0;
            
            for (String location : failedLocations) {
                try {
                    spawnLocations.add(getLocationFromString(location).getBlock());
                    fixed++;
                } catch (Exception ignore) {
                    failed++;
                }
            }
            
            if (fixed > 0) plugin.getLogger().info("Was able to fix " + fixed + " locations that failed.");
            if (failed > 0) plugin.getLogger().info("Failed to fix " + failed + " locations and will not reattempt.");
        }

        Flare.load();
    }
    
    /**
     * Run this when you need to save the locations.
     */
    public void unload() {
        deSpawnCrates();
        FileManager.Files.DATA.getFile().set("Next-Envoy", getNextEnvoy().getTimeInMillis());
        FileManager.Files.DATA.saveFile();
        spawnLocations.clear();
        EnvoyControl.clearCooldowns();
    }
    
    /**
     * Used when the plugin starts to control the count-down and when the event starts
     */
    public void startEnvoyCountDown() {
        cancelEnvoyCooldownTime();
        coolDownTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isEnvoyActive()) {
                    Calendar cal = Calendar.getInstance();
                    cal.clear(Calendar.MILLISECOND);

                    // Ryder Start
                    int online = plugin.getServer().getOnlinePlayers().size();

                    if (online == 0 && envoySettings.isEnvoyFilterEnabled()) return;
                    // Ryder End

                    for (Calendar warn : getWarnings()) {
                        Calendar check = Calendar.getInstance();
                        check.setTimeInMillis(warn.getTimeInMillis());
                        check.clear(Calendar.MILLISECOND);

                        if (check.compareTo(cal) == 0) {
                            HashMap<String, String> placeholder = new HashMap<>();
                            placeholder.put("%time%", getNextEnvoyTime());
                            placeholder.put("%Time%", getNextEnvoyTime());
                            Messages.WARNING.broadcastMessage(false, placeholder);
                        }
                    }

                    Calendar next = Calendar.getInstance();
                    next.setTimeInMillis(getNextEnvoy().getTimeInMillis());
                    next.clear(Calendar.MILLISECOND);
                    
                    if (next.compareTo(cal) <= 0 && !isEnvoyActive()) {
                        if (envoySettings.isMinPlayersEnabled()) {

                            if (online < envoySettings.getMinPlayers()) {
                                HashMap<String, String> placeholder = new HashMap<>();
                                placeholder.put("%amount%", online + "");
                                placeholder.put("%Amount%", online + "");
                                Messages.NOT_ENOUGH_PLAYERS.broadcastMessage(false, placeholder);
                                setNextEnvoy(getEnvoyCooldown());
                                resetWarnings();
                                return;
                            }
                        }
                        
                        if (envoySettings.isRandomLocationsEnabled() && center.getWorld() == null) {
                            plugin.getLogger().info("The envoy center's world can't be found and so envoy has been canceled.");
                            plugin.getLogger().info("Center String: " + centerString);
                            setNextEnvoy(getEnvoyCooldown());
                            resetWarnings();
                            return;
                        }
                        
                        EnvoyStartEvent event = new EnvoyStartEvent(autoTimer ? EnvoyStartEvent.EnvoyStartReason.AUTO_TIMER : EnvoyStartEvent.EnvoyStartReason.SPECIFIED_TIME);
                        plugin.getServer().getPluginManager().callEvent(event);

                        if (!event.isCancelled()) startEnvoyEvent();
                    }
                }
            }
        }.runTaskTimer(plugin, 20, 20);
    }
    
    /**
     * @param block The location you want the tier from.
     * @return The tier that location is.
     */
    public Tier getTier(Block block) {
        return activeEnvoys.get(block);
    }
    
    /**
     * @return True if the envoy event is currently happening and false if not.
     */
    public boolean isEnvoyActive() {
        return envoyActive;
    }
    
    /**
     * Despawns all the active crates.
     */
    public void deSpawnCrates() {
        envoyActive = false;
        cleanLocations();
        
        for (Block block : getActiveEnvoys()) {
            if (!block.getChunk().isLoaded()) {
                block.getChunk().load();
            }

            block.setType(Material.AIR);
            stopSignalFlare(block.getLocation());
        }
        
        fallingBlocks.keySet().forEach(Entity :: remove);

        if (hasHologramPlugin()) hologramController.removeAllHolograms();

        fallingBlocks.clear();
        activeEnvoys.clear();
    }
    
    public WorldGuardVersion getWorldGuardSupport() {
        return worldGuardVersion;
    }
    
    public HologramController getHologramController() {
        return hologramController;
    }
    
    public boolean hasHologramPlugin() {
        return hologramController != null;
    }
    
    /**
     * @return All the location the chests will spawn.
     */
    public List<Block> getSpawnLocations() {
        return spawnLocations;
    }
    
    /**
     * @param location The location that you want to check.
     */
    public boolean isLocation(Location location) {
        for (Block block : spawnLocations) {
            if (block.getLocation().equals(location)) {
                return true;
            }
        }

        return false;
    }
    
    public void saveSpawnLocations() {
        ArrayList<String> locations = new ArrayList<>();

        for (Block block : spawnLocations) {
            try {
                locations.add(getStringFromLocation(block.getLocation()));
            } catch (Exception ignored) {}
        }

        FileManager.Files.DATA.getFile().set("Locations.Spawns", locations);
        FileManager.Files.DATA.saveFile();
    }
    
    /**
     * @return All the active envoys that are active.
     */
    public Set<Block> getActiveEnvoys() {
        return activeEnvoys.keySet();
    }
    
    /**
     * @param block The location you are checking.
     * @return Turn if it is and false if not.
     */
    public boolean isActiveEnvoy(Block block) {
        return activeEnvoys.containsKey(block);
    }
    
    /**
     * @param block The location you wish to add.
     */
    public void addActiveEnvoy(Block block, Tier tier) {
        activeEnvoys.put(block, tier);
    }
    
    /**
     * @param block The location you wish to remove.
     */
    public void removeActiveEnvoy(Block block) {
        activeEnvoys.remove(block);
    }
    
    /**
     * @param block The location you want to add.
     */
    public void addLocation(Block block) {
        spawnLocations.add(block);
        saveSpawnLocations();
    }
    
    /**
     * @param block The location you want to remove.
     */
    public void removeLocation(Block block) {
        if (isLocation(block.getLocation())) {
            spawnLocations.remove(block);
            saveSpawnLocations();
        }
    }
    
    /**
     * Clear all Envoy locations.
     */
    public void clearLocations() {
        spawnLocations.clear();
        saveSpawnLocations();
    }
    
    /**
     * @return The next envoy time as a calendar.
     */
    public Calendar getNextEnvoy() {
        return nextEnvoy;
    }
    
    /**
     * @param cal A calendar that has the next time the envoy will happen.
     */
    public void setNextEnvoy(Calendar cal) {
        nextEnvoy = cal;
    }
    
    /**
     * @return The time till the next envoy.
     */
    public String getNextEnvoyTime() {
        String message = Methods.convertTimeToString(getNextEnvoy());

        if (message.equals("0" + Messages.SECOND.getMessage())) {
            message = Messages.ON_GOING.getMessage();
        }

        return message;
    }
    
    /**
     * @return All falling blocks are currently going.
     */
    public Map<Entity, Block> getFallingBlocks() {
        return fallingBlocks;
    }
    
    /**
     * @param entity Remove a falling block from the list.
     */
    public void removeFallingBlock(Entity entity) {
        fallingBlocks.remove(entity);
    }
    
    /**
     * Call when you want to set the new warning.
     */
    public void resetWarnings() {
        warnings.clear();
        envoySettings.getEnvoyWarnings().forEach(time -> addWarning(makeWarning(time)));
    }
    
    /**
     * @param cal When adding a new warning.
     */
    public void addWarning(Calendar cal) {
        warnings.add(cal);
    }
    
    /**
     * @return All the current warnings.
     */
    public List<Calendar> getWarnings() {
        return warnings;
    }
    
    /**
     * @param time The new time for the warning.
     * @return The new time as a calendar
     */
    public Calendar makeWarning(String time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(getNextEnvoy().getTimeInMillis());

        for (String i : time.split(" ")) {
            if (i.contains("d")) {
                cal.add(Calendar.DATE, -Integer.parseInt(i.replace("d", "")));
            } else if (i.contains("h")) {
                cal.add(Calendar.HOUR, -Integer.parseInt(i.replace("h", "")));
            } else if (i.contains("m")) {
                cal.add(Calendar.MINUTE, -Integer.parseInt(i.replace("m", "")));
            } else if (i.contains("s")) {
                cal.add(Calendar.SECOND, -Integer.parseInt(i.replace("s", "")));
            }
        }

        return cal;
    }
    
    /**
     * @return The time left in the current envoy event.
     */
    public String getEnvoyRunTimeLeft() {
        String message = Methods.convertTimeToString(envoyTimeLeft);

        if (message.equals("0" + Messages.SECOND.getMessage())) {
            message = Messages.NOT_RUNNING.getMessage();
        }

        return message;
    }
    
    /**
     * Call when the run time needs canceled.
     */
    public void cancelEnvoyRunTime() {
        try {
            runTimeTask.cancel();
        } catch (Exception ignored) {}
    }
    
    /**
     * Call when the cool downtime needs canceled.
     */
    public void cancelEnvoyCooldownTime() {
        try {
            coolDownTask.cancel();
        } catch (Exception ignored) {}
    }
    
    public List<Block> generateSpawnLocations() {
        List<Block> dropLocations = new ArrayList<>();
        int maxSpawns;

        if (envoySettings.isMaxCrateEnabled()) {
            maxSpawns = envoySettings.getMaxCrates();
        } else if (envoySettings.isRandomAmount()) {
            // Generates a random number between the min and max settings
            maxSpawns = this.random.nextInt(envoySettings.getMaxCrates() + 1 - envoySettings.getMinCrates()) + envoySettings.getMinCrates();
        } else {
            maxSpawns = envoySettings.isRandomLocationsEnabled() ? envoySettings.getMaxCrates() : spawnedLocations.size();
        }

        if (envoySettings.isRandomLocationsEnabled()) {
            if (!testCenter()) {
                return new ArrayList<>();
            }

            List<Block> minimumRadiusBlocks = getBlocks(center.clone(), envoySettings.getMinRadius());

            while (dropLocations.size() < maxSpawns) {
                int maxRadius = envoySettings.getMaxRadius();
                Location location = center.clone();
                location.add(-(maxRadius / 2) + random.nextInt(maxRadius), 0, -(maxRadius / 2) + random.nextInt(maxRadius));
                location = location.getWorld().getHighestBlockAt(location).getLocation();

                if (!location.getChunk().isLoaded() && !location.getChunk().load()) continue;
                
                if (location.getBlockY() <= 0 ||
                minimumRadiusBlocks.contains(location.getBlock()) || minimumRadiusBlocks.contains(location.clone().add(0, 1, 0).getBlock()) ||
                dropLocations.contains(location.getBlock()) || dropLocations.contains(location.clone().add(0, 1, 0).getBlock()) ||
                blacklistedBlocks.contains(location.getBlock().getType())) continue;
                
                Block block = location.getBlock();
                if (block.getType() != Material.AIR) block = block.getLocation().add(0, 1, 0).getBlock();
                
                dropLocations.add(block);
            }
            FileManager.Files.DATA.getFile().set("Locations.Spawned", getStringsFromLocationList(dropLocations));
            FileManager.Files.DATA.saveFile();
        } else {
            if (envoySettings.isMaxCrateEnabled()) {
                if (spawnLocations.size() <= maxSpawns) {
                    dropLocations.addAll(spawnLocations);
                } else {
                    while (dropLocations.size() < maxSpawns) {
                        Block block = spawnLocations.get(random.nextInt(spawnLocations.size()));

                        if (!dropLocations.contains(block)) {
                            dropLocations.add(block);
                        }
                    }
                }
            } else {
                dropLocations.addAll(spawnLocations);
            }
        }

        return dropLocations;
    }
    
    /**
     * Starts the envoy event.
     *
     * @return true if the event started successfully and false if it had an issue.
     */
    public boolean startEnvoyEvent() {
        // Called before locations are generated due to it setting those locations to air and causing
        // crates to spawn in the ground when not using falling blocks.

        deSpawnCrates();
        List<Block> dropLocations = generateSpawnLocations();

        if (envoySettings.isRandomLocationsEnabled() && !isCenterLoaded()) {
            testCenter();
        }

        if (dropLocations.isEmpty() || (envoySettings.isRandomLocationsEnabled() && !isCenterLoaded())) {
            setNextEnvoy(getEnvoyCooldown());
            resetWarnings();
            EnvoyEndEvent event = new EnvoyEndEvent(EnvoyEndEvent.EnvoyEndReason.NO_LOCATIONS_FOUND);
            plugin.getServer().getPluginManager().callEvent(event);
            Messages.NO_SPAWN_LOCATIONS_FOUND.broadcastMessage(false);
            return false;
        }
        
        for (Player player : EditControl.getEditors()) {
            EditControl.removeFakeBlocks();
            player.getInventory().removeItem(new ItemStack(Material.BEDROCK, 1));
            Messages.KICKED_FROM_EDITOR_MODE.sendMessage(player);
        }
        
        EditControl.getEditors().clear();

        if (tiers.isEmpty()) {
            plugin.getServer().broadcastMessage(Methods.getPrefix() + Methods.color("&cNo tiers were found. Please delete the Tiers folder" + " to allow it to remake the default tier files."));
            return false;
        }

        setEnvoyActive(true);
        int max = dropLocations.size();
        HashMap<String, String> placeholder = new HashMap<>();
        placeholder.put("%amount%", max + "");
        placeholder.put("%Amount%", max + "");
        Messages.STARTED.broadcastMessage(false, placeholder);

        for (Block block : dropLocations) {
            if (block != null && block.getWorld() != null) {
                boolean spawnFallingBlock = false;

                if (envoySettings.isFallingBlocksEnabled()) {
                    for (Entity entity : Methods.getNearbyEntities(block.getLocation(), 40, 40, 40)) {
                        if (entity instanceof Player) {
                            spawnFallingBlock = true;
                            break;
                        }
                    }
                } else {
                    spawnFallingBlock = false;
                }

                if (spawnFallingBlock) {
                    if (!block.getChunk().isLoaded()) {
                        block.getChunk().load();
                    }

                    FallingBlock chest = block.getWorld().spawnFallingBlock(block.getLocation().add(.5, envoySettings.getFallingHeight(), .5), envoySettings.getFallingBlockMaterial(), (byte) envoySettings.getFallingBlockDurability());
                    chest.setDropItem(false);
                    chest.setHurtEntities(false);
                    fallingBlocks.put(chest, block);
                } else {
                    Tier tier = pickRandomTier();

                    if (!block.getChunk().isLoaded()) {
                        block.getChunk().load();
                    }

                    block.setType(tier.getPlacedBlockMaterial());

                    if (tier.isHoloEnabled() && hasHologramPlugin()) {
                        hologramController.createHologram(block, tier);
                    }

                    addActiveEnvoy(block, tier);
                    addSpawnedLocation(block);

                    if (tier.getSignalFlareToggle()) {
                        startSignalFlare(block.getLocation(), tier);
                    }
                }
            }
        }

        //This is code for testing how the chance system is doing.
        //Best to set no falling blocks for best testing as its quick and don't need to wait for the blocks to drop to the ground.
//        Map<Tier, Integer> tierAmount = new HashMap<>();
//        for (Block block : spawnedLocations) {
//            Tier tier = getTier(block);
//            tierAmount.put(tier, tierAmount.getOrDefault(tier, 0) + 1);
//        }
//        for (Tier tier : tiers) {
//            System.out.println(tier.getName() + ": " + (tierAmount.getOrDefault(tier, 0)));
//        }

        runTimeTask = new BukkitRunnable() {
            @Override
            public void run() {
                EnvoyEndEvent event = new EnvoyEndEvent(EnvoyEndEvent.EnvoyEndReason.OUT_OF_TIME);
                plugin.getServer().getPluginManager().callEvent(event);
                Messages.ENDED.broadcastMessage(false);
                endEnvoyEvent();
            }
        }.runTaskLater(plugin, getTimeSeconds(envoySettings.getEnvoyRunTimer()) * 20L);

        envoyTimeLeft = getEnvoyRunTimeCalendar();
        return true;
    }
    
    /**
     * Ends the envoy event.
     */
    public void endEnvoyEvent() {
        deSpawnCrates();
        setEnvoyActive(false);
        cancelEnvoyRunTime();

        if (envoySettings.isEnvoyRunTimerEnabled()) {
            setNextEnvoy(getEnvoyCooldown());
            resetWarnings();
        }

        EnvoyControl.clearCooldowns();
    }
    
    /**
     * Get a list of all the tiers.
     *
     * @return List of all the tiers.
     */
    public List<Tier> getTiers() {
        return tiers;
    }
    
    /**
     * Get a tier from its name.
     *
     * @param tierName The name of the tier.
     * @return Returns a tier or will return null if not tier is found.
     */
    public Tier getTier(String tierName) {
        for (Tier tier : tiers) {
            if (tier.getName().equalsIgnoreCase(tierName)) {
                return tier;
            }
        }

        return null;
    }
    
    /**
     * @param loc The location the signals will be at.
     * @param tier The tier the signal is.
     */
    public void startSignalFlare(final Location loc, final Tier tier) {
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                playSignal(loc.clone().add(.5, 0, .5), tier);
            }
        }.runTaskTimer(plugin, getTimeSeconds(tier.getSignalFlareTimer()) * 20L, getTimeSeconds(tier.getSignalFlareTimer()) * 20L);

        activeSignals.put(loc, task);
    }
    
    /**
     * @param loc The location that the signal is stopping.
     */
    public void stopSignalFlare(Location loc) {
        try {
            activeSignals.get(loc).cancel();
        } catch (Exception ignored) {}

        activeSignals.remove(loc);
    }
    
    /**
     * @return The center location for the random crates.
     */
    public Location getCenter() {
        return center;
    }
    
    /**
     * Sets the center location for the random crates.
     *
     * @param loc The new center location.
     */
    public void setCenter(Location loc) {
        center = loc;
        centerString = getStringFromLocation(center);
        FileManager.Files.DATA.getFile().set("Center", getStringFromLocation(center));
        FileManager.Files.DATA.saveFile();
    }
    
    /**
     * Check if a player is ignoring the messages.
     *
     * @param uuid The player's UUID.
     * @return True if they are ignoring them and false if not.
     */
    public boolean isIgnoringMessages(UUID uuid) {
        return ignoreMessages.contains(uuid);
    }
    
    /**
     * Make a player ignore the messages.
     *
     * @param uuid The player's UUID.
     */
    public void addIgnorePlayer(UUID uuid) {
        ignoreMessages.add(uuid);
    }
    
    /**
     * Make a player stop ignoring the messages.
     *
     * @param uuid The player's UUID.
     */
    public void removeIgnorePlayer(UUID uuid) {
        ignoreMessages.remove(uuid);
    }
    
    /**
     * Used to clean all spawn locations and set them back to air.
     */
    public void cleanLocations() {
        List<Block> locations = new ArrayList<>(spawnedLocations);

        if (envoySettings.isRandomLocationsEnabled()) {
            locations.addAll(getLocationsFromStringList(FileManager.Files.DATA.getFile().getStringList("Locations.Spawned")));
        } else {
            locations.addAll(spawnLocations);
        }

        for (Block spawnedLocation : locations) {
            if (spawnedLocation != null) {
                if (!spawnedLocation.getChunk().isLoaded()) {
                    spawnedLocation.getChunk().load();
                }

                spawnedLocation.setType(Material.AIR);
                stopSignalFlare(spawnedLocation.getLocation());

                if (hasHologramPlugin()) {
                    hologramController.removeAllHolograms();
                }
            }
        }

        spawnedLocations.clear();
        FileManager.Files.DATA.getFile().set("Locations.Spawned", new ArrayList<>());
        FileManager.Files.DATA.saveFile();
    }
    
    /**
     * Add a location to the cleaning list of where crates actually spawned.
     *
     * @param block block the crate spawned at.
     */
    public void addSpawnedLocation(Block block) {
        if (!spawnedLocations.contains(block)) spawnedLocations.add(block);
    }
    
    public List<Block> getSpawnedLocations() {
        return spawnedLocations;
    }
    
    private boolean testCenter() {
        if (!isCenterLoaded()) { // Check to make sure the center exist and if not try to load it again.
            plugin.getLogger().info("Attempting to fix Center location that failed.");
            loadCenter();

            if (!isCenterLoaded()) { // If center still doesn't exist then it cancels the event.
                plugin.getLogger().info("Debug Start");
                plugin.getLogger().info("Center String: \"" + centerString + "'");
                plugin.getLogger().info("Location Object: \"" + center.toString() + "'");
                plugin.getLogger().info("World Exist: \"" + (center.getWorld() != null) + "'");
                plugin.getLogger().info("Debug End");
                plugin.getLogger().info(
                "Failed to fix Center. Will try again next event.");
                return false;
            } else {
                plugin.getLogger().info(
                "Center has been fixed and will continue event.");
            }
        }

        return true;
    }
    
    private void loadCenter() {
        FileConfiguration data = FileManager.Files.DATA.getFile();

        if (data.contains("Center")) {
            centerString = data.getString("Center");
            assert centerString != null;
            center = getLocationFromString(centerString);
        } else {
            center = plugin.getServer().getWorlds().get(0).getSpawnLocation();
        }

        if (center.getWorld() == null) {
            if (fileManager.isLogging()) plugin.getLogger().info(
            "Failed to fix Center. Will try again next event.");
        }
    }
    
    private boolean isCenterLoaded() {
        return center.getWorld() != null;
    }
    
    private void setEnvoyActive(boolean toggle) {
        envoyActive = toggle;
    }
    
    private Calendar getEnvoyCooldown() {
        Calendar cal = Calendar.getInstance();

        if (envoySettings.isEnvoyCooldownEnabled()) {
            String time = envoySettings.getEnvoyCooldown();

            for (String i : time.split(" ")) {
                if (i.contains("d")) {
                    cal.add(Calendar.DATE, Integer.parseInt(i.replace("d", "")));
                } else if (i.contains("h")) {
                    cal.add(Calendar.HOUR, Integer.parseInt(i.replace("h", "")));
                } else if (i.contains("m")) {
                    cal.add(Calendar.MINUTE, Integer.parseInt(i.replace("m", "")));
                } else if (i.contains("s")) {
                    cal.add(Calendar.SECOND, Integer.parseInt(i.replace("s", "")));
                }
            }
        } else {
            String time = envoySettings.getEnvoyClockTime();
            int hour = Integer.parseInt(time.split(" ")[0].split(":")[0]);
            int min = Integer.parseInt(time.split(" ")[0].split(":")[1]);
            int c = Calendar.AM;

            if (time.split(" ")[1].equalsIgnoreCase("AM")) {
                c = Calendar.AM;
            } else if (time.split(" ")[1].equalsIgnoreCase("PM")) {
                c = Calendar.PM;
            }

            cal.set(Calendar.HOUR_OF_DAY, hour);
            cal.getTime(); // Without this makes the hours not change for some reason.
            cal.set(Calendar.MINUTE, min);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.AM_PM, c);

            if (cal.before(Calendar.getInstance())) {
                cal.set(Calendar.DAY_OF_MONTH, cal.get(Calendar.DAY_OF_MONTH) + 1);
            }
        }

        return cal;
    }
    
    private Calendar getEnvoyRunTimeCalendar() {
        Calendar cal = Calendar.getInstance();
        String time = envoySettings.getEnvoyRunTimer().toLowerCase();

        for (String i : time.split(" ")) {
            if (i.contains("d")) {
                cal.add(Calendar.DATE, Integer.parseInt(i.replace("d", "")));
            } else if (i.contains("h")) {
                cal.add(Calendar.HOUR, Integer.parseInt(i.replace("h", "")));
            } else if (i.contains("m")) {
                cal.add(Calendar.MINUTE, Integer.parseInt(i.replace("m", "")));
            } else if (i.contains("s")) {
                cal.add(Calendar.SECOND, Integer.parseInt(i.replace("s", "")));
            }
        }

        return cal;
    }
    
    private String getStringFromLocation(Location location) {
        return "World:" + location.getWorld().getName()
        + ", X:" + location.getBlockX()
        + ", Y:" + location.getBlockY()
        + ", Z:" + location.getBlockZ();
    }
    
    private List<String> getStringsFromLocationList(List<Block> stringList) {
        ArrayList<String> strings = new ArrayList<>();

        for (Block block : stringList) {
            strings.add(getStringFromLocation(block.getLocation()));
        }

        return strings;
    }
    
    private Location getLocationFromString(String locationString) {
        World w = plugin.getServer().getWorlds().get(0);
        int x = 0;
        int y = 0;
        int z = 0;

        for (String i : locationString.toLowerCase().split(", ")) {
            if (i.startsWith("world:")) {
                w = plugin.getServer().getWorld(i.replace("world:", ""));
            } else if (i.startsWith("x:")) {
                x = Integer.parseInt(i.replace("x:", ""));
            } else if (i.startsWith("y:")) {
                y = Integer.parseInt(i.replace("y:", ""));
            } else if (i.startsWith("z:")) {
                z = Integer.parseInt(i.replace("z:", ""));
            }
        }

        return new Location(w, x, y, z);
    }

    private List<Block> getLocationsFromStringList(List<String> locationsList) {
        ArrayList<Block> locations = new ArrayList<>();

        for (String location : locationsList) {
            locations.add(getLocationFromString(location).getBlock());
        }

        return locations;
    }
    
    private void playSignal(Location loc, Tier tier) {
        List<Color> colors = tier.getFireworkColors();
        Firework firework = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta fireworkMeta = firework.getFireworkMeta();
        fireworkMeta.addEffects(FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE).withColor(colors).trail(true).flicker(false).build());
        fireworkMeta.setPower(1);
        firework.setFireworkMeta(fireworkMeta);
        FireworkDamageAPI.addFirework(firework);
    }
    
    //TODO find a better away of doing this as it causes crashes with big radius.
    private List<Block> getBlocks(Location location, int radius) {
        Location locations2 = location.clone();
        location.add(-radius, 0, -radius);
        locations2.add(radius, 0, radius);
        List<Block> locations = new ArrayList<>();
        int topBlockX = (Math.max(location.getBlockX(), locations2.getBlockX()));
        int bottomBlockX = (Math.min(location.getBlockX(), locations2.getBlockX()));
        int topBlockZ = (Math.max(location.getBlockZ(), locations2.getBlockZ()));
        int bottomBlockZ = (Math.min(location.getBlockZ(), locations2.getBlockZ()));

        if (location.getWorld() != null) {
            for (int x = bottomBlockX; x <= topBlockX; x++) {
                for (int z = bottomBlockZ; z <= topBlockZ; z++) {
                    locations.add(location.getWorld().getHighestBlockAt(x, z));
                }
            }
        }

        return locations;
    }
    
    private int getTimeSeconds(String time) {
        int seconds = 0;

        for (String i : time.split(" ")) {
            if (i.contains("d")) {
                seconds += Integer.parseInt(i.replace("d", "")) * 86400;
            } else if (i.contains("h")) {
                seconds += Integer.parseInt(i.replace("h", "")) * 3600;
            } else if (i.contains("m")) {
                seconds += Integer.parseInt(i.replace("m", "")) * 60;
            } else if (i.contains("s")) {
                seconds += Integer.parseInt(i.replace("s", ""));
            }
        }

        return seconds;
    }
    
    private Tier pickRandomTier() {
        if (cachedChances.isEmpty()) {
            for (Tier tier : tiers) {
                for (int i = 0; i < tier.getSpawnChance(); i++) {
                    cachedChances.add(tier);
                }
            }
        }

        return cachedChances.get(random.nextInt(cachedChances.size()));
    }
    
}
