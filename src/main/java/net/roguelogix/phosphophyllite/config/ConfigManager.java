package net.roguelogix.phosphophyllite.config;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.roguelogix.phosphophyllite.parsers.Element;
import net.roguelogix.phosphophyllite.parsers.JSON5;
import net.roguelogix.phosphophyllite.parsers.TOML;
import net.roguelogix.phosphophyllite.registry.RegisterConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ConfigManager {
    
    static final Logger LOGGER = LogManager.getLogger("Phosphophyllite/Config");
    
    private static final HashSet<ModConfig> modConfigs = new HashSet<>();
    
    public static void registerConfig(Field field, String modName) {
        if (!field.isAnnotationPresent(RegisterConfig.class)) {
            throw new IllegalArgumentException("Field must be annotated with @RegisterConfig");
        }
        if (!Modifier.isStatic(field.getModifiers())) {
            throw new IllegalArgumentException("Base config object must be static");
        }
//        if(Modifier.isFinal(field.getModifiers())){
//            throw new IllegalArgumentException("Base config object cannot be final");
//        }
        var annotation = field.getAnnotation(RegisterConfig.class);
        if (!annotation.name().equals("")) {
            modName = annotation.name();
        }
        
        ModConfig config = new ModConfig(field, modName);
        modConfigs.add(config);
        config.load();
    }
    
    private static class ModConfig {
        private final Object configObject;
        private final Class<?> configClazz;
        final RegisterConfig annotation;
        private final String modName;
        File baseFile;
        File actualFile = null;
        ConfigFormat actualFormat;
        
        private final ConfigSpec spec;
        
        ModConfig(Field field, String name) {
            try {
                configObject = field.get(null);
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException(e);
            }
            configClazz = field.getType();
            modName = name;
            annotation = field.getAnnotation(RegisterConfig.class);
            spec = new ConfigSpec(field, configObject);
            baseFile = new File("config/" + annotation.folder() + "/" + name + "-" + annotation.type().toString().toLowerCase());
            
            loadReflections();
        }
        
        private Field enableAdvanced;
        
        boolean enableAdvanced() {
            if (enableAdvanced == null) {
                return false;
            }
            try {
                return enableAdvanced.getBoolean(configObject);
            } catch (IllegalAccessException ignored) {
                return false;
            }
        }
        
        private final HashSet<Method> preLoads = new HashSet<>();
        private final HashSet<Method> loads = new HashSet<>();
        private final HashSet<Method> postLoads = new HashSet<>();
        
        void loadReflections() {
            for (Field declaredField : configClazz.getDeclaredFields()) {
                if (declaredField.isAnnotationPresent(ConfigValue.class) && declaredField.getAnnotation(ConfigValue.class).enableAdvanced()) {
                    enableAdvanced = declaredField;
                    enableAdvanced.setAccessible(true);
                    Class<?> EAClass = enableAdvanced.getType();
                    if (EAClass != boolean.class && EAClass != Boolean.class) {
                        throw new ConfigSpec.DefinitionError("Advanced enable flag must be a boolean");
                    }
                    break;
                }
            }
            
            for (Method declaredMethod : configClazz.getDeclaredMethods()) {
                if (declaredMethod.getReturnType() != Void.TYPE) {
                    continue;
                }
                if (declaredMethod.getParameterCount() != 0) {
                    continue;
                }
                if (!Modifier.isStatic(declaredMethod.getModifiers())) {
                    continue;
                }
                if (declaredMethod.isAnnotationPresent(RegisterConfig.PreLoad.class)) {
                    declaredMethod.setAccessible(true);
                    preLoads.add(declaredMethod);
                }
                if (declaredMethod.isAnnotationPresent(RegisterConfig.Load.class)) {
                    declaredMethod.setAccessible(true);
                    loads.add(declaredMethod);
                }
                if (declaredMethod.isAnnotationPresent(RegisterConfig.PostLoad.class)) {
                    declaredMethod.setAccessible(true);
                    postLoads.add(declaredMethod);
                }
            }
        }
        
        void runPreLoads() {
            for (Method preLoad : preLoads) {
                try {
                    preLoad.invoke(null);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        
        void runLoads() {
            for (Method load : loads) {
                try {
                    load.invoke(null);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        
        void runPostLoads() {
            for (Method postLoad : postLoads) {
                try {
                    postLoad.invoke(null);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        
        void load() {
            if (actualFile == null) {
                findFile();
            }
            if (!actualFile.exists()) {
                generateFile();
                // if we just generated the file, its default values, no need to do anything else
                return;
            }
            Element tree = readFile();
            if (tree == null) {
                LOGGER.error("No config data for " + modName + " loaded, leaving defaults");
                return;
            }
            spec.writeElementTree(tree);
            tree = spec.trimAndRegenerateTree(tree, enableAdvanced());
            writeFile(tree);
        }
        
        void findFile() {
            File file = null;
            ConfigFormat format = null;
            for (ConfigFormat value : ConfigFormat.values()) {
                File fullFile = new File(baseFile + "." + value.toString().toLowerCase());
                if (fullFile.exists()) {
                    if (file != null) {
                        // why the fuck are there multiple?
                        // if its the correct format, we will use it, otherwise, whatever we have is good enough
                        if (annotation.format() == value) {
                            file = fullFile;
                            format = value;
                        }
                    } else {
                        format = value;
                        file = fullFile;
                    }
                }
            }
            
            if (file == null) {
                file = new File(baseFile + "." + annotation.format().toString().toLowerCase());
                format = annotation.format();
            }
            
            actualFile = file;
            actualFormat = format;
        }
        
        void generateFile() {
            spec.writeDefaults();
            writeFile(spec.trimElementTree(spec.generateElementTree(false)));
        }
        
        void writeFile(@Nullable Element tree) {
            if (tree == null) {
                var path = Paths.get(String.valueOf(actualFile));
                if (Files.exists(path)) {
                    try {
                        Files.delete(path);
                    } catch (IOException ignored) {
                        // i really dont carez
                    }
                }
                return;
            }
            String str = switch (actualFormat) {
                case JSON5 -> JSON5.parseElement(tree);
                case TOML -> TOML.parseElement(tree);
            };
            try {
                //noinspection ResultOfMethodCallIgnored
                actualFile.getParentFile().mkdirs();
                Files.write(Paths.get(String.valueOf(actualFile)), str.getBytes());
            } catch (IOException e) {
                LOGGER.error("Failed to write config file for " + modName);
                e.printStackTrace();
            }
        }
        
        @Nullable
        Element readFile() {
            String str;
            try {
                str = new String(Files.readAllBytes(Paths.get(String.valueOf(actualFile))));
            } catch (IOException e) {
                LOGGER.error("Failed to read config file for " + modName);
                return null;
            }
            
            Element element = switch (actualFormat) {
                case JSON5 -> JSON5.parseString(str);
                case TOML -> TOML.parseString(str);
            };
            if (element == null) {
                LOGGER.error("Failed to parse config for " + modName);
            }
            return element;
        }
    }
}
