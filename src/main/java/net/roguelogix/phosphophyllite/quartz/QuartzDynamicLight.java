package net.roguelogix.phosphophyllite.quartz;

public interface QuartzDynamicLight extends QuartzDisposable {
    
    // [0, 64) range
    void write(int vertex, int vertexDirection, byte skyLight, byte blockLight, byte AO);
    
    default void write(byte skyLight, byte blockLight, byte AO) {
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 6; j++) {
                write(i, j, skyLight, blockLight, AO);
            }
        }
    }
}
