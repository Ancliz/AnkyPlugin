package me.ancliz.minecraft;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.BiConsumer;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import com.google.common.base.Charsets;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType.StringType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import me.ancliz.minecraft.annotations.CommandExecutor;
import me.ancliz.minecraft.annotations.TabCompleter;
import me.ancliz.minecraft.commands.BrigadierCommand;
import me.ancliz.minecraft.commands.CommandManager;
import me.ancliz.minecraft.commands.CommandMappingsProvider;
import me.ancliz.minecraft.commands.CommandSpec;
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
        return embedded
        ? YamlConfiguration.loadConfiguration(new InputStreamReader(getEmbeddedResource(file), Charsets.UTF_8))
        : YamlConfiguration.loadConfiguration(new InputStreamReader(getResource(file), Charsets.UTF_8));
    }

    public void saveYaml(YamlConfiguration yaml, String file) {
        try {
            yaml.save(new File(getDataFolder(), file));
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private void setupCommands() {
        try {
            setExecutors();
            setTabCompleters();
        } catch(Exception e) {
            logger.error("Error loading commands: {}", e, e.getCause() != null ? e.getCause().getMessage() : "");
        }
    }
    
    private void setExecutors() throws Exception {
        Set<String> present = registerCommandBinding(
            CommandExecutor.class,
            org.bukkit.command.CommandExecutor.class,
            PluginCommand::setExecutor
        );

        setToDefault(DefaultCommandExecutor.class, PluginCommand::setExecutor, present, "CommandExecutor");
    }

    private void setTabCompleters() throws Exception {
        Set<String> present = registerCommandBinding(
            TabCompleter.class,
            org.bukkit.command.TabCompleter.class,
            PluginCommand::setTabCompleter 
        );
        
        setToDefault(DefaultTabCompleter.class, PluginCommand::setTabCompleter, present, "TabCompleter");
    }

    @SuppressWarnings("deprecation")
    private <T> void setToDefault(Class<? extends T> defaultClass, BiConsumer<PluginCommand, T> binder, 
            Set<String> registeredCommands, String warningLabel) throws Exception {

        Set<String> allCommands = new HashSet<>(getDescription().getCommands().keySet());
        allCommands.removeAll(registeredCommands);

        for(String command : allCommands) {
            logger.warn("No {} found for command '{}', setting to default.", warningLabel, command);
            T defaultInstance = defaultClass.getDeclaredConstructor(CommandManager.class).newInstance(commandManager);
            binder.accept(getCommand(command), defaultInstance);
        }
    }

    private <T> Set<String> registerCommandBinding(Class<? extends Annotation> annotationClass, Class<T> targetType,
        BiConsumer<PluginCommand, T> binder) throws Exception {

        Set<String> present = new HashSet<>();
        try(ScanResult result = new ClassGraph().enableAllInfo().enableAnnotationInfo().scan()) {
            ClassInfoList filtered = result.getClassesWithAnnotation(annotationClass);

            for(ClassInfo classInfo : filtered) {
                AnnotationInfo anno = classInfo.getAnnotationInfo(annotationClass);
                String value = (annotationClass.equals(CommandExecutor.class)) ? "name" : "fullyQualifiedName";
                String command = (String) anno.getParameterValues().getValue(value);
                logger.debug("value: {}, command: {}", value, command);
                Class<? extends T> clazz = classInfo.loadClass(targetType);
                PluginCommand pluginCommand = getCommand(command);
                logger.trace("Setting {} for {}: {}", targetType.getSimpleName(), command, clazz.getSimpleName());

                if(pluginCommand != null) {
                    T instance = clazz.getDeclaredConstructor(CommandManager.class).newInstance(commandManager);
                    binder.accept(pluginCommand, instance);
                    present.add(pluginCommand.getName());
                } else {
                    logger.warn("{} '{}' attempts to register for command '{}' but it does not exist - is it in plugin.yml?",
                        targetType.getSimpleName(), clazz.getSimpleName(), command);
                }
            }
        }

        return present;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<CommandSpec<CommandSourceStack>> loadSpecsFromProviders() {
        List<CommandSpec<CommandSourceStack>> all = new ArrayList<>();
        ServiceLoader<CommandMappingsProvider> loader =
            ServiceLoader.load(CommandMappingsProvider.class, this.getClassLoader());

        logger.info("Loading commands from providers...");

        for(CommandMappingsProvider provider : loader) {
            CommandMappingsProvider<CommandSourceStack> cmp = (CommandMappingsProvider<CommandSourceStack>) provider;
            all.addAll(cmp.getSpecs());
        }
        
        return all;
    }
    
    public static AnkyPlugin getInstance() {
        return instance;
    }

    abstract public void reload();

}