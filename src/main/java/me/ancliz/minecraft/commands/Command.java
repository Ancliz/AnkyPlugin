package me.ancliz.minecraft.commands;

import java.util.ArrayList;
import java.util.List;

public class Command {
    private final String name;
    private List<String> aliases = new ArrayList<>();
    private String description = "";
    private String usage = "";

    public Command(String name, List<String> aliases, String description, String usage) {
       this.name = name;
       this.aliases = aliases;
       this. description =  description;
       this.usage = usage;
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
    
}