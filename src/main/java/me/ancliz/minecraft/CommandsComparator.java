package me.ancliz.minecraft;

import java.util.Comparator;
import me.ancliz.minecraft.commands.Command;

public class CommandsComparator implements Comparator<Command> {

    @Override
    public int compare(Command o1, Command o2) {
        return o1.FULLY_QUALIFIED_NAME.compareTo(o2.FULLY_QUALIFIED_NAME);
    }
    
}