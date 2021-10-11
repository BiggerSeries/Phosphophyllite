package net.roguelogix.phosphophyllite.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigValue {
    
    String comment() default "";
    
    boolean commentDefaultValue() default true;
    
    // only used for numbers
    String range() default "(,)";
    
    // only used for enums
    String[] allowedValues() default {};
    
    boolean advanced() default false;
    
    boolean hidden() default false;
    
    boolean enableAdvanced() default false;
    
    boolean reloadable() default false;
}

