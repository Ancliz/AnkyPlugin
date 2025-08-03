package me.ancliz.minecraft.commands;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import com.google.auto.service.AutoService;
import me.ancliz.minecraft.annotations.CommandExecutor;
import me.ancliz.minecraft.annotations.CommandMapping;

@SupportedAnnotationTypes({"me.ancliz.minecraft.annotations.CommandExecutor", "me.ancliz.minecraft.annotations.CommandMapping"})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class CommandExecutorProcessor extends AbstractProcessor {

    @Override
    @SuppressWarnings("unchecked")
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Types typeUtils = processingEnv.getTypeUtils();
        Elements elementUtils = processingEnv.getElementUtils();
        TypeMirror executor = elementUtils.getTypeElement("me.ancliz.minecraft.commands.DefaultCommandExecutor").asType();

        for(Element element : roundEnv.getElementsAnnotatedWith(CommandExecutor.class)) {

            if(element.getKind() == ElementKind.CLASS) {
                TypeElement clazz = (TypeElement) element;
            
                if(!typeUtils.isSubtype(clazz.asType(), executor)) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, 
                    "@CommandExecutor class must be a subtype of me.ancliz.minecraft.commands.DefaultCommandExecutor",
                    clazz);
                } else {
                    CommandExecutor command = element.getAnnotation(CommandExecutor.class);
                    File file = new File("src/main/resources/plugin.yml");

                    try(FileReader reader = new FileReader(file)) {
                        Map<String, Object> data = new Yaml().load(reader);
                        Map<String, Object> commandsSection = (Map<String, Object>) data.getOrDefault("commands", new LinkedHashMap<String, Object>());
                        Map<String, Object> existingCommand = (Map<String, Object>) commandsSection.getOrDefault(command.name(), new LinkedHashMap<String, Object>());
                        
                        existingCommand = populateCommandSection(existingCommand, command.description(),
                            command.usage(),
                            Arrays.asList(command.aliases()),
                            new ArrayList<>(),
                            null
                        );
                        
                        commandsSection.put(command.name(), existingCommand);
                        data.put("commands", commandsSection);
                        writeYaml(file, data);
                    } catch(IOException e) {
                        e.printStackTrace();
                    }

                }
            }
        }

        for(Element element : roundEnv.getElementsAnnotatedWith(CommandMapping.class)) {
            CommandMapping command = element.getAnnotation(CommandMapping.class);
            File file = new File("src/main/resources/plugin.yml");
            
            try(FileReader reader = new FileReader(file)) {
                Map<String, Object> data = new Yaml().load(reader);
                String[] pathParts = command.fullyQualifiedName().split("\\.");
                String parentCommand = pathParts[0];
                Map<String, Object> commandsSection = (Map<String, Object>) data.getOrDefault("commands", new LinkedHashMap<String, Object>());
                Map<String, Object> topLevelCommand = (Map<String, Object>) commandsSection.getOrDefault(parentCommand, new LinkedHashMap<String, Object>());

                // Ensure the top level command exists in the "commands" section
                commandsSection.put(parentCommand, topLevelCommand);

                // Ensure the sub-commands map exists
                Map<String, Object> subCommands = (Map<String, Object>) topLevelCommand.getOrDefault("sub-commands", new LinkedHashMap<String, Object>());
                topLevelCommand.put("sub-commands", subCommands);

                // Add each sub-command by iterating through pathParts (excluding the first part which is the root command)
                Map<String, Object> currentMap = subCommands;

                for(int i = 1; i < pathParts.length - 1; i++) {
                    String subCommand = pathParts[i];
                    Map<String, Object> subCommandSection = (Map<String, Object>) currentMap.getOrDefault(subCommand, new LinkedHashMap<>());
                    Map<String, Object> nestedSubCommands = (Map<String, Object>) subCommandSection.getOrDefault("sub-commands", new LinkedHashMap<>());
                    subCommandSection.put("sub-commands", nestedSubCommands);
                    currentMap.put(subCommand, subCommandSection);
                    currentMap = nestedSubCommands;
                }

                Map<String, Object> leafNode = (Map<String, Object>) currentMap.getOrDefault(
                    pathParts[pathParts.length - 1], new LinkedHashMap<>());

                Map<String, Object> existingSubCommands =
                    (Map<String, Object>) leafNode.getOrDefault("sub-commands", new LinkedHashMap<>());

                LinkedHashMap<String, Object> newLeafNode = populateCommandSection(
                    leafNode,
                    command.description(),
                    command.usage(),
                    Arrays.asList(command.aliases()),
                    Arrays.asList(command.topLevelAliases()),
                    existingSubCommands
                );

                currentMap.put(pathParts[pathParts.length - 1], newLeafNode);

                writeYaml(file, data);
            } catch(IOException e) {
                 e.printStackTrace();
            }
        }

        return true;
    }

    private void writeYaml(File file, Map<String, Object> data) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(options);
        try(FileWriter writer = new FileWriter(file)) {
            yaml.dump(data, writer);
        }
    }

    private LinkedHashMap<String, Object> populateCommandSection(
            Map<String, Object> existingCommand,
            String description,
            String usage,
            List<String> aliases,
            List<String> topLevelAliases,
            Map<String, Object> subCommandsMap) {

        LinkedHashMap<String, Object> ordered = new LinkedHashMap<>();
        

        ordered.put("description", (description != null && !description.isEmpty())
                ? description
                : existingCommand.getOrDefault("description", ""));
        ordered.put("usage", (usage != null && !usage.isEmpty())
                ? usage
                : existingCommand.getOrDefault("usage", ""));
        ordered.put("top-level-aliases", (topLevelAliases != null && !topLevelAliases.isEmpty())
                ? topLevelAliases
                : existingCommand.getOrDefault("top-level-aliases", new ArrayList<>()));
        ordered.put("aliases", (aliases != null && !aliases.isEmpty())
                ? aliases
                : existingCommand.getOrDefault("aliases", new ArrayList<>()));

        for(Map.Entry<String, Object> entry : existingCommand.entrySet()) {
            String key = entry.getKey();
            if(!key.equals("description") &&
                !key.equals("usage") &&
                !key.equals("aliases") &&
                !key.equals("top-level-aliases") &&
                !key.equals("sub-commands")) {
                ordered.put(key, entry.getValue());
            }
        }

        if(subCommandsMap != null && !subCommandsMap.isEmpty()) {
            ordered.put("sub-commands", subCommandsMap);
        } else if(existingCommand.containsKey("sub-commands")) {
            ordered.put("sub-commands", existingCommand.get("sub-commands"));
        }

        return ordered;
    }

}