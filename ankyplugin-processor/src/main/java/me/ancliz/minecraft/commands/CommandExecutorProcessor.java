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
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import com.google.auto.service.AutoService;
import me.ancliz.minecraft.annotations.CommandExecutor;

@SupportedAnnotationTypes("me.ancliz.minecraft.annotations.CommandExecutor")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
@AutoService(Processor.class)
public class CommandExecutorProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Types typeUtils = processingEnv.getTypeUtils();
        Elements elementUtils = processingEnv.getElementUtils();
        TypeMirror executor = elementUtils.getTypeElement("me.ancliz.minecraft.commands.DefaultCommandExecutor").asType();
        for(Element annotatedElement : roundEnv.getElementsAnnotatedWith(CommandExecutor.class)) {

            if(annotatedElement.getKind() == ElementKind.CLASS) {
                TypeElement clazz = (TypeElement) annotatedElement;
            
                if(!typeUtils.isSubtype(clazz.asType(), executor)) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, 
                    "@CommandExecutor class must be a subtype of me.ancliz.minecraft.commands.DefaultCommandExecutor",
                    clazz);
                }
            }
        }
        return true;
    }

}