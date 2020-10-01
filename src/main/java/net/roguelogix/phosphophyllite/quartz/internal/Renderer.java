package net.roguelogix.phosphophyllite.quartz.internal;

import net.roguelogix.phosphophyllite.quartz.QuartzConfig;
import net.roguelogix.phosphophyllite.quartz.internal.gl46cpp.RendererGL46CPP;

import java.util.ArrayList;

public abstract class Renderer {
    
    public static Renderer INSTANCE = null;
    
    public static void create(){
        if(INSTANCE != null){
            throw new IllegalStateException();
        }
        //noinspection SwitchStatementWithTooFewBranches
        switch (QuartzConfig.OPERATION_MODE){
//            case GL21_JAVA:{
//                INSTANCE = new QuartzRendererGL21Java();
//                break;
//            }
            case GL46_CPP:
                INSTANCE = new RendererGL46CPP();
                break;
        }
    }
    
    protected Renderer(){
    }
    
    public abstract void GLSetup();
    
    public abstract void draw();
    
    public abstract void GLShutdown();
    
    public abstract void setBlockRenderInfo(ArrayList<BlockRenderInfo> info);
    
    public abstract int loadTexture(String textureLocation);
}
