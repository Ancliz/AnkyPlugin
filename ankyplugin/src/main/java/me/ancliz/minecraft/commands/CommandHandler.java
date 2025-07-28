package me.ancliz.minecraft.commands;

import org.bukkit.command.CommandSender;

@FunctionalInterface
public interface CommandHandler {
    
 public boolean invoke(CommandSender sender, String[] args);

}