package net.roguelogix.phosphophyllite.tile;

public interface IModularTile {
    ITileModule getModule(Class<?> interfaceClazz);
    
    default  <T> T getModule(Class<?> interfaceClazz, Class<T> moduleType) {
        //noinspection unchecked
        return (T) getModule(interfaceClazz);
    }
}
