package net.roguelogix.phosphophyllite.quartz.client.renderer;

public interface IRenderChunkSection {
    
    class BlockRenderInfo{
    
    }
    
    void init();
    
    void shutdown();
    
    void setBlock();
    
    void draw();
}
