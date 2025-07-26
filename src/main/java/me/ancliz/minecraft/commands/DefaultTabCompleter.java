package me.ancliz.minecraft.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

public class DefaultTabCompleter implements TabCompleter {
    private CommandManager commandManager;
    private List<String> completions;
    
    
    public DefaultTabCompleter(CommandManager commandManager, String commandName) {
        this.commandManager = commandManager;
        reload();
    }

    protected String constructPath(String command, String[] ancestors) {
        String path = Arrays.stream(ancestors)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.joining("."));
        return path.isEmpty() ? command : command + "." + path;   
    }

    protected List<String> getCompletions(String commandStr, String[] ancestors) {
        Command cmd = commandManager.getCommand(constructPath(commandStr, ancestors));
        return Optional.ofNullable(cmd)
            .map(command -> command.subCommands()
                .stream()
                .map(Command::name)
                .toList())
            .orElse(new ArrayList<>());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        String current = args[args.length-1].toLowerCase();
        String[] ancestors = Arrays.copyOf(args, args.length-1);
        completions = getCompletions(command.getName(), ancestors);
        return completions.stream()
            .filter(cmd -> cmd.toLowerCase()
            .startsWith(current))
            .toList();
    }
    
    public void reload() {}
    
}