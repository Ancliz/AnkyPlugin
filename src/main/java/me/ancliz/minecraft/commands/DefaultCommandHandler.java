package me.ancliz.minecraft.commands;

import java.util.Arrays;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import me.ancliz.minecraft.AnkyPlugin;
import me.ancliz.minecraft.MMFormatter;
import me.ancliz.minecraft.exceptions.CommandDisabledException;
import me.ancliz.minecraft.exceptions.NotRegisteredException;
import me.ancliz.util.logging.Logger;

public class DefaultCommandHandler implements CommandExecutor {
    private Logger logger = new Logger(this.getClass());
    protected CommandManager commandManager;
    protected MMFormatter formatter;

    public DefaultCommandHandler(CommandManager commandManager) {
        this.commandManager = commandManager;
        formatter = new MMFormatter(AnkyPlugin.getInstance().getName(), commandManager);
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command bukkitCommand, String label, String[] args) {
        String path = bukkitCommand.getName() + "." + String.join(".", args);
        Command command = commandManager.findCommandInMap(path);

        if(command == null) {
            return false;
        }

        String[] commandArgs = parseArguments(command.PATH.split("\\."), args);

        if(command.isEnabled()) {
            if(!invokeCommand(sender, command, commandArgs)) {
                sender.sendMessage(formatter.pluginMessage(command.usage()));
            }
        } else {
            sender.sendMessage(formatter.pluginMessage("This command has been disabled."));
        }

        return true;
    }

    private boolean invokeCommand(CommandSender sender, Command command, String[] commandArgs) {
        try {
            return command.invoke(sender, commandArgs);
        } catch(CommandDisabledException e) {
            sender.sendMessage(e.getMessage());
            logger.info("'{}' tried executing command '{}' but it has been disabled.", sender.getName(), command.name());
            return true;
        } catch(NotRegisteredException e) {
            sender.sendMessage(formatter.error("This command is unavailable due to an error."));
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