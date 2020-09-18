package net.roguelogix.phosphophyllite.quartz.client.renderer;

import net.roguelogix.phosphophyllite.quartz.client.QuartzClientConfig;
import net.roguelogix.phosphophyllite.quartz.client.gl46cpp.QuartzRendererGL46CPP;
import net.roguelogix.phosphophyllite.quartz.common.QuartzBlockRenderInfo;

import java.util.ArrayList;

public abstract class QuartzRenderer {
    
    public static QuartzRenderer INSTANCE = null;
    
    public static void create(){
        if(INSTANCE != null){
            throw new IllegalStateException();
        }
        //noinspection SwitchStatementWithTooFewBranches
        switch (QuartzClientConfig.OPERATION_MODE){
//            case GL21_JAVA:{
//                INSTANCE = new QuartzRendererGL21Java();
//                break;
//            }
            case GL46_CPP:
                INSTANCE = new QuartzRendererGL46CPP();
                break;
        }
    }
    
    protected QuartzRenderer(){
    }
    
    public abstract void GLSetup();
    
    public abstract void draw();
    
    public abstract void GLShutdown();
    
    public abstract void setBlockRenderInfo(ArrayList<QuartzBlockRenderInfo> info);
    
    public abstract int loadTexture(String textureLocation);
}
