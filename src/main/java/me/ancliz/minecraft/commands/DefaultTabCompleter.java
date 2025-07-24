package me.ancliz.minecraft.commands;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;

public class DefaultTabCompleter implements TabCompleter {
    private static ConfigurationSection root;
    private CommandManager commandManager;
    private String rootPath;
    Set<String> completions;

    public DefaultTabCompleter(CommandManager commandManager, String commandName) {
        this.commandManager = commandManager;
        root = commandManager.getCommand(commandName + ".sub-commands");
        rootPath = root.getCurrentPath();
        reload();
    }

    public void reload() {}

    protected String constructPath(String[] ancestors) {
        ancestors = Arrays.stream(ancestors).filter(s -> !s.isEmpty()).toArray(String[]::new);
        String path = rootPath.replaceFirst("commands.","") + ".";
        path = ancestors.length > 1
            ? path + String.join(".sub-commands.", ancestors)
            : path + ancestors[0];

        return path;
    }

    protected Set<String> getCompletions(String[] ancestors) {
        ConfigurationSection commandSection = commandManager.getCommand(constructPath(ancestors));
        if(commandSection != null) {
            return Optional.ofNullable(commandSection.getConfigurationSection("sub-commands"))
                .map(cmds -> cmds.getKeys(false))
                .orElseGet(HashSet::new);
        }

        return new HashSet<>();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        String current = args[args.length-1];
        String[] ancestors = Arrays.copyOfRange(args, 0, args.length-1);
        completions = args.length == 1 ? root.getKeys(false) : getCompletions(ancestors);
        return completions.stream().filter(cmd -> cmd.toLowerCase().startsWith(current.toLowerCase())).toList();
    }
    
}