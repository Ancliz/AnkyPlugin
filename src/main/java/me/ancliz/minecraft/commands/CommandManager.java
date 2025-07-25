package me.ancliz.minecraft.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import com.google.common.base.Preconditions;
import me.ancliz.minecraft.AnkyPlugin;
import me.ancliz.util.logging.Logger;

@SuppressWarnings("deprecation")
public class CommandManager extends Observable {
    Logger logger = new Logger(getClass());
    private ConfigurationSection commandsSection;
    private Map<String, Command> commandsMap;

    public CommandManager() {
        reload();
    }

    public void reload() {
        commandsSection = AnkyPlugin.getInstance().getYaml("plugin.yml", true).getConfigurationSection("commands");
        Preconditions.checkNotNull(commandsSection, "Commands entry in plugin.yml not found.");
        commandsMap = commandsMap == null ? new HashMap<>() : commandsMap;

        for(String cmd : commandsSection.getKeys(false)) {
            createCommandInstance(commandsSection.getConfigurationSection(cmd), cmd);
        }

        logger.info("Notifying Observers");
        setChanged();
        notifyObservers();
    }

    private void createCommandInstance(ConfigurationSection command, String commandName) {
        if(command == null) {
            logger.debug("no ConfigurationSection for command '{}'", commandName);
            return;
        }

        ConfigurationSection subCommands = command.getConfigurationSection("sub-commands");
        List<Command> subCommandsList = new ArrayList<>();
        String commandPath = asCommandsMapPath(command);
        Command cmd = new Command(command, commandPath);
        logger.trace("{} - adding command to commandsMap with path: {}", commandName, commandPath);
        commandsMap.put(commandPath, cmd);

        if(subCommands != null) {
            Set<String> subCommandsKeys = subCommands.getKeys(false);

            for(String sub : subCommandsKeys) {
                String subPath = asCommandsMapPath(subCommands.getConfigurationSection(sub));
                logger.trace("{} - creating subcommand with path: {}", commandName, subPath);
                createCommandInstance(command.getConfigurationSection("sub-commands." + sub), sub);
                Command subCmd = commandsMap.get(subPath);

                if(subCmd != null) {
                    subCommandsList.add(subCmd);
                }
            }
        }

        cmd.setSubCommands(subCommandsList);
    }

    public String asCommandsMapPath(ConfigurationSection command) {
        return command.getCurrentPath().replaceFirst("commands.", "").replaceAll(".sub-commands", "");
    }

    public String asCommandsMapPath(String path) {
        return path.replaceFirst("commands.", "").replaceAll(".sub-commands", "");
    }
    
    public List<String> getCommandsNameList(String path) {
        return commandsMap.keySet().stream().filter(key -> key.contains(path)).toList();
    }

    public List<Command> getCommandsMap() {
        return new ArrayList<>(commandsMap.values());
    }

    public Command getCommand(String path) {
        return commandsMap.get(path);
    }

    public List<Command> getSubCommands(String path) {
        return commandsMap.get(path).subCommands();
    }
    
    public List<Command> getTopLevel() {
        return getLevel(0);
    }

    public List<Command> getLevelOne() {
        return getLevel(1);
    }

    public List<Command> getLevel(int level) {
        String regex = "^[a-zA-Z0-9]+(\\.[a-zA-Z0-9]+){0," + (level - 1) + "$}";
        return commandsMap.entrySet().stream()
            .filter(entry -> entry.getKey().matches(regex))
            .map(Map.Entry::getValue)
            .toList();
    }

    public Command findCommandInMap(String path) {
        Command command = null;
        try {
            do {
                command = commandsMap.get(path);
                path = path.substring(0, path.lastIndexOf("."));
            } while(command == null);
        } catch(IndexOutOfBoundsException e) {}

        return command; 
    }

    public ConfigurationSection findCommand(String cmd, String[] args) {
        return findCommand(commandsSection, cmd, args);
    }

    public ConfigurationSection findCommand(ConfigurationSection root, String cmd, String[] args) {
        ConfigurationSection command = null;
        String path = cmd + ".sub-commands." + String.join(".sub-commands.", args);
        try {
            do {
                command = root.getConfigurationSection(path);
                path = path.substring(0, path.lastIndexOf(".sub-commands."));
            } while(command == null);
        } catch(IndexOutOfBoundsException e) {
            command = root.getConfigurationSection(cmd);
        }
        return command;
    }

    public ConfigurationSection getCommandsSection() {
        return commandsSection;
    }

    public ConfigurationSection getCommandSection(String path) {
        return getCommandSection(commandsSection, path);
    }

    public ConfigurationSection getCommandSection(ConfigurationSection root, String path) {
        return root.getConfigurationSection(path);
    }

    public void registerHandlers(Map<String, CommandHandler> commandHandlers) {
        commandHandlers.forEach(this::registerHandler);
    }

    public void registerHandler(String command, CommandHandler handler) {
        Preconditions.checkNotNull("commands." + commandsMap.get(command).FULLY_QUALIFIED_NAME,
                "Invalid fully qualified command name {}, handler not set - is it in plugin.yml?",
                command);
        commandsMap.get(command).setHandler(handler);
    }

}