package net.roguelogix.phosphophyllite.quartz;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.roguelogix.phosphophyllite.modular.api.IModularTile;
import net.roguelogix.phosphophyllite.modular.api.TileModule;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix4fc;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;
import net.roguelogix.phosphophyllite.serialization.PhosphophylliteCompound;
import net.roguelogix.phosphophyllite.util.MethodsReturnNonnullByDefault;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface QuartzTile extends IModularTile {
    
    @Nullable
    default DrawBatch.Instance createInstance(StaticMesh mesh, @Nullable DynamicMatrix dynamicMatrix, @Nullable Matrix4fc staticMatrix) {
        return module(QuartzTile.class, Module.class).createInstance(mesh, dynamicMatrix, staticMatrix);
    }
    
    default DynamicMatrix createDynamicMatrix(@Nullable DynamicMatrix parentTransform, @Nullable DynamicMatrix.UpdateFunc updateFunc){
        return module(QuartzTile.class, Module.class).createDynamicMatrix(parentTransform, updateFunc);
    }
    
    default void rebuildModel(PhosphophylliteCompound modelData) {
        module(QuartzTile.class, Module.class).rebuildModel(modelData);
    }
    
    void buildQuartzModel(PhosphophylliteCompound modelData);
    
    default void updateModelData(PhosphophylliteCompound updateData) {
        module(QuartzTile.class, Module.class).updateModelData(updateData);
    }
    
    void modelDataUpdate(PhosphophylliteCompound updateData);
    
    class Module extends TileModule<QuartzTile> {
        
        private DrawBatch drawBatch;
        private final ObjectArrayList<DrawBatch.Instance> instances = new ObjectArrayList<>();
        private final BlockPos blockPos = iface.as(BlockEntity.class).getBlockPos();
        private final Vector3i position = new Vector3i(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        
        private Module(QuartzTile iface) {
            super(iface);
        }
        
        @Nullable
        private DrawBatch.Instance createInstance(StaticMesh mesh, @Nullable DynamicMatrix dynamicMatrix, @Nullable Matrix4fc staticMatrix) {
            return getDrawBatch().createInstance(position, mesh, dynamicMatrix, staticMatrix, null, null);
        }
    
        DynamicMatrix createDynamicMatrix(@Nullable DynamicMatrix parentTransform, @Nullable DynamicMatrix.UpdateFunc updateFunc){
            return getDrawBatch().createDynamicMatrix(parentTransform, updateFunc);
        }
    
        private DrawBatch getDrawBatch() {
            if (drawBatch == null) {
                drawBatch = Quartz.getDrawBatchForBlock(blockPos);
            }
            return drawBatch;
        }
        
        PhosphophylliteCompound fullModelData;
        PhosphophylliteCompound lastDataUpdate;
        
        private void rebuildModel(PhosphophylliteCompound modelData) {
            this.fullModelData = modelData;
            lastDataUpdate = null;
            requestUpdate();
        }
        
        private void updateModelData(PhosphophylliteCompound modelData) {
            lastDataUpdate = modelData;
            fullModelData.combine(modelData);
            requestUpdate();
        }
        
        private void requestUpdate() {
            var BE = iface.as(BlockEntity.class);
            var level = BE.getLevel();
            if (level != null) {
                level.sendBlockUpdated(BE.getBlockPos(), BE.getBlockState(), BE.getBlockState(), 0);
            }
        }
        
        @Nullable
        @Override
        public CompoundTag getUpdateNBT() {
            var tag = new CompoundTag();
            tag.putByteArray("data", lastDataUpdate.toROBN());
            return tag;
        }
        
        @Override
        public void handleUpdateNBT(CompoundTag nbt) {
            iface.updateModelData(new PhosphophylliteCompound(nbt.getByteArray("data")));
        }
        
        @Nullable
        @Override
        public CompoundTag getDataNBT() {
            var tag = new CompoundTag();
            tag.putByteArray("data", fullModelData.toROBN());
            return tag;
        }
        
        @Override
        public void handleDataNBT(CompoundTag nbt) {
            for (DrawBatch.Instance instance : instances) {
                instance.delete();
            }
            instances.clear();
            iface.buildQuartzModel(new PhosphophylliteCompound(nbt.getByteArray("data")));
        }
    }
}
