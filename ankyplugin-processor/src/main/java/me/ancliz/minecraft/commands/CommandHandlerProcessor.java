package me.ancliz.minecraft.commands;

import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
import com.google.auto.service.AutoService;
import me.ancliz.minecraft.annotations.CommandMapping;

@SupportedAnnotationTypes("me.ancliz.minecraft.annotations.CommandMapping")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class CommandHandlerProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for(Element element : roundEnv.getElementsAnnotatedWith(CommandMapping.class)) {
            if(element.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) element;

                if(!isValidMethodSignature(method)) {
                    processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "@CommandMapping must have signature: boolean CommandHandler(CommandSender, String[])",
                        method
                    );
                }
            }
        }
        return true;
    }

    private boolean isValidMethodSignature(ExecutableElement method) {
        return method.getReturnType().getKind().equals(TypeKind.BOOLEAN) &&
               method.getParameters().size() == 2 &&
               method.getParameters().get(0).asType().toString().equals("org.bukkit.command.CommandSender") &&
               method.getParameters().get(1).asType().toString().equals("java.lang.String[]");  
    }

}