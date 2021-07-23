package net.roguelogix.phosphophyllite.tile;

import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ModuleRegistry {
    private static final LinkedHashMap<Class<?>, Function<BlockEntity, ITileModule>> moduleRegistry = new LinkedHashMap<>();
    private static final ArrayList<BiConsumer<Class<?>, Function<BlockEntity, ITileModule>>> externalRegistrars = new ArrayList<>();
    
    /**
     * Registers an ITileModule and the interface the tile class will implement to signal to create an instance at tile creation
     * <p>
     * Also passes this value to any external registries registered below
     *
     * @param moduleInterface: Interface class that will be implemented by the tile.
     * @param constructor:     Creates an instance of an ITileModule for the given tile with the interface implemented
     */
    public synchronized static void registerModule(Class<?> moduleInterface, Function<BlockEntity, ITileModule> constructor) {
        moduleRegistry.put(moduleInterface, constructor);
        externalRegistrars.forEach(c -> c.accept(moduleInterface, constructor));
    }
    
    /**
     * allows for external implementations of the ITileModule system, so extensions from PhosphophylliteTile and PhosphophylliteBlock aren't forced
     *
     * @param registrar external ITileModuleRegistration function
     */
    public synchronized static void registerExternalRegistrar(BiConsumer<Class<?>, Function<BlockEntity, ITileModule>> registrar) {
        externalRegistrars.add(registrar);
        moduleRegistry.forEach(registrar);
    }
    
    public static void forEach(BiConsumer<Class<?>, Function<BlockEntity, ITileModule>> callback){
        moduleRegistry.forEach(callback);
    }
}
