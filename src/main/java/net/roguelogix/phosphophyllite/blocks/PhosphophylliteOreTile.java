package net.roguelogix.phosphophyllite.blocks;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.modular.tile.PhosphophylliteTile;
import net.roguelogix.phosphophyllite.quartz.DrawBatch;
import net.roguelogix.phosphophyllite.quartz.Quartz;
import net.roguelogix.phosphophyllite.quartz.QuartzEvent;
import net.roguelogix.phosphophyllite.quartz.StaticMesh;
import net.roguelogix.phosphophyllite.registry.RegisterTile;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix4f;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3f;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PhosphophylliteOreTile extends PhosphophylliteTile {
    
    @RegisterTile(name = "phosphophyllite_ore")
    public static final BlockEntityType.BlockEntitySupplier<PhosphophylliteOreTile> SUPPLIER = new RegisterTile.Producer<>(PhosphophylliteOreTile::new);
    
    public PhosphophylliteOreTile(BlockEntityType<?> TYPE, BlockPos pWorldPosition, BlockState pBlockState) {
        super(TYPE, pWorldPosition, pBlockState);
    }
    
    static {
        Quartz.EVENT_BUS.addListener(PhosphophylliteOreTile::onQuartzStartup);
    }
    
    private static StaticMesh mesh;
    
    static void onQuartzStartup(QuartzEvent.Startup quartzStartup) {
        mesh = Quartz.createStaticMesh(Blocks.STONE.defaultBlockState());
    }
    
    DrawBatch.Instance instance = null;
    Matrix4f spinMatrix = new Matrix4f();
    float rotation = 0;
    
    @Override
    public void onAdded() {
        assert level != null;
        if (!level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 0);
            return;
        }
        if (mesh != null && level.isClientSide()) {
            var modelPos = new Vector3i(getBlockPos().getX(), getBlockPos().getY() + 1, getBlockPos().getZ());
            var batcher = Quartz.getDrawBatcherForBlock(modelPos);
            var quartzMatrix = batcher.createDynamicMatrix((matrix, nanoSinceLastFrame, partialTicks, playerBlock, playerPartialBlock) -> {
                rotation += nanoSinceLastFrame / 1_000_000_000f;
                spinMatrix.identity();
//                spinMatrix.scale(1.5f);
//                spinMatrix.translate(-0.1875f, -0.1875f, -0.1875f);
                spinMatrix.scale(0.5f);
                spinMatrix.translate(0.5f, 0.5f, 0.5f);
                spinMatrix.translate(0.5f, 0.5f, 0.5f);
                spinMatrix.rotate(rotation, new Vector3f(1, 1, 1).normalize());
                spinMatrix.translate(-0.5f, -0.5f, -0.5f);
//                spinMatrix.translate(0, 2, 0);
                matrix.write(spinMatrix);
            });
            instance = batcher.createInstance(modelPos, mesh, quartzMatrix, null, null, null);
        }
    }
    
    private static byte AOMode(boolean sideA, boolean corner, boolean sideB) {
        if (sideA && sideB) {
            return 3;
        }
        if ((sideA || sideB) && corner) {
            return 2;
        }
        if (sideA || sideB || corner) {
            return 1;
        }
        return 0;
    }
    
    @Override
    public void onRemoved(boolean chunkUnload) {
        assert level != null;
        if (mesh != null && level.isClientSide()) {
            instance.delete();
            instance = null;
        }
    }
}
