package net.roguelogix.phosphophyllite.quartz;

import net.roguelogix.phosphophyllite.repack.org.joml.Matrix4fc;

public interface QuartzDynamicMatrix extends QuartzDisposable {
    void write(Matrix4fc matrixData);
}
