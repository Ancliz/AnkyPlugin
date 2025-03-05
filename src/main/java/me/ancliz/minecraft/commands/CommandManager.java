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
    private ConfigurationSection commands;
    private Map<String, Command> commands2;

    public CommandManager() {
        reload();
    }

    public void reload() {
        commands = AnkyPlugin.getInstance().getYaml("plugin.yml", true).getConfigurationSection("commands");
        Preconditions.checkNotNull(commands, "Commands entry in plugin.yml not found.");
        commands2 = commands2 == null ? new HashMap<>() : commands2;

        for(String cmd : commands.getKeys(false)) {
            createCommandInstance(commands.getConfigurationSection(cmd), cmd);
        }

        logger.info("Notifying Observers");
        setChanged();
        notifyObservers();
    }

    private void createCommandInstance(ConfigurationSection command, String commandName) {
        if(command == null) {
            return;
        }

        ConfigurationSection subCommands = command.getConfigurationSection("sub-commands");
        List<Command> subCommandsList = new ArrayList<>();

        Command cmd = new Command(command);
        String commandPath = asCommands2Path(command);
        logger.debug("{} - adding command to commands2 with path: {}", commandName, commandPath);
        commands2.put(commandPath, cmd);

        if(subCommands != null) {
            Set<String> subCommandsKeys = subCommands.getKeys(false);

            for(String sub : subCommandsKeys) {

                String subPath = asCommands2Path(subCommands.getConfigurationSection(sub));
                logger.debug("{} - creating subcommand with path: {}", commandName, subPath);
                createCommandInstance(command.getConfigurationSection("sub-commands." + sub), sub);
                Command subCmd = commands2.get(subPath);

                if(subCmd != null) {
                    subCommandsList.add(subCmd);
                }
            }
        }

        cmd.setSubCommands(subCommandsList);
    }

    public String asCommands2Path(ConfigurationSection command) {
        return command.getCurrentPath().replaceFirst("commands.", "");
    }

    public List<Command> getTopLevel() {
        List<Command> list = new ArrayList<>();
        commands2.entrySet().stream()
            .filter(entry -> entry.getKey().matches("^[a-zA-Z0-9]+$"))
            .forEach(entry -> list.add(entry.getValue()));
        return list;
    }

    public List<Command> getLevelOne() {
        List<Command> list = new ArrayList<>();
        commands2.entrySet().stream()
            .filter(entry -> entry.getKey().matches("^[a-zA-Z0-9]+\\.sub-commands\\.[a-zA-Z0-9]+$"))
            .forEach(entry -> list.add(entry.getValue()));
        return list;
    }

    public ConfigurationSection getCommands() {
        return commands;
    }

    public List<Command> getCommands2() {
        return new ArrayList<>(commands2.values());
    }

    public ConfigurationSection getCommand(String path) {
        return getCommand(commands, path);
    }

    public Command getCommandInstance(String path) {
        return commands2.get(path);
    }

    public ConfigurationSection getCommand(ConfigurationSection root, String path) {
        return root.getConfigurationSection(path);
    }

    public ConfigurationSection getSubCommands(ConfigurationSection root, String path) {
        return root.getConfigurationSection(path + ".sub-commands");
    }

    public ConfigurationSection getSubCommands(String path) {
        return commands.getConfigurationSection(path + ".sub-commands");
    }

    public ConfigurationSection findCommand(String cmd, String[] args) {
        return findCommand(commands, cmd, args);
    }

    public ConfigurationSection findCommand(ConfigurationSection root, String cmd, String[] args) {
        ConfigurationSection command = null;
        String path = cmd + ".sub-commands." + String.join(".sub-commands.", args);
        try {
            do {
                // logger.debug("path: {}, cmd: {}, args: {}", path, cmd, Arrays.toString(args));

                command = root.getConfigurationSection(path);
                path = path.substring(0, path.lastIndexOf(".sub-commands."));

                // logger.debug("updated path: {}", path);
            } while(command == null);
        } catch(IndexOutOfBoundsException e) {
            command = root.getConfigurationSection(cmd);
        }
        
        // logger.debug("command: {}", command);
        return command;
    }
    
    /*
    public Command findCommand2(Command next) {
        if(next == null) {
            return next;
        }

        for(String cmd : next.subCommands()) {
            if(cmd.equals(next.name())) {
                next = commands2.get(cmd);
            }
        }

        return findCommand2(next);
    }
    */

    public List<String> getCommandsList(String path) {
        return commands.getConfigurationSection(path).getKeys(false).stream().toList();
    }

    public void registerHandlers(Map<String, CommandHandler> commandHandlers) {
        commandHandlers.forEach(this::registerHandler);
    }

    public void registerHandler(String command, CommandHandler handler) {
        Preconditions.checkNotNull(commands.getConfigurationSection(command),
                "Invalid fully qualified command name {}, handler not set - is it in plugin.yml?",
                command);
        commands2.get(command).setHandler(handler);
    }


    public void printCommands2() {
        StringBuilder sb = new StringBuilder();
        commands2.forEach((k, v) -> {
            sb.append(k + ": " + v.name() + "\n");
        });
        logger.debug(sb.toString());
    }

    @SuppressWarnings("unused")
    @Deprecated(forRemoval = true)
    private void commandPathsMethod() {
        Set<String> commandPaths = commands.getKeys(true);

        for(String path : commandPaths) {

            logger.trace("path: {}", path);
            Object entry = commands.get(path);

            if(entry instanceof ConfigurationSection) {

                ConfigurationSection command = (ConfigurationSection) entry;

                if(!command.getName().equals("sub-commands")) {

                    if(commands2.get(path) == null) {

                        logger.debug("adding command: {}", path);
                        commands2.put(path, new Command(command));
                    }
                }
            }
        }
    }

}