package me.ancliz.minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import org.bukkit.ChatColor;
import me.ancliz.minecraft.commands.Command;
import me.ancliz.minecraft.commands.CommandManager;
import me.ancliz.minecraft.exceptions.MinecraftMessageFormatterException;
import me.ancliz.util.logging.Logger;

/**
 * Minecraft message formatting
 */
@SuppressWarnings("deprecation")
public class MMFormatter implements Reloadable, Observer {
    private final Logger logger = new Logger(this);
    private final String plugin;
    private CommandManager commandManager;

    public MMFormatter(String pluginName, CommandManager commandManager) {
       this.commandManager = commandManager;
       commandManager.addObserver(this);
       plugin = pluginName;
    }

    @Override
    public void update(Observable o, Object arg) {
        logger.debug("Updating...");
        if(o instanceof CommandManager) {
           commandManager = (CommandManager) o;
            logger.debug("Updated.");
        }
    }

    /** Delimiter for ChatColor substitution */
    public final char D = '␚';
    public ChatColor pluginColour;

    public String format(String message, ChatColor ... colours) {
        int count = message.length() - message.replace(String.valueOf(D), "").length();

        try {
            if(count == 0) {
                message = D + message;
            } else if(count < colours.length) {
                throw new MinecraftMessageFormatterException("Number of colours is greater than the number of colour characters.");
            } else if (count > colours.length) {
                throw new MinecraftMessageFormatterException("Number of colours is less than the number of colour characters.");
            }
        } catch(MinecraftMessageFormatterException e) {
            logger.warn(e.getMessage() + "\n" + logger.stackTrace(e, 5) + "\n" + e.getCause());
        }

        for(ChatColor colour : colours) {
            message = message.replaceFirst(String.valueOf(D), colour.toString());
        }

        return message;
    }

    public String pluginMessage(String message) {
        return ChatColor.RED + plugin + ": " + ChatColor.WHITE + message;
    }
    public String error(String message) {
        return ChatColor.RED + "Error: " + ChatColor.DARK_RED + message;
    }

    public String warn(String message) {
        return ChatColor.YELLOW + "Warning: " + ChatColor.RED + message;
    }

    public String broadcast(String message) {
        return format(D+"["+D+"Broadcast"+D+"] " +D+ message, ChatColor.GOLD, ChatColor.DARK_RED, ChatColor.GOLD, ChatColor.GREEN);
    }

    public String help(int page, int totalPages, int maxPageLines) {
        StringBuilder builder = new StringBuilder();

        buildHeader(builder, page, totalPages);
        buildPage(builder, getSortedCommands(), page, maxPageLines);

        if(page != totalPages) {
            builder.append(format(D+"Type "+D+"/" + plugin + " help " + (page+1) +D+ " to read the next page.",
                    ChatColor.GOLD, ChatColor.RED, ChatColor.GOLD));
        }

        return builder.toString();
    }

    private List<Command> getSortedCommands() {
        List<Command> pluginCommands = new ArrayList<>(commandManager.getTopLevel());
        pluginCommands.addAll(commandManager.getLevelOne());
        Collections.sort(pluginCommands);
        return pluginCommands;
    }

    private void buildHeader(StringBuilder builder, int page, int totalPages) {
        builder.append(format(D + "---- " + D + "Help: " + capitalise(plugin) + " " + D + "-- " + D + "Page " + D + page + D + "/" + D + totalPages + D + " ----\n",
                ChatColor.YELLOW, ChatColor.GOLD, ChatColor.YELLOW,
                ChatColor.GOLD, ChatColor.RED, ChatColor.GOLD, ChatColor.RED, ChatColor.YELLOW));
    }

    private void buildPage(StringBuilder builder, List<Command> commands, int page, int maxPageLines) {
        int startIndex = (page - 1) * maxPageLines;
        int endIndex = Math.min(startIndex + maxPageLines, commands.size());

        for(int i = startIndex; i < endIndex; i++) {
            Command cmd = commands.get(i);
            String description = (cmd.description() != null) ? cmd.description() : "No information available.";
            String topLevel = cmd.FULLY_QUALIFIED_NAME.split("\\.")[1];
    
            builder.append(buildLine(cmd, description, topLevel));
        }
    }
    
    private String buildLine(Command cmd, String description, String topLevel) {
        if(!topLevel.equals(cmd.name())) {
            return format(D+"/"+ topLevel + " " + cmd.name() +D+ ": " +  description + "\n", ChatColor.GOLD, ChatColor.WHITE);
        }
        return format(D+"/"+ cmd.name() +D+ ": " +  description + "\n", ChatColor.GOLD, ChatColor.WHITE);
    }

    private String capitalise(String w) {
        return w.substring(0, 1).toUpperCase() + w.substring(1);
    }

    public String green(String message) {
        return format(message, ChatColor.GREEN);
    }

    public String success(String message) {
        return green(message);
    }

    public String red(String message) {
        return format(message, ChatColor.RED);
    }

    @Override
    public void reload() {
        pluginColour = colour("plugin-chat-colour", AnkyPlugin.getInstance().getConfig().getString("plugin-chat-colour"));
    }

    private ChatColor colour(String path, String setColour) {
        ChatColor colour;
        try {
            colour = switch(setColour) {
                case "aqua"         -> ChatColor.AQUA;
                case "dark aqua"    -> ChatColor.DARK_AQUA;
                case "dark blue"    -> ChatColor.DARK_BLUE;
                case "dark gray"    -> ChatColor.DARK_GRAY;
                case "dark green"   -> ChatColor.DARK_GREEN;
                case "dark purple"  -> ChatColor.DARK_PURPLE;
                case "gold"         -> ChatColor.GOLD;
                case "gray"         -> ChatColor.GRAY;
                case "green"        -> ChatColor.GREEN;
                case "yellow"       -> ChatColor.YELLOW;
                case "blue"         -> ChatColor.BLUE;
                case "red"          -> ChatColor.RED;
                default             -> throw new IllegalStateException("Unavailable Colour ("
                                        + path + "), defaulting to red");
            };
        } catch(IllegalStateException error) {
            logger.warn(error.getMessage());
            colour = ChatColor.RED;
        }
        return colour;
    }
    
}