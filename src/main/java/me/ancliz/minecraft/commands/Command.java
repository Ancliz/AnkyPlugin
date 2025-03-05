package me.ancliz.minecraft.commands;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import me.ancliz.minecraft.exceptions.NotRegisteredException;
import me.ancliz.util.BinaryState;

public class Command {
    private BinaryState enabled;
    public final String FULLY_QUALIFIED_NAME;
    private final String name;
    private List<String> aliases = new ArrayList<>();
    private String description = "";
    private String usage = "";
    private List<Command> subCommands;
    private CommandHandler handler;

    public Command(ConfigurationSection command) {
        this.FULLY_QUALIFIED_NAME = command.getCurrentPath();
        this.name = command.getName();
        this.aliases = command.getStringList("aliases");
        this.description = command.getString("description");
        this.usage = command.getString("usage");
        this.enabled = new BinaryState(command.getBoolean("enabled", true));
    }

    public Command(ConfigurationSection command, List<Command> subCommands) {
        this.FULLY_QUALIFIED_NAME = command.getCurrentPath();
        this.name = command.getName();
        this.aliases = command.getStringList("aliases");
        this.description = command.getString("description");
        this.usage = command.getString("usage");
        this.subCommands = subCommands;
        this.enabled = new BinaryState(command.getBoolean("enabled", true));
    }

    public List<String> aliases() {
        return aliases;
    }

    public String description() {
        return description;
    }

    public String usage() {
        return usage;
    }

    public String name() {
        return name;
    }

    public List<Command> subCommands() {
        return subCommands;
    }
    
    public void setSubCommands(List<Command> subCommands) {
        this.subCommands = subCommands;
    }
    public void enable() {
        enabled.setState(true);
    }

    public void disable() {
        enabled.setState(false);
    }

    public boolean isEnabled() {
        return enabled.getState();
    }

    public boolean invoke(CommandSender sender, String[] args) {
        if(handler == null) {
            throw new NotRegisteredException("CommandHandler has not been registered for " + FULLY_QUALIFIED_NAME.replaceFirst("commands.", ""));
        }

        return enabled.getState() ? handler.invoke(sender, args) : false;
    }

    public void setHandler(CommandHandler handler) {
        this.handler = handler;
    }
    
    @Override
    public String toString() {
        return String.format("FQN: %s, enabled: %b", FULLY_QUALIFIED_NAME, enabled);
    }

}