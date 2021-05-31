package net.roguelogix.phosphophyllite.quartz;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class QuartzTESR<T extends TileEntity> {
    
    public static abstract class Renderer {
        public abstract void build(QuartzMesh.Builder builder);
        
        public abstract void updateMatrices(float partialTicks);
    }
    
    
    public interface IQuartzTile {
        /**
         * Get info needed for rendering on the client side
         * Will be passed back to you client side in setRenderInfo
         * The same packet may be sent to multiple clients
         *
         * This is not updated per frame, and should contain only general information needed.
         *
         * As this is a byte array, it is recommended to use ROBN to pack your data
         *
         * @Side: server only
         *
         * @return byte array representing required render info, or null to not send any info
         */
        @Nullable
        ArrayList<Byte> getRenderInfo();
    
        /**
         * Receive render info from server
         *
         * @Side: client only
         *
         * @param info: byte array given  by the server tile for rendering
         */
        void setRenderInfo(ArrayList<Byte> info);
    
        /**
         * Hints to quartz to render or not render this tile
         * This check is not intended for AABB/frustum checking
         * In the event that a TESR may be active/inactive at times, use this to signal to disable it
         *
         * Used on the client to hint to skip rendering
         * Used on the server to hint to not update data
         *
         * @Side: Both
         *
         * @return if this tile should be rendered
         */
        boolean shouldRender();
    
        /**
         * Create a renderer instance capable of rendering all states of this tile
         *
         * @Side: client only
         *
         * @return renderer instance
         */
        Renderer createRenderer();
    }
    
}
