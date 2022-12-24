package net.roguelogix.phosphophyllite.registry;

import net.roguelogix.phosphophyllite.config.ConfigFormat;
import net.roguelogix.phosphophyllite.config.ConfigType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static net.roguelogix.phosphophyllite.config.ConfigFormat.JSON5;
import static net.roguelogix.phosphophyllite.config.ConfigType.COMMON;
import static net.roguelogix.phosphophyllite.config.ConfigType.NULL;

/**
 * This is specific for Phosphophyllite configs
 * All this does is tell the config loader that this is the root of a config, and to start loading from here
 * <p>
 * you can do this manually via ConfigManager.registerConfig
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RegisterConfig {
    String name() default "";
    
    String folder() default "";
    
    String comment() default "";
    
    ConfigFormat format() default JSON5;
    
    ConfigType[] type() default COMMON;
    
    ConfigType rootLevelType() default NULL;
    
    boolean rootLevelReloadable() default false;
    
    // TODO: default to true in 0.7.0
    boolean required() default false;
    
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Registration {
        ConfigType[] type() default {};
    }
    
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface PreLoad {
        ConfigType[] type() default {};
    }
    
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface PostLoad {
        ConfigType[] type() default {};
    }
}
