package me.ancliz.minecraft.exceptions;

public class CommandDisabledException extends RuntimeException {

    public CommandDisabledException() {
        super("This command has been disabled.");
    }

    public CommandDisabledException(String message) {
        super(message);
    }
    
}