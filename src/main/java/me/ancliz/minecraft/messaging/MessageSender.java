package me.ancliz.minecraft.messaging;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import me.ancliz.minecraft.AnkyPlugin;
import me.ancliz.minecraft.commands.CommandManager;

public class MessageSender {
    private MMFormatter formatter;

    public MessageSender(CommandManager commandManager) {
        formatter = new MMFormatter(AnkyPlugin.getInstance().getName(), commandManager);
    }

    // public void sendMessage(CommandSender sender, String formattedMessage) {
    //     String finalMessage = (sender instanceof Player) ? formattedMessage : formattedMessage.replaceAll(""+formatter.D, "");
    //     sender.sendMessage(finalMessage);
    // }

    public void sendMessage(CommandSender sender, String message, MMFormat format) {
        String finalMessage = (sender instanceof Player) ? format.apply(message) : message;
        sender.sendMessage(finalMessage);  
    }

    @SuppressWarnings("deprecation")
    public void sendMessage(CommandSender sender, String formattedMessage, ChatColor[] colours) {
        String finalMessage = (sender instanceof Player)
            ? formatter.format(formattedMessage, colours)
            : formattedMessage.replaceAll(""+formatter.D, "");
        sender.sendMessage(finalMessage);
    }

}