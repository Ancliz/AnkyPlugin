package me.ancliz.minecraft.metadata;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import org.bukkit.World;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.Metadatable;
import org.bukkit.plugin.java.JavaPlugin;
import me.ancliz.util.logging.Logger;

public class Metadata {
    private static Logger logger = new Logger(Metadata.class);
    private static JavaPlugin plugin;


    public Metadata(JavaPlugin plugin) {
        Metadata.plugin = plugin;
    }

    public static Object getMetadata(JavaPlugin plugin, Metadatable object, String key) {
        return object.getMetadata(key).stream()
            .filter(data -> data.getOwningPlugin().equals(plugin))
            .map(MetadataValue::value)
            .findFirst().orElseThrow(() -> new NoSuchElementException());
    }

    @SuppressWarnings("unchecked")
    public static <T> T getOrDefault(JavaPlugin plugin, Metadatable object, String key, T def) {
        try {
            return (T) getMetadata(plugin, object, key);
        } catch(NoSuchElementException e) {
            logger.warn("No metadata '{}' for plugin '{}', defaulting to '{}'", key, plugin, def);
            return def;
        }
    }

    public static class MapBuilder<T extends MetadataValue> {
        private Map<String, T> map = new HashMap<>();
        private BiFunction<JavaPlugin, Object, T> factory;

        public MapBuilder(BiFunction<JavaPlugin, Object, T> factory) {
            this.factory = factory;
        }

        public MapBuilder<T> put(String key, Object value) {
            map.put(key, factory.apply(plugin, value));
            return this;
        }

        public Map<String, T> build() {
            return map;
        } 
    }

    public static <T extends MetadataValue> MapBuilder<T> mapBuilder(BiFunction<JavaPlugin, Object, T> factory) {
        return new MapBuilder<>(factory);
    }

    public static String getWorldGroup(World world) {
        return Metadata.getOrDefault(plugin, world, "group", "world");
    }

    public static String getWorldBaseName(World world) {
        return Metadata.getOrDefault(plugin, world, "base-name", "world");
    }

}