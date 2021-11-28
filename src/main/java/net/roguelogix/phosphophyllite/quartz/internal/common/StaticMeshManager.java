package net.roguelogix.phosphophyllite.quartz.internal.common;

import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.roguelogix.phosphophyllite.quartz.Quartz;
import net.roguelogix.phosphophyllite.quartz.QuartzEvent;
import net.roguelogix.phosphophyllite.quartz.QuartzStaticMesh;
import net.roguelogix.phosphophyllite.quartz.internal.QuartzCore;
import net.roguelogix.phosphophyllite.util.MethodsReturnNonnullByDefault;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.function.Consumer;

import static net.roguelogix.phosphophyllite.quartz.internal.common.MagicNumbers.VERTEX_BYTE_SIZE;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class StaticMeshManager implements GLDeletable {
    
    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static class TrackedMesh implements GLDeletable {
        private final StaticMesh mesh;
        private final GLBuffer vertexBuffer;
        private final GLFence fence;
        private GLBuffer.Allocation vertexAllocation;
        private final Object2ObjectArrayMap<RenderType, Component> drawInfo = new Object2ObjectArrayMap<>();
        private final ObjectArrayList<Consumer<TrackedMesh>> buildCallbacks = new ObjectArrayList<>();
        
        public record Component(int vertexOffset, int vertexCount){
        }
        
        private TrackedMesh(StaticMesh mesh, GLBuffer vertexBuffer) {
            this.mesh = mesh;
            this.vertexBuffer = vertexBuffer;
            fence = vertexBuffer.createFence();
        }
        
        void build(Collection<RenderType> renderTypes) {
            fence.fence();
            drawInfo.clear();
            var rawDrawInfo = mesh.build(this::allocBuffer, renderTypes);
            for (var renderTypeEntry : rawDrawInfo.object2LongEntrySet()) {
                var renderType = renderTypeEntry.getKey();
                var drawLong = renderTypeEntry.getLongValue();
                var drawComponent = new Component((int) (drawLong >> 32) + vertexAllocation.offset() / VERTEX_BYTE_SIZE, (int) drawLong);
                drawInfo.put(renderType, drawComponent);
            }
            for (int i = 0; i < buildCallbacks.size(); i++) {
                buildCallbacks.get(i).accept(this);
            }
            vertexAllocation.flush();
        }
        
        private ByteBuffer allocBuffer(int size) {
            if (vertexAllocation != null) {
                vertexAllocation = vertexBuffer.realloc(vertexAllocation, size, VERTEX_BYTE_SIZE);
            } else {
                vertexAllocation = vertexBuffer.alloc(size, VERTEX_BYTE_SIZE);
            }
            fence.clientWait(0);
            return vertexAllocation.buffer();
        }
        
        @Override
        public void delete() {
            vertexAllocation.delete();
        }
    
        public Collection<RenderType> usedRenderTypes() {
            return drawInfo.keySet();
        }
        
        @Nullable
        public Component renderTypeComponent(RenderType renderType) {
            return drawInfo.get(renderType);
        }
        
        public void addBuildCallback(Consumer<TrackedMesh> consumer) {
            buildCallbacks.add(consumer);
        }
        
        public void addBuildCallback(Runnable runnable) {
            addBuildCallback(e -> runnable.run());
        }
    }
    
    private final Object2ObjectOpenHashMap<StaticMesh, TrackedMesh> trackedMeshes = new Object2ObjectOpenHashMap<>();
    public final GLBuffer vertexBuffer;
    
    public StaticMeshManager() {
        vertexBuffer = QuartzCore.instance().allocBuffer(false);
        Quartz.EVENT_BUS.register(this);
    }
    
    @Override
    public void delete() {
        vertexBuffer.delete();
        Quartz.EVENT_BUS.unregister(this);
    }
    
    public StaticMesh createMesh(Consumer<QuartzStaticMesh.Builder> buildFunc) {
        var staticMesh = new StaticMesh(buildFunc, this::meshDeleted);
        var trackedMesh = new TrackedMesh(staticMesh, vertexBuffer);
        trackedMeshes.put(staticMesh, trackedMesh);
        return staticMesh;
    }
    
    private void meshDeleted(StaticMesh mesh) {
        var trackedMesh = trackedMeshes.get(mesh);
        if (trackedMesh != null) {
            trackedMesh.delete();
        }
    }
    
    @Nullable
    public TrackedMesh getMeshInfo(StaticMesh mesh) {
        return trackedMeshes.get(mesh);
    }
    
    public void buildAllMeshes() {
        for (TrackedMesh value : trackedMeshes.values()) {
            buildTrackedMesh(value);
        }
    }
    
    public void buildMesh(StaticMesh mesh) {
        var trackedMesh = trackedMeshes.get(mesh);
        if (trackedMesh == null) {
            return;
        }
        buildTrackedMesh(trackedMesh);
    }
    
    private void buildTrackedMesh(TrackedMesh trackedMesh) {
        trackedMesh.build(QuartzCore.instance().registeredRenderTypes());
    }
    
    @SubscribeEvent
    public void resourceLoadEvent(QuartzEvent.ResourcesLoaded e) {
        buildAllMeshes();
    }
}
