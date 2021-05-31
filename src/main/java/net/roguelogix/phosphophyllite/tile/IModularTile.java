package net.roguelogix.phosphophyllite.tile;

public interface IModularTile {
    PhosphophylliteTile.Module getModule(Class<?> interfaceClazz);
}
