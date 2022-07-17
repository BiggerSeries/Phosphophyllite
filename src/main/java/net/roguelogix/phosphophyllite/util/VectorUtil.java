package net.roguelogix.phosphophyllite.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3ic;

@NonnullDefault
public class VectorUtil {
    
    public static boolean less(Vector3ic left, Vector3ic right) {
        return left.x() < right.x() && left.y() < right.y() && left.z() < right.z();
    }
    
    public static boolean less(Vector3ic left, BlockPos right) {
        return left.x() < right.getX() && left.y() < right.getY() && left.z() < right.getZ();
    }
    
    public static boolean less(BlockPos left, Vector3ic right) {
        return greater(right, left);
    }
    
    public static boolean greater(Vector3ic left, Vector3ic right) {
        return left.x() > right.x() && left.y() > right.y() && left.z() > right.z();
    }
    
    public static boolean greater(Vector3ic left, BlockPos right) {
        return left.x() > right.getX() && left.y() > right.getY() && left.z() > right.getZ();
    }
    
    public static boolean greater(BlockPos left, Vector3ic right) {
        return less(right, left);
    }
    
    public static boolean lequal(Vector3ic left, Vector3ic right) {
        return left.x() <= right.x() && left.y() <= right.y() && left.z() <= right.z();
    }
    
    public static boolean lequal(Vector3ic left, BlockPos right) {
        return left.x() <= right.getX() && left.y() <= right.getY() && left.z() <= right.getZ();
    }
    
    public static boolean grequal(Vector3ic left, Vector3ic right) {
        return left.x() >= right.x() && left.y() >= right.y() && left.z() >= right.z();
    }
    
    public static boolean grequal(Vector3ic left, BlockPos right) {
        return left.x() >= right.getX() && left.y() >= right.getY() && left.z() >= right.getZ();
    }
    
    public static long blockPosToChunkPosLon(Vector3ic blockpos) {
        return ChunkPos.asLong(SectionPos.blockToSectionCoord(blockpos.x()), SectionPos.blockToSectionCoord(blockpos.z()));
    }
}
