package me.badbones69.crazyenvoys;

import me.badbones69.crazyenvoys.api.CrazyManager;
import me.badbones69.crazyenvoys.api.FileManager;
import me.badbones69.crazyenvoys.api.FileManager.Files;
import me.badbones69.crazyenvoys.api.enums.Messages;
import me.badbones69.crazyenvoys.api.events.EnvoyEndEvent;
import me.badbones69.crazyenvoys.api.events.EnvoyEndEvent.EnvoyEndReason;
import me.badbones69.crazyenvoys.commands.EnvoyCommand;
import me.badbones69.crazyenvoys.commands.EnvoyTab;
import me.badbones69.crazyenvoys.controllers.EditControl;
import me.badbones69.crazyenvoys.controllers.EnvoyControl;
import me.badbones69.crazyenvoys.controllers.FireworkDamageAPI;
import me.badbones69.crazyenvoys.controllers.FlareControl;
import me.badbones69.crazyenvoys.multisupport.MVdWPlaceholderAPISupport;
import me.badbones69.crazyenvoys.multisupport.PlaceholderAPISupport;
import me.badbones69.crazyenvoys.multisupport.Support;
import me.badbones69.crazyenvoys.multisupport.ServerProtocol;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class CrazyEnvoys extends JavaPlugin implements Listener {

    private static CrazyEnvoys plugin;

    private FileManager fileManager;
    private CrazyManager crazyManager;
    
    @Override
    public void onEnable() {
        plugin = this;
        fileManager = new FileManager();
        crazyManager = new CrazyManager();

        if (ServerProtocol.isNewer(ServerProtocol.v1_16_R3)) {
            getLogger().warning("This jar only works on 1.16.X & below.");
            getServer().getPluginManager().disablePlugin(this);

            return;
        }

        String homeFolder = ServerProtocol.isNewer(ServerProtocol.v1_12_R1) ? "/tiers1.13-Up" : "/tiers1.12.2-Down";

        fileManager.logInfo(true)
        .registerCustomFilesFolder("/tiers")
        .registerDefaultGenerateFiles("Basic.yml", "/tiers", homeFolder)
        .registerDefaultGenerateFiles("Lucky.yml", "/tiers", homeFolder)
        .registerDefaultGenerateFiles("Titan.yml", "/tiers", homeFolder)
        .setup();
        
        Messages.addMissingMessages();
        
        crazyManager.load();
        
        PluginManager pluginManager = getServer().getPluginManager();

        pluginManager.registerEvents(this, this);
        pluginManager.registerEvents(new EditControl(), this);
        pluginManager.registerEvents(new EnvoyControl(), this);
        pluginManager.registerEvents(new FlareControl(), this);

        FileConfiguration config = Files.CONFIG.getFile();

        boolean metricsEnabled = config.getBoolean("Settings.Toggle-Metrics");
        String metricsPath = config.getString("Settings.Toggle-Metrics");

        if (metricsPath == null) {
            config.set("Settings.Toggle-Metrics", true);

            Files.CONFIG.saveFile();
        }

        if (metricsEnabled) new Metrics(this, 4537);

        try {
            if (ServerProtocol.isNewer(ServerProtocol.v1_10_R1)) {
                pluginManager.registerEvents(new FireworkDamageAPI(), this);
            }
        } catch (Exception ignored) {}

        if (Support.PLACEHOLDER_API.isPluginLoaded()) new PlaceholderAPISupport().register();
        if (Support.MVDW_PLACEHOLDER_API.isPluginLoaded()) MVdWPlaceholderAPISupport.registerPlaceholders();

        registerCommand(getCommand("crazyenvoys"), new EnvoyTab(), new EnvoyCommand());
    }

    private void registerCommand(PluginCommand pluginCommand, TabCompleter tabCompleter, CommandExecutor commandExecutor) {
        if (pluginCommand != null) {
            pluginCommand.setExecutor(commandExecutor);

            if (tabCompleter != null) pluginCommand.setTabCompleter(tabCompleter);
        }
    }

    @Override
    public void onDisable() {
        for (Player player : getServer().getOnlinePlayers()) {
            if (EditControl.isEditor(player)) {
                EditControl.removeEditor(player);
                EditControl.removeFakeBlocks();
            }
        }

        if (crazyManager.isEnvoyActive()) {
            EnvoyEndEvent event = new EnvoyEndEvent(EnvoyEndReason.SHUTDOWN);
            getServer().getPluginManager().callEvent(event);
            crazyManager.endEnvoyEvent();
        }

        crazyManager.unload();
    }

    public static CrazyEnvoys getPlugin() {
        return plugin;
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public CrazyManager getCrazyManager() {
        return crazyManager;
    }
}