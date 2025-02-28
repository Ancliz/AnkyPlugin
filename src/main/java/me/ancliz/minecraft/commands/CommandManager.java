package me.ancliz.minecraft.commands;

import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import org.bukkit.configuration.ConfigurationSection;
import me.ancliz.minecraft.AnkyPlugin;
import me.ancliz.util.logging.Logger;

@SuppressWarnings("deprecation")
public class CommandManager extends Observable {
    Logger logger = new Logger(getClass());
    private ConfigurationSection commands;
    
    public CommandManager() {
        reload();
    }

    public void reload() {
        commands = AnkyPlugin.getInstance().getYaml("plugin.yml", true).getConfigurationSection("commands");

        if(commands == null) {
            throw new RuntimeException("Commands entry not found.");   
        }

        logger.info("Notifying Observers");
        setChanged();
        notifyObservers();
    }

    public ConfigurationSection getCommands() {
        return commands;
    }

    public ConfigurationSection getCommand(String path) {
        return getCommand(commands, path);
    }

    public ConfigurationSection getCommand(ConfigurationSection root, String path) {
        return root.getConfigurationSection(path);
     }
    
    public ConfigurationSection findCommand(String cmd, String[] args) {
        return findCommand(commands, cmd, args);
    }

    public ConfigurationSection findCommand(ConfigurationSection root, String cmd, String[] args) {
        ConfigurationSection command = null;
        String path = cmd + ".sub-commands." + String.join(".sub-commands.", args);
        try {
            do {
                logger.debug("path: {}, cmd: {}, args: {}", path, cmd, Arrays.toString(args));
                command = root.getConfigurationSection(path);
                path = path.substring(0, path.lastIndexOf(".sub-commands."));
                logger.debug("updated path: {}", path);
            } while(command == null);
        } catch(IndexOutOfBoundsException e) {
            command = root.getConfigurationSection(cmd);
        }
        logger.debug("command: {}", command);
        return command;
    }

    public List<String> getCommandsList(String path) {
        return commands.getConfigurationSection(path).getKeys(false).stream().toList();
    }

}