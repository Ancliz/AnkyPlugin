package me.ancliz.minecraft.commands;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import me.ancliz.minecraft.exceptions.CommandDisabledException;
import me.ancliz.minecraft.exceptions.NotRegisteredException;

public class Command implements Comparable<Command> {
    public final String FULLY_QUALIFIED_NAME;
    public final String PATH;
    private boolean enabled;
    private final String name;
    private List<String> aliases = new ArrayList<>();
    private String description = "";
    private String usage = "";
    private List<Command> subCommands;
    private CommandHandler handler;

    public Command(ConfigurationSection command, String fullyQualifiedName) {
        this.FULLY_QUALIFIED_NAME = fullyQualifiedName;
        this.PATH = command.getCurrentPath();
        this.name = command.getName();
        this.aliases = command.getStringList("aliases");
        this.description = command.getString("description");
        this.usage = command.getString("usage");
        this.enabled = command.getBoolean("enabled", true);
    }

    public Command(ConfigurationSection command, String fullyQualifiedName, List<Command> subCommands) {
        this.FULLY_QUALIFIED_NAME = fullyQualifiedName;
        this.PATH = command.getCurrentPath();
        this.name = command.getName();
        this.aliases = command.getStringList("aliases");
        this.description = command.getString("description");
        this.usage = command.getString("usage");
        this.subCommands = subCommands;
        this.enabled = command.getBoolean("enabled", true);
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
        enabled = true;
    }

    public void disable() {
        enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean invoke(CommandSender sender, String[] args) {
        if(handler == null) { throw new NotRegisteredException("CommandHandler has not been registered for " + FULLY_QUALIFIED_NAME); } 
        else if(!enabled)   { throw new CommandDisabledException();                                                   }
        return handler.invoke(sender, args);
    }

    public void setHandler(CommandHandler handler) {
        this.handler = handler;
    }
    
    @Override
    public String toString() {
        return String.format("{FQN: %s, enabled: %b}", FULLY_QUALIFIED_NAME, enabled);
    }

	@Override
	public int compareTo(Command o) {
        return FULLY_QUALIFIED_NAME.compareTo(o.FULLY_QUALIFIED_NAME);
	}

}