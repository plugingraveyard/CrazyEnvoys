package me.badbones69.crazyenvoys;

import me.badbones69.crazyenvoys.api.CrazyManager;
import me.badbones69.crazyenvoys.api.FileManager;
import me.badbones69.crazyenvoys.api.FileManager.Files;
import me.badbones69.crazyenvoys.api.enums.Messages;
import me.badbones69.crazyenvoys.controllers.FireworkDamageAPI;
import me.badbones69.crazyenvoys.multisupport.ServerProtocol;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Methods {
    
    private static final Random random = new Random();
    
    public final static Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");

    private static final CrazyEnvoys plugin = CrazyEnvoys.getPlugin();
    
    public static String color(String message) {
        if (ServerProtocol.isNewer(ServerProtocol.v1_15_R1)) {
            Matcher matcher = HEX_PATTERN.matcher(message);
            StringBuffer buffer = new StringBuffer();

            while (matcher.find()) {
                matcher.appendReplacement(buffer, net.md_5.bungee.api.ChatColor.of(matcher.group()).toString());
            }

            return ChatColor.translateAlternateColorCodes('&', matcher.appendTail(buffer).toString());
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public static String removeColor(String msg) {
        return ChatColor.stripColor(msg);
    }
    
    public static String getPrefix() {
        return color(Files.CONFIG.getFile().getString("Settings.Prefix"));
    }
    
    public static String getPrefix(String message) {
        return color(Files.CONFIG.getFile().getString("Settings.Prefix") + message);
    }
    
    @SuppressWarnings({"deprecation", "squid:CallToDeprecatedMethod"})
    public static ItemStack getItemInHand(Player player) {
        if (ServerProtocol.isNewer(ServerProtocol.v1_8_R3)) {
            return player.getInventory().getItemInMainHand();
        } else {
            return player.getItemInHand();
        }
    }
    
    public static boolean isInt(String s) {
        try {
            Integer.parseInt(s);
        } catch (NumberFormatException nfe) {
            return false;
        }

        return true;
    }
    
    public static boolean isOnline(String name) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }

        return false;
    }
    
    public static Player getPlayer(String name) {
        return plugin.getServer().getPlayer(name);
    }
    
    public static boolean isInvFull(Player player) {
        return player.getInventory().firstEmpty() == -1;
    }
    
    public static Entity fireWork(Location loc, List<Color> colors) {
        Firework firework = loc.getWorld().spawn(loc, Firework.class);
        FireworkMeta fireworkMeta = firework.getFireworkMeta();
        fireworkMeta.addEffects(FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE).withColor(colors).trail(false).flicker(false).build());
        fireworkMeta.setPower(0);
        firework.setFireworkMeta(fireworkMeta);
        FireworkDamageAPI.addFirework(firework);
        detonate(firework);

        return firework;
    }
    
    private static void detonate(final Firework f) {
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, f :: detonate, 2);
    }
    
    public static Color getColor(String color) {
        if (color != null) {
            switch (color.toUpperCase()) {
                case "AQUA":
                    return Color.AQUA;
                case "BLACK":
                    return Color.BLACK;
                case "BLUE":
                    return Color.BLUE;
                case "FUCHSIA":
                    return Color.FUCHSIA;
                case "GRAY":
                    return Color.GRAY;
                case "GREEN":
                    return Color.GREEN;
                case "LIME":
                    return Color.LIME;
                case "MAROON":
                    return Color.MAROON;
                case "NAVY":
                    return Color.NAVY;
                case "OLIVE":
                    return Color.OLIVE;
                case "ORANGE":
                    return Color.ORANGE;
                case "PURPLE":
                    return Color.PURPLE;
                case "RED":
                    return Color.RED;
                case "SILVER":
                    return Color.SILVER;
                case "TEAL":
                    return Color.TEAL;
                case "WHITE":
                    return Color.WHITE;
                case "YELLOW":
                    return Color.YELLOW;
            }
            
            try {
                String[] rgb = color.split(",");
                return Color.fromRGB(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]), Integer.parseInt(rgb[2]));
            } catch (Exception ignore) {
            }
        }
        return Color.WHITE;
    }
    
    public static List<String> getPage(List<String> list, Integer page) {
        List<String> locations = new ArrayList<>();
        if (page <= 0) {
            page = 1;
        }
        int max = 10;
        int index = page * max - max;
        int endIndex = index >= list.size() ? list.size() - 1 : index + max;
        for (; index < endIndex; index++) {
            if (index < list.size()) {
                locations.add(list.get(index));
            }
        }
        for (; locations.isEmpty(); page--) {
            if (page <= 0) {
                break;
            }
            index = page * max - max;
            endIndex = index >= list.size() ? list.size() - 1 : index + max;
            for (; index < endIndex; index++) {
                if (index < list.size()) {
                    locations.add(list.get(index));
                }
            }
        }
        return locations;
    }
    
    public static boolean isSuccessful(int min, int max) {
        if (max <= min || max <= 0) {
            return true;
        }

        int chance = 1 + random.nextInt(max);
        return chance <= min;
    }
    
    @SuppressWarnings({"deprecation", "squid:CallToDeprecatedMethod"})
    public static List<Entity> getNearbyEntities(Location loc, double x, double y, double z) {
        List<Entity> out = new ArrayList<>();

        if (loc.getWorld() != null) {
            FallingBlock ent = loc.getWorld().spawnFallingBlock(loc.subtract(0, 0, 0), Material.AIR, (byte) 0);
            out = ent.getNearbyEntities(x, y, z);
            ent.remove();
        }

        return out;
    }
    
    public static String convertTimeToString(Calendar timeTill) {
        Calendar rightNow = Calendar.getInstance();
        int total = ((int) (timeTill.getTimeInMillis() / 1000) - (int) (rightNow.getTimeInMillis() / 1000));
        int day = 0;
        int hour = 0;
        int minute = 0;
        int second = 0;

        for (; total > 86400; total -= 86400, day++) ;
        for (; total > 3600; total -= 3600, hour++) ;
        for (; total >= 60; total -= 60, minute++) ;

        second += total;
        String message = "";

        if (day > 0) message += day + Messages.DAY.getMessage() + ", ";
        if (day > 0 || hour > 0) message += hour + Messages.HOUR.getMessage() + ", ";
        if (day > 0 || hour > 0 || minute > 0) message += minute + Messages.MINUTE.getMessage() + ", ";
        if (day > 0 || hour > 0 || minute > 0 || second > 0) message += second + Messages.SECOND.getMessage() + ", ";
        if (message.length() < 2) {
            message = "0" + Messages.SECOND.getMessage();
        } else {
            message = message.substring(0, message.length() - 2);
        }

        return message;
    }
    
}