package net.roguelogix.phosphophyllite.quartz.internal.rendering;

import net.roguelogix.phosphophyllite.quartz.QuartzConfig;
import net.roguelogix.phosphophyllite.quartz.internal.BlockRenderInfo;
import net.roguelogix.phosphophyllite.quartz.internal.rendering.gl46cpp.RendererGL46CPP;
import org.apache.commons.lang3.SystemUtils;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class Renderer {
    
    public static Renderer INSTANCE = null;
    
    public static void create() {
        if (INSTANCE != null) {
            throw new IllegalStateException();
        }
        QuartzConfig.OperationMode mode = QuartzConfig.OPERATION_MODE;
        if (mode == QuartzConfig.OperationMode.ANY) {
            // fantastic, now i get to decide
            // its gotta be at least GL2.1, so, just gonna set that by default
            mode = QuartzConfig.OperationMode.GL21_JAVA;
            if (!SystemUtils.IS_OS_MAC && System.getProperty("sun.arch.data.model").equals("64")) {
                // aight, we aren't on a mac, and it 64bit, do we have the GL capabilities for higher?
                GLCapabilities capabilties = GL.getCapabilities();
                if (capabilties.OpenGL33) {
                    // this uses core GL33
                    mode = QuartzConfig.OperationMode.GL33_CPP;
                    
                    if (
                            capabilties.OpenGL46
                                    && capabilties.GL_ARB_bindless_texture
//                                    && capabilties.GL_ARB_sparse_buffer
                    ) {
                        
                        // oh good, GL4.6
                        // this is the fastest one
                        mode = QuartzConfig.OperationMode.GL46_CPP;
                    }
                }
            }
            if (QuartzConfig.MAX_OPERATION_MODE != QuartzConfig.OperationMode.ANY) {
                switch (QuartzConfig.MAX_OPERATION_MODE) {
                    case GL21_JAVA: {
                        mode = QuartzConfig.OperationMode.GL21_JAVA;
                    }
                    case GL33_CPP:
                        if (mode == QuartzConfig.OperationMode.GL46_CPP) {
                            mode = QuartzConfig.OperationMode.GL33_CPP;
                        }
                        break;
                    case GL46_CPP:
                        // effectively no limit here
                        break;
                }
            }
        }
        
        //noinspection SwitchStatementWithTooFewBranches
        switch (QuartzConfig.OPERATION_MODE) {
//            case GL21_JAVA:{
//                INSTANCE = new QuartzRendererGL21Java();
//                break;
//            }
            case GL46_CPP:
                INSTANCE = new RendererGL46CPP();
                break;
        }
        if (INSTANCE == null) {
            throw new IllegalStateException("Quartz unable to initialize, invalid operation mode chosen");
        }
        
        INSTANCE.GLSetup();
    }
    
    protected Renderer() {
    }
    
    public abstract void GLSetup();
    
    public abstract void draw();
    
    public abstract void GLShutdown();
    
    public abstract void setBlockRenderInfo(ArrayList<BlockRenderInfo> info);
    
    public abstract Map<String, Integer> loadTextures(List<String> textures);
}
