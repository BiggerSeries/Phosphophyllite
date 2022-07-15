package net.roguelogix.phosphophyllite.config;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.roguelogix.phosphophyllite.parsers.Element;
import net.roguelogix.phosphophyllite.parsers.JSON5;
import net.roguelogix.phosphophyllite.parsers.ROBN;
import net.roguelogix.phosphophyllite.parsers.TOML;
import net.roguelogix.phosphophyllite.registry.OnModLoad;
import net.roguelogix.phosphophyllite.registry.RegisterConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import static net.roguelogix.phosphophyllite.Phosphophyllite.modid;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ConfigManager {
    
    static final Logger LOGGER = LogManager.getLogger("Phosphophyllite/Config");
    
    private static final Object2ObjectOpenHashMap<String, ModConfig> modConfigs = new Object2ObjectOpenHashMap<>();
    
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
        
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            if (annotation.type() == ConfigType.CLIENT) {
                // no need to load client configs on the server
                return;
            }
        }
        
        ModConfig config = new ModConfig(field, modName);
        modConfigs.put(config.baseFile.toString(), config);
        config.load();
    }
    
    void reloadConfigs() {
        for (ModConfig value : modConfigs.values()) {
            value.reload();
        }
        for (ServerPlayer player : players) {
            sendConfigToPlayer(player, false);
        }
    }
    
    @OnModLoad
    private static void onModLoad() {
        INSTANCE.registerMessage(1, ByteArrayPacketMessage.class, ByteArrayPacketMessage::encodePacket, ByteArrayPacketMessage::decodePacket, ConfigManager::packetHandler);
        MinecraftForge.EVENT_BUS.addListener(ConfigManager::onPlayerLogin);
        MinecraftForge.EVENT_BUS.addListener(ConfigManager::onPlayerLogout);
    }
    
    private static final ObjectArrayList<ServerPlayer> players = new ObjectArrayList<>();
    
    private static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent e) {
        var server = e.getEntity().getServer();
        assert server != null;
        if (!server.isDedicatedServer()) {
            var serverUUID = e.getEntity().getUUID();
            var localUUID = Minecraft.getInstance().getUser().getUuid();
            if (serverUUID.toString().equals(localUUID)) {
                // ignore local player on integrated server
                // do have the configs reload the saved tree though
                for (ModConfig value : modConfigs.values()) {
                    value.reloadSavedTree();
                }
                return;
            }
        }
        var player = (ServerPlayer) e.getEntity();
        players.add(player);
        sendConfigToPlayer(player, true);
    }
    
    private static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        var player = (ServerPlayer) e.getEntity();
        players.remove(player);
    }
    
    private static void sendConfigToPlayer(ServerPlayer player, boolean initialLogin) {
        final var configs = new Object2ObjectOpenHashMap<String, ArrayList<Byte>>();
        for (ModConfig modConfig : modConfigs.values()) {
            if (modConfig.type == ConfigType.COMMON) {
                var configTree = modConfig.spec.generateElementTree(true, true);
                var configROBN = ROBN.parseElement(configTree);
                if (configROBN != null) {
                    configs.put(modConfig.baseFile.toString(), configROBN);
                }
            }
        }
        var robn = net.roguelogix.phosphophyllite.robn.ROBN.toROBN(new Pair(initialLogin, configs));
        var bytes = new byte[robn.size()];
        for (int i = 0; i < robn.size(); i++) {
            bytes[i] = robn.get(i);
        }
        INSTANCE.sendTo(new ByteArrayPacketMessage(bytes), player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }
    
    private static void packetHandler(@Nonnull ByteArrayPacketMessage packet, @Nonnull Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
            ctx.get().setPacketHandled(true);
            return;
        }
        ctx.get().enqueueWork(() -> {
            try {
                ArrayList<Byte> buf = new ArrayList<>();
                for (byte aByte : packet.bytes) {
                    buf.add(aByte);
                }
                //noinspection unchecked
                final var pair = (Pair<Boolean, Map<String, ArrayList<Byte>>>) net.roguelogix.phosphophyllite.robn.ROBN.fromROBN(buf);
                boolean initialLogin = pair.getFirst();
                final var configs = pair.getSecond();
                for (final var entry : configs.entrySet()) {
                    final var configName = entry.getKey();
                    final var config = modConfigs.get(configName);
                    if (config == null) {
                        return;
                    }
                    try {
                        final var elementTree = ROBN.parseROBN(entry.getValue());
                        config.loadServerTree(elementTree, initialLogin);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            } catch (ClassCastException ignored) {
            }
        });
        ctx.get().setPacketHandled(true);
    }
    
    private static final String PROTOCOL_VERSION = "0";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(modid, "phosphophyllite/configsync"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    
    private static class ByteArrayPacketMessage {
        public byte[] bytes;
        
        public ByteArrayPacketMessage() {
        
        }
        
        public ByteArrayPacketMessage(@Nonnull byte[] readByteArray) {
            bytes = readByteArray;
        }
        
        private static void encodePacket(@Nonnull ByteArrayPacketMessage packet, @Nonnull FriendlyByteBuf buf) {
            buf.writeBytes(packet.bytes);
        }
        
        private static ByteArrayPacketMessage decodePacket(@Nonnull FriendlyByteBuf buf) {
            byte[] byteBuf = new byte[buf.readableBytes()];
            buf.readBytes(byteBuf);
            return new ByteArrayPacketMessage(byteBuf);
        }
    }
    
    private static class ModConfig {
        private final Object configObject;
        private final Class<?> configClazz;
        final RegisterConfig annotation;
        final ConfigType type;
        private final String modName;
        File baseFile;
        File actualFile = null;
        ConfigFormat actualFormat;
        Element savedTree;
        
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
            type = annotation.type();
            spec = new ConfigSpec(field, configObject);
            baseFile = new File("config/" + annotation.folder() + "/" + name + "-" + annotation.type().toString().toLowerCase(Locale.US));
            
            loadReflections();
            runRegistrations();
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
        
        private final HashSet<Method> registrations = new HashSet<>();
        private final HashSet<Method> preLoads = new HashSet<>();
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
                if (declaredMethod.isAnnotationPresent(RegisterConfig.Registration.class)) {
                    declaredMethod.setAccessible(true);
                    registrations.add(declaredMethod);
                }
                if (declaredMethod.isAnnotationPresent(RegisterConfig.PreLoad.class)) {
                    declaredMethod.setAccessible(true);
                    preLoads.add(declaredMethod);
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
        
        void runRegistrations() {
            for (Method load : registrations) {
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
        
        public void reload() {
            load(true);
        }
        
        void load() {
            load(false);
        }
        
        private void load(boolean isReload) {
            if (actualFile == null) {
                findFile();
            }
            if (!actualFile.exists()) {
                generateFile();
                // if we just generated the file, its default values, no need to do anything else
                runPreLoads();
                runPostLoads();
                return;
            }
            Element tree = readFile();
            if (tree == null) {
                LOGGER.error("No config data for " + modName + " loaded, leaving defaults");
                runPreLoads();
                runPostLoads();
                return;
            }
            loadElementTree(tree, isReload);
            tree = spec.trimAndRegenerateTree(tree, enableAdvanced());
            writeFile(tree);
        }
        
        void findFile() {
            File file = null;
            ConfigFormat format = null;
            for (ConfigFormat value : ConfigFormat.values()) {
                File fullFile = new File(baseFile + "." + value.toString().toLowerCase(Locale.US));
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
                file = new File(baseFile + "." + annotation.format().toString().toLowerCase(Locale.US));
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
        
        public void reloadSavedTree() {
            if (savedTree == null) {
                return;
            }
            loadElementTree(savedTree, true);
            savedTree = null;
        }
        
        public void loadServerTree(@Nullable Element tree, boolean initialLogin) {
            if (tree == null) {
                return;
            }
            savedTree = spec.generateElementTree(true, true);
            loadElementTree(tree, !initialLogin);
        }
        
        public void loadElementTree(Element tree, boolean isReload) {
            runPreLoads();
            spec.writeElementTree(tree, isReload);
            runPostLoads();
        }
    }
}
