package net.roguelogix.phosphophyllite.quartz;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraftforge.eventbus.EventBus;
import net.minecraftforge.eventbus.api.BusBuilder;
import net.roguelogix.phosphophyllite.quartz.internal.QuartzCore;
import net.roguelogix.phosphophyllite.repack.org.joml.AABBi;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3ic;
import net.roguelogix.phosphophyllite.util.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class Quartz {
    
    public static EventBus EVENT_BUS = new EventBus(new BusBuilder().setTrackPhases(false));
    
    public static StaticMesh createStaticMesh(Consumer<StaticMesh.Builder> buildFunc) {
        return QuartzCore.INSTANCE.meshManager.createMesh(buildFunc);
    }
    
    public static DrawBatch getDrawBatchForBlock(BlockPos blockPos) {
        return getDrawBatcherForSection(SectionPos.asLong(blockPos));
    }
    
    public static DrawBatch getDrawBatcherForBlock(Vector3ic blockPos) {
        return getDrawBatcherForSection(SectionPos.asLong(blockPos.x() >> 4, blockPos.y() >> 4, blockPos.z() >> 4));
    }
    
    public static DrawBatch getDrawBatcherForSection(long sectionPos) {
        return QuartzCore.INSTANCE.getWorldEngine().getBatcherForSection(sectionPos);
    }
    
    public static DrawBatch getDrawBatcherForAABB(AABBi aabb) {
        return null;
    }
}
