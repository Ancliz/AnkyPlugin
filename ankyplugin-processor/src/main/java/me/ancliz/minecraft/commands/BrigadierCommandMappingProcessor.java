package me.ancliz.minecraft.commands;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import com.google.auto.service.AutoService;
import me.ancliz.minecraft.annotations.BrigadierCommandMapping;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@SupportedAnnotationTypes("me.ancliz.minecraft.annotations.BrigadierCommandMapping")
public class BrigadierCommandMappingProcessor extends AbstractProcessor {

    private static final class Mapping {
        final String root;
        final String path;
        final String fqcn;
        final String handler;
        final List<String> argTypes;

        Mapping(String root,
                String path,
                String fqcn,
                String handler,
                List<String> argTypes) {
                    
            this.root = root;
            this.path = path;
            this.fqcn = fqcn;
            this.handler = handler;
            this.argTypes = argTypes;
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> annotated = roundEnv.getElementsAnnotatedWith(BrigadierCommandMapping.class);

        if(annotated.isEmpty()) {
            return false;
        }

        List<Mapping> mappings = new ArrayList<>();

        for(Element element : annotated) {
            if(element.getKind() != ElementKind.METHOD) {
                // TODO
            }

            ExecutableElement handler = (ExecutableElement) element;
            BrigadierCommandMapping anno = handler.getAnnotation(BrigadierCommandMapping.class);

            String path = anno.command();
            int idx = path.indexOf('.');
            String root = (idx == -1) ? path : path.substring(0, idx);

            TypeElement type = (TypeElement) handler.getEnclosingElement();
            String fqcn = type.getQualifiedName().toString();
            String handlerName = handler.getSimpleName().toString();
            List<String> argTypeLiterals = getHandlerArgTypes(handler, BrigadierCommandMapping.class, "args");
            
            mappings.add(new Mapping(root, path, fqcn, handlerName, argTypeLiterals));
        }

        try {
            generateRegistry(mappings);
        } catch(IOException e) {
            throw new RuntimeException(e);
        }

        return false;
    }
    
    @SuppressWarnings("unchecked")
    private List<String> getHandlerArgTypes(ExecutableElement method, Class<?> annoType, String argsElement) {
        for(AnnotationMirror anno : method.getAnnotationMirrors()) {
            if(!((TypeElement) anno.getAnnotationType().asElement())
                    .getQualifiedName().contentEquals(annoType.getName())) {
                continue;
            }

            for(Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : anno.getElementValues().entrySet()) {
                if(entry.getKey().getSimpleName().contentEquals(argsElement)) {
                    List<? extends AnnotationValue> values = (List<? extends AnnotationValue>) entry.getValue().getValue();
                    List<String> list = new ArrayList<>();

                    for(AnnotationValue av : values) {
                        list.add(((String) av.getValue()).toLowerCase());
                    }
                    
                    return list;
                }
            }
        }

        return List.of();
    }

    private void generateRegistry(List<Mapping> mappings) throws IOException {
        String pkg = "me.ancliz.minecraft.ankyplugin.generated";
        String clazz = "GeneratedCommandMappingsProvider";
        String fqcn = pkg + "." + clazz;

        JavaFileObject file = processingEnv.getFiler().createSourceFile(fqcn);
        Map<String, String> enclosingClasses = new HashMap<>();

        try(Writer w = file.openWriter()) {
            w.write("package " + pkg + ";\n\n");
            w.write("import java.util.*;\n");
            w.write("import javax.annotation.processing.Generated;\n");
            w.write("import com.google.auto.service.AutoService;\n");
            w.write("import io.papermc.paper.command.brigadier.CommandSourceStack;\n");
            w.write("import me.ancliz.minecraft.commands.CommandMappingsProvider;\n");
            w.write("import me.ancliz.minecraft.commands.CommandSpec;\n\n");
            
            w.write("@AutoService(CommandMappingsProvider.class)\n");
            w.write("@Generated(\"" + getClass().getCanonicalName() + "\")\n");
            w.write("public final class " + clazz + " implements CommandMappingsProvider<CommandSourceStack> {\n");
            w.write("\tpublic " + clazz + "() {}\n\n");

            w.write("\t@Override\n");
            w.write("\tpublic List<CommandSpec<CommandSourceStack>> getSpecs() {\n");
            w.write("\t\tList<CommandSpec<CommandSourceStack>> list = new ArrayList<>();\n");

            for(Mapping m : mappings) {
                String enclosingClassVar = m.fqcn.substring(m.fqcn.lastIndexOf(".")+1);
                enclosingClassVar = enclosingClassVar.substring(0, 1).toLowerCase() + enclosingClassVar.substring(1);

                if(!enclosingClasses.containsKey(m.fqcn)) {
                    w.write("\t\t" + m.fqcn + " " + enclosingClassVar + " = new " + m.fqcn + "();\n");
                    enclosingClasses.put(m.fqcn, enclosingClassVar);
                } else {
                    enclosingClassVar = enclosingClasses.get(m.fqcn);
                }

                w.write("\t\tlist.add(new CommandSpec<CommandSourceStack>(\"" + m.root + "\", \"" + m.path + "\", new String[] {");
                
                for(int i = 0; i < m.argTypes.size(); ++i) {
                    if(i > 0) w.write(", ");
                    w.write("\"" + m.argTypes.get(i) + "\"");
                }

                w.write("}, ");
                w.write(enclosingClassVar + "::" + m.handler + "));\n");
            }

            w.write("\t\treturn java.util.Collections.unmodifiableList(list);\n");
            w.write("\t}\n\n");
            w.write("}");
        }
    }

}