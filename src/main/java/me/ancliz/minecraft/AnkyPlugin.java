package me.ancliz.minecraft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import com.google.common.base.Charsets;
import me.ancliz.minecraft.commands.CommandManager;
import me.ancliz.minecraft.commands.DefaultCommandExecutor;
import me.ancliz.minecraft.commands.DefaultTabCompleter;
import me.ancliz.util.logging.Logger;

public abstract class AnkyPlugin extends JavaPlugin {
    protected Logger logger;
    protected static AnkyPlugin instance;
    protected CommandManager commandManager;

    protected void setLogger(AnkyPlugin plugin) {
        logger = new Logger(plugin.getClass());
    }

    @Override
    public void onEnable() {
        setLogger(this);
        instance = this;
        commandManager = new CommandManager();
        setupCommands();
    }
    
    @Override
    public InputStream getResource(String file) {
        try {
            return new FileInputStream(new File(getDataFolder(), file));
        } catch(FileNotFoundException e) {}
        logger.info("File not found on disk, getting embedded resource '{}'", file);
        return super.getResource(file);
    }

    public InputStream getEmbeddedResource(String file) {
        return super.getResource(file);
    }

    public YamlConfiguration getYaml(String file, boolean embedded) {
        if(embedded) { 
            return YamlConfiguration.loadConfiguration(new InputStreamReader(getEmbeddedResource(file), Charsets.UTF_8));
        }
        return YamlConfiguration.loadConfiguration(new InputStreamReader(getResource(file), Charsets.UTF_8));
    }

    public void saveYaml(YamlConfiguration yaml, String file) {
        try {
            yaml.save(new File(getDataFolder(), file));
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    private void setupCommands() {
        Map<String, Map<String, Object>> commands = getDescription().getCommands();
        commands.forEach((command, tree) -> {
            try {
                setExecutor(command, tree);
            } catch(Exception e) {
                logger.error("Error loading commands.", e);
            }
        });
    }

    @SuppressWarnings({"unchecked", "null"})
    private void setTabCompleter(String command) throws Exception {
        Class<? extends TabCompleter> clazz = null;
        logger.debug("command: {}", command);
        try {
            clazz = (Class<? extends TabCompleter>) Class.forName(this.getClass().getPackageName() + ".commands.completers.TabCompleter" + command);
            getCommand(command).setTabCompleter(clazz.getDeclaredConstructor(CommandManager.class, String.class).newInstance(commandManager, command.toLowerCase()));
        } catch(ClassNotFoundException e) {
            logger.warn("No tab completion for '" + command + "'. Setting to default.");
            clazz = DefaultTabCompleter.class;
        } finally {
            getCommand(command).setTabCompleter(clazz.getDeclaredConstructor(CommandManager.class, String.class).newInstance(commandManager, command.toLowerCase()));

        }
    }

    @SuppressWarnings({"unchecked", "null"})
    private void setExecutor(String command, Map<String, Object> tree) throws Exception {
        Class<? extends CommandExecutor> clazz = null;
        command = command.substring(0, 1).toUpperCase() + command.substring(1);
        try {
            clazz = (Class<? extends CommandExecutor>) Class.forName(this.getClass().getPackageName() + ".commands.Command" + command);
            getCommand(command).setExecutor(clazz.getDeclaredConstructor(CommandManager.class).newInstance(commandManager));
            setTabCompleter(command);
        } catch(ClassNotFoundException e) {
            logger.error("Command Executor class could not be found for '" + command + "'. Setting to default. This will be breaking.");
            clazz = DefaultCommandExecutor.class;
        } finally {
            getCommand(command).setExecutor(clazz.getDeclaredConstructor(CommandManager.class).newInstance(commandManager));
        }
    }

    public static AnkyPlugin getInstance() {
        return instance;
    }

    abstract public void reload();
}