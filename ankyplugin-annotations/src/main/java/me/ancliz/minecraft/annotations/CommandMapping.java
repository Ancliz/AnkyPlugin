package me.ancliz.minecraft.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandMapping {

    String fullyQualifiedName();
    String description() default "";
    String usage() default "";
    String[] aliases() default {};
    String[] topLevelAliases() default {};
    
}