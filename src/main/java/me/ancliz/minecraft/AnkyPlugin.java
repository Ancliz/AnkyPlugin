package me.ancliz.minecraft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import com.google.common.base.Charsets;
import me.ancliz.minecraft.commands.CommandManager;
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
        logger.debug();
        try {
            return new FileInputStream(new File(getDataFolder(), file));
        } catch(FileNotFoundException e) {}
        logger.info("Getting embedded resource '{}'", file);
        return super.getResource(file);
    }

    public InputStream getEmbeddedResource(String file) {
        return super.getResource(file);
    }

    public YamlConfiguration getYaml(String file, boolean embedded) {
        Reader reader;
        if(embedded) { 
            reader = new InputStreamReader(getEmbeddedResource(file), Charsets.UTF_8);
        } else { 
            logger.debug();
            reader = new InputStreamReader(getResource(file), Charsets.UTF_8);
        }
        return YamlConfiguration.loadConfiguration(reader);
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
        commands.forEach(this::setExecutor);
    }

    @SuppressWarnings("unchecked")
    private void setTabCompleter(String command) throws Exception {
        Class<? extends TabCompleter> clazz = null;
        try {
            clazz = (Class<? extends TabCompleter>) Class.forName(this.getClass().getPackageName() + ".commands.completers.TabCompleter" + command);
            getCommand(command).setTabCompleter(clazz.getDeclaredConstructor(CommandManager.class).newInstance(commandManager));
        } catch(ClassNotFoundException e) {
            logger.warn("No tab completion for " + command + ". Setting to default.");
            clazz = DefaultTabCompleter.class;
        } finally {
            getCommand(command).setTabCompleter(clazz.getDeclaredConstructor(CommandManager.class).newInstance(commandManager));

        }
    }

    @SuppressWarnings("unchecked")
    private void setExecutor(String command, Map<String, Object> tree) {
        command = command.substring(0, 1).toUpperCase() + command.substring(1);
        try {
            Class<? extends CommandExecutor> clazz = (Class<? extends CommandExecutor>) Class.forName(this.getClass().getPackageName() + ".commands.Command" + command);
            getCommand(command).setExecutor(clazz.getDeclaredConstructor(CommandManager.class).newInstance(commandManager));
            setTabCompleter(command);
        } catch(ClassNotFoundException e) {
            logger.error("Command Executor class could not be found for " + command);
        } catch(Exception e) {
            logger.error("Error loading commands.", e);
        }
    }

    public static AnkyPlugin getInstance() {
        return instance;
    }

}