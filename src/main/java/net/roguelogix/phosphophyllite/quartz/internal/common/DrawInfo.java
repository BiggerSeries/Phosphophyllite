package net.roguelogix.phosphophyllite.quartz.internal.common;

import net.roguelogix.phosphophyllite.quartz.internal.MagicNumbers;
import net.roguelogix.phosphophyllite.quartz.internal.QuartzCore;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix4f;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3f;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class DrawInfo {
    public long deltaNano;
    public float partialTicks;
    
    public final Vector3i playerPosition = new Vector3i();
    public final Vector3f playerSubBlock = new Vector3f();
    public final Matrix4f projectionMatrix = new Matrix4f();
    public final ByteBuffer projectionMatrixByteBuffer = MemoryUtil.memAlloc(MagicNumbers.MATRIX_4F_BYTE_SIZE);
    public final FloatBuffer projectionMatrixFloatBuffer = projectionMatrixByteBuffer.asFloatBuffer();
    
    public float fogStart;
    public float fogEnd;
    public final Vector4f fogColor = new Vector4f();
    
    
    public DrawInfo() {
        ByteBuffer projectionMatrixByteBuffer = this.projectionMatrixByteBuffer;
        QuartzCore.CLEANER.register(this, () -> MemoryUtil.memFree(projectionMatrixByteBuffer));
    }
}
