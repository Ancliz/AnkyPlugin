package me.ancliz.minecraft.commands;

import java.util.Arrays;
import java.util.Optional;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.ancliz.minecraft.AnkyPlugin;
import me.ancliz.minecraft.MMFormatter;
import me.ancliz.minecraft.exceptions.CommandDisabledException;
import me.ancliz.minecraft.exceptions.NotRegisteredException;
import me.ancliz.util.logging.Logger;

public class DefaultCommandExecutor implements CommandExecutor {
    private Logger logger = new Logger(this.getClass());
    protected CommandManager commandManager;
    protected MMFormatter formatter;

    public DefaultCommandExecutor(CommandManager commandManager) {
        this.commandManager = commandManager;
        formatter = new MMFormatter(AnkyPlugin.getInstance().getName(), commandManager);
    }

    private Optional<String> findAlias(String command, String aliasCandidate) {
        return commandManager.getCommandInstance(command)
            .subCommands()
            .stream()
            .filter(sub -> sub.aliases().contains(aliasCandidate))
            .map(Command::name)
            .findFirst();
    }

    private String[] resolveAliases(String command, String[] args) {
        if(args.length == 0) return args;

        return findAlias(command, args[0]).map(alias -> {
                args[0] = alias;
                return args;
            }).orElse(args);
    }

    @SuppressWarnings("deprecation")
    protected void sendMessage(CommandSender sender, String message, ChatColor ... colours) {
        if(sender instanceof Player player) {
            player.sendMessage(formatter.format(message, colours));
        } else {
            sender.sendMessage(message.replaceAll(""+formatter.D, ""));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command bukkitCommand, String label, String[] args) {
        args = resolveAliases(
            bukkitCommand.getName(),
            Arrays.stream(args).filter(s -> !s.isEmpty()).toArray(String[]::new)
        );

        String path = bukkitCommand.getName() + "." + String.join(".", args);
        Command command = commandManager.findCommandInMap(path);

        if(command == null) {
            return false;
        }

        String[] commandArgs = parseArguments(command.PATH.split("\\."), args);

        if(command.isEnabled()) {
            if(!invokeCommand(sender, command, commandArgs)) {
                sendMessage(sender, command.usage());
            }
        } else {
            sendMessage(sender, "This command has been disabled.");
        }

        return true;
    }

    private boolean invokeCommand(CommandSender sender, Command command, String[] commandArgs) {
        try {
            return command.invoke(sender, commandArgs);
        } catch(CommandDisabledException e) {
            sendMessage(sender, e.getMessage());
            logger.info("'{}' tried executing command '{}' but it has been disabled.", sender.getName(), command.name());
            return true;
        } catch(NotRegisteredException e) {
            sendMessage(sender, formatter.error("This command is unavailable due to an error."));
            logger.error("Command {} has no registered handler. This is not a configuration error, " +
                "there is a fundamental failure in the plugin code. Contact the plugin vendor for a remedy.",
                command.PATH);
            return true;
        }
    }

    private String[] parseArguments(String[] commandTokenised, String[] args) {
        return args.length == 1
            ? new String[0]
            : Arrays.copyOfRange(args, commandTokenised.length-1, args.length);
    }

}