package me.ancliz.minecraft.commands;

import java.util.List;

public interface CommandMappingsProvider<S> {
    
    public List<CommandSpec<S>> getSpecs();
    
}