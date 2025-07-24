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
    
    public ConfigurationSection getCommandsSection() {
        return commandsSection;
    }

    public List<Command> getCommandsMap() {
        return new ArrayList<>(commandsMap.values());
    }

    public ConfigurationSection getCommand(String path) {
        return getCommand(commandsSection, path);
    }

    public Command getCommandInstance(String path) {
        return commandsMap.get(path);
    }

    public ConfigurationSection getCommand(ConfigurationSection root, String path) {
        return root.getConfigurationSection(path);
    }

    public ConfigurationSection getSubCommands(ConfigurationSection root, String path) {
        return root.getConfigurationSection(path + ".sub-commands");
    }

    public ConfigurationSection getSubCommands(String path) {
        return commandsSection.getConfigurationSection(path + ".sub-commands");
    }   
    
    public List<String> getCommandsList(String path) {
        return commandsSection.getConfigurationSection(path).getKeys(false).stream().toList();
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

    public List<Command> getTopLevel() {
        List<Command> list = new ArrayList<>();
        commandsMap.entrySet().stream()
            .filter(entry -> entry.getKey().matches("^[a-zA-Z0-9]+$"))
            .forEach(entry -> list.add(entry.getValue()));
        return list;
    }

    public List<Command> getLevelOne() {
        List<Command> list = new ArrayList<>();
        commandsMap.entrySet().stream()
            .filter(entry -> entry.getKey().matches("^[a-zA-Z0-9]+\\.[a-zA-Z0-9]+$"))
            .forEach(entry -> list.add(entry.getValue()));
        return list;
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

    public void printCommandsMap() {
        StringBuilder sb = new StringBuilder();
        commandsMap.forEach((k, v) -> {
            sb.append(k + ": " + v.name() + "\n");
        });
        logger.debug(sb.toString());
    }

}