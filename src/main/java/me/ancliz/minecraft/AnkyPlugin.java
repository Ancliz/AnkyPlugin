package me.ancliz.minecraft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import com.google.common.base.Charsets;
import me.ancliz.util.logging.Logger;

public abstract class AnkyPlugin extends JavaPlugin {
    protected Logger logger;
    protected static AnkyPlugin instance;

    protected void setLogger(AnkyPlugin plugin) {
        logger = new Logger(plugin.getClass());
    }

    @Override
    public void onEnable() {
        setLogger(this);
        logger.debug();
        instance = this;
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

    public static AnkyPlugin getInstance() {
        return instance;
    }

}