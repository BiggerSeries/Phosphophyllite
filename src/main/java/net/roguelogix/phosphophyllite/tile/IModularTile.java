package net.roguelogix.phosphophyllite.tile;

public interface IModularTile {
    ITileModule getModule(Class<?> interfaceClazz);
}
