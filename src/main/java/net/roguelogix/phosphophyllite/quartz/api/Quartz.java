package net.roguelogix.phosphophyllite.quartz.api;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.roguelogix.phosphophyllite.quartz.internal.management.WorldManagement;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3ic;

public class Quartz {
    
    public static void registerTileEntity(IQuartzTile tile) {
        if (!(tile instanceof TileEntity)) {
            // what in the fuck are you doing?
            return;
        }
        if(!((TileEntity) tile).getWorld().isRemote){
            return;
        }
        BlockPos blockPos = ((TileEntity) tile).getPos();
        Vector3ic position = new Vector3i(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        QuartzState quartzState = tile.getState();
        registerState(position, quartzState);
    }
    
    public static void unregisterTileEntity(IQuartzTile tile) {
        if (!(tile instanceof TileEntity)) {
            // what in the fuck are you doing?
            return;
        }
        if(!((TileEntity) tile).getWorld().isRemote){
            return;
        }
        BlockPos blockPos = ((TileEntity) tile).getPos();
        Vector3ic position = new Vector3i(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        registerState(position, null);
    }
    
    public static void registerState(Vector3ic pos, QuartzState state) {
        WorldManagement.registerState(pos, state);
    }
    
    public static void requestBlockUpdate(TileEntity tile) {
        requestBlockUpdate(tile.getPos());
    }
    
    public static void requestBlockUpdate(BlockPos pos) {
        requestBlockUpdate(new Vector3i(pos.getX(), pos.getY(), pos.getZ()));
    }
    
    /**
     * Pushes changes to QuartzState objects at position to renderer
     * may not update entire render chunk
     * @param pos: position to update
     */
    public static void requestBlockUpdate(Vector3ic pos) {
        requestRangeUpdate(pos, pos);
    }
    
    public static void requestChunkUpdate(TileEntity tile) {
        requestChunkUpdate(tile.getPos());
    }
    
    public static void requestChunkUpdate(BlockPos pos) {
        requestChunkUpdate(new Vector3i(pos.getX(), pos.getY(), pos.getZ()));
    }
    
    /**
     * Requests update to entire renderchunk that contains given position
     * matches minecraft renderchunk sizes for update, even if internal representation is different
     * @param pos: block position within renderchunk. NOTE: this is a blockpos, not a chunkpos, values are floored as required automatically
     */
    public static void requestChunkUpdate(Vector3ic pos) {
        Vector3i chunkBasePos = new Vector3i(pos.x() & ~0xF, pos.y() & ~0xF, pos.z() & ~0xF);
        Vector3i chunkEndPos = new Vector3i(chunkBasePos).add(15, 15, 15);
        requestRangeUpdate(chunkBasePos, chunkEndPos);
    }
    
    public static void requestRangeUpdate(TileEntity tileA, TileEntity tileB) {
        requestRangeUpdate(tileA.getPos(), tileB.getPos());
    }
    
    public static void requestRangeUpdate(BlockPos posA, BlockPos posB) {
        requestRangeUpdate(new Vector3i(posA.getX(), posA.getY(), posA.getZ()), new Vector3i(posB.getX(), posB.getY(), posB.getZ()));
    }
    
    
    /**
     * Requests update of range of blocks
     * may or may not update blocks outside of range
     * @param posA one bound of cubic volume
     * @param posB other bound, order agnostic
     */
    public static void requestRangeUpdate(Vector3ic posA, Vector3ic posB) {
        WorldManagement.updateRange(posA.min(posB, new Vector3i()), posA.max(posB, new Vector3i()));
    }
    
    public static void loadTexture(ResourceLocation textureLocation) {
        loadTexture(textureLocation.toString());
    }
    
    
    /**
     * Preloads a texture into the system
     * will give you an error if it cant be found
     * also, loads it ahead of when its needed for rendering
     * may not actually load the texture
     * will not throw on error
     * @param textureLocation: resource string format, eventually turned back into a resource location
     */
    public static void loadTexture(String textureLocation) {
        // TODO: give to internal
    }
    
    public static void cacheStateJSON(ResourceLocation location){
        // TODO: give to internal
    }
    
    public static void cacheStateJSON(String location){
        cacheStateJSON(new ResourceLocation(location));
    }
    
    public static void cacheState(QuartzState state){
        // TODO: give to internal
    }
}
