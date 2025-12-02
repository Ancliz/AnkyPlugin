package me.ancliz.minecraft.commands;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.ancliz.minecraft.exceptions.CommandDisabledException;
import me.ancliz.minecraft.exceptions.NotRegisteredException;

public class BrigadierCommand<S> implements Comparable<BrigadierCommand<S>>, Command<S> {
    public final String FULLY_QUALIFIED_NAME;
    private LiteralCommandNode<S> brigadierNode;
    private boolean enabled;
    private final String name;
    private List<String> aliases = new ArrayList<>();
    private String description = "";
    private String usage = "";
    private com.mojang.brigadier.Command<S> handler;
    
    
    public BrigadierCommand(ConfigurationSection command, String fullyQualifiedName) {
        this.FULLY_QUALIFIED_NAME = fullyQualifiedName;
        this.name = command.getName();
        this.aliases = command.getStringList("aliases");
        this.description = command.getString("description", "");
        this.usage = command.getString("usage", "");
        this.enabled = command.getBoolean("enabled", true);
    }

    public LiteralCommandNode<S> getBrigadierNode() {
        return brigadierNode;
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
    
    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

	@Override
	public int run(CommandContext<S> context) throws CommandSyntaxException {
        if(handler == null) { throw new NotRegisteredException("CommandHandler has not been registered for " + FULLY_QUALIFIED_NAME); } 
        else if(!enabled)   { throw new CommandDisabledException();                                                   }
        return handler.run(context);
	}

    public void setHandler(Command<S> handler) {
        this.handler = handler;
    }
    
    @Override
    public String toString() {
        return String.format("{FQN: %s, enabled: %b}", FULLY_QUALIFIED_NAME, enabled);
    }

    @Override
	public int compareTo(BrigadierCommand<S> o) {
        return FULLY_QUALIFIED_NAME.compareTo(o.FULLY_QUALIFIED_NAME);
	}

}