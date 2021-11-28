package net.roguelogix.phosphophyllite.blocks;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.modular.tile.PhosphophylliteTile;
import net.roguelogix.phosphophyllite.quartz.*;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.repack.org.joml.Matrix4f;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3f;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@RegisterTileEntity(name = "phosphophyllite_ore")
public class PhosphophylliteOreTile extends PhosphophylliteTile {
    
    @RegisterTileEntity.Type
    static BlockEntityType<?> TYPE;
    
    public PhosphophylliteOreTile(BlockPos pWorldPosition, BlockState pBlockState) {
        super(TYPE, pWorldPosition, pBlockState);
    }
    
    static {
        Quartz.EVENT_BUS.addListener(PhosphophylliteOreTile::onQuartzStartup);
    }
    
    private static QuartzStaticMesh mesh;
    
    static void onQuartzStartup(QuartzEvent.Startup quartzStartup) {
        Quartz.registerRenderType(RenderType.solid());
        Quartz.registerRenderType(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
        mesh = Quartz.createStaticMesh((builder) -> {
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.STONE.defaultBlockState(), builder.matrixStack(), builder.bufferSource(), 0, 0x00000, net.minecraftforge.client.model.data.EmptyModelData.INSTANCE);
        });
    }
    
    int instanceID = -1;
    QuartzDynamicMatrix quartzMatrix;
    QuartzDynamicLight quartzLight;
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
            quartzLight = Quartz.createDynamicLight((light, blockAndTintGetter) -> {
                // TODO: proper light tracking system
                var lightVals = new int[3][3][3][2];
                var mutableBlockPos = new BlockPos.MutableBlockPos();
                for (int i = -1; i < 2; i++) {
                    for (int j = -1; j < 2; j++) {
                        for (int k = -1; k < 2; k++) {
                            mutableBlockPos.set(getBlockPos());
                            mutableBlockPos.move(i, j, k);
                            mutableBlockPos.move(0, 1, 0);
                            if (blockAndTintGetter.getBlockState(mutableBlockPos).isViewBlocking(blockAndTintGetter, mutableBlockPos)) {
                                lightVals[i + 1][j + 1][k + 1][0] = -1;
                                lightVals[i + 1][j + 1][k + 1][1] = -1;
                            } else {
                                lightVals[i + 1][j + 1][k + 1][0] = blockAndTintGetter.getBrightness(LightLayer.SKY, mutableBlockPos);
                                lightVals[i + 1][j + 1][k + 1][1] = blockAndTintGetter.getBrightness(LightLayer.BLOCK, mutableBlockPos);
                            }
                        }
                    }
                }
                for (int x = 0; x < 2; x++) {
                    for (int y = 0; y < 2; y++) {
                        for (int z = 0; z < 2; z++) {
                            for (int i = 0; i < 2; i++) {
                                {
                                    int defaultVal;
                                    int val;
                                    
                                    int skyLight = 0;
                                    defaultVal = lightVals[x + 1 - i][1][1][0];
                                    defaultVal = defaultVal == -1 ? lightVals[1][1][1][0] : defaultVal;
                                    val = lightVals[x + 1 - i][y][z][0];
                                    skyLight += val == -1 ? defaultVal : val;
                                    val = lightVals[x + 1 - i][y + 1][z][0];
                                    skyLight += val == -1 ? defaultVal : val;
                                    val = lightVals[x + 1 - i][y][z + 1][0];
                                    skyLight += val == -1 ? defaultVal : val;
                                    val = lightVals[x + 1 - i][y + 1][z + 1][0];
                                    skyLight += val == -1 ? defaultVal : val;
                                    
                                    int blockLight = 0;
                                    defaultVal = lightVals[x + 1 - i][1][1][1];
                                    defaultVal = defaultVal == -1 ? lightVals[1][1][1][1] : defaultVal;
                                    val = lightVals[x + 1 - i][y][z][1];
                                    blockLight += val == -1 ? defaultVal : val;
                                    val = lightVals[x + 1 - i][y + 1][z][1];
                                    blockLight += val == -1 ? defaultVal : val;
                                    val = lightVals[x + 1 - i][y][z + 1][1];
                                    blockLight += val == -1 ? defaultVal : val;
                                    val = lightVals[x + 1 - i][y + 1][z + 1][1];
                                    blockLight += val == -1 ? defaultVal : val;
                                    
                                    byte AO = AOMode(lightVals[x + 1 - i][y * 2][1][1] == -1, lightVals[x + 1 - i][y * 2][z * 2][1] == -1, lightVals[x + 1 - i][1][z * 2][1] == -1);
                                    
                                    light.write(z * 4 + y * 2 + x, i * 3, (byte) skyLight, (byte) blockLight, AO);
                                }
                                {
                                    int defaultVal;
                                    int val;
                                    
                                    int skyLight = 0;
                                    defaultVal = lightVals[1][y + 1 - i][1][0];
                                    defaultVal = defaultVal == -1 ? lightVals[1][1][1][0] : defaultVal;
                                    val = lightVals[x][y + 1 - i][z][0];
                                    skyLight += val == -1 ? defaultVal : val;
                                    val = lightVals[x + 1][y + 1 - i][z][0];
                                    skyLight += val == -1 ? defaultVal : val;
                                    val = lightVals[x][y + 1 - i][z + 1][0];
                                    skyLight += val == -1 ? defaultVal : val;
                                    val = lightVals[x + 1][y + 1 - i][z + 1][0];
                                    skyLight += val == -1 ? defaultVal : val;
                                    
                                    int blockLight = 0;
                                    defaultVal = lightVals[1][y + 1 - i][1][1];
                                    defaultVal = defaultVal == -1 ? lightVals[1][1][1][1] : defaultVal;
                                    val = lightVals[x][y + 1 - i][z][1];
                                    blockLight += val == -1 ? defaultVal : val;
                                    val = lightVals[x + 1][y + 1 - i][z][1];
                                    blockLight += val == -1 ? defaultVal : val;
                                    val = lightVals[x][y + 1 - i][z + 1][1];
                                    blockLight += val == -1 ? defaultVal : val;
                                    val = lightVals[x + 1][y + 1 - i][z + 1][1];
                                    blockLight += val == -1 ? defaultVal : val;
                                    
                                    byte AO = AOMode(lightVals[x * 2][y + 1 - i][1][1] == -1, lightVals[x * 2][y + 1 - i][z * 2][1] == -1, lightVals[1][y + 1 - i][z * 2][1] == -1);
                                    
                                    light.write(z * 4 + y * 2 + x, 1 + i * 3, (byte) skyLight, (byte) blockLight, AO);
                                }
                                {
                                    int defaultVal;
                                    int val;
                                    
                                    int skyLight = 0;
                                    defaultVal = lightVals[1][1][z + 1 - i][0];
                                    defaultVal = defaultVal == -1 ? lightVals[1][1][1][0] : defaultVal;
                                    val = lightVals[x][y][z + 1 - i][0];
                                    skyLight += val == -1 ? defaultVal : val;
                                    val = lightVals[x + 1][y][z + 1 - i][0];
                                    skyLight += val == -1 ? defaultVal : val;
                                    val = lightVals[x][y + 1][z + 1 - i][0];
                                    skyLight += val == -1 ? defaultVal : val;
                                    val = lightVals[x + 1][y + 1][z + 1 - i][0];
                                    skyLight += val == -1 ? defaultVal : val;
                                    
                                    int blockLight = 0;
                                    defaultVal = lightVals[1][1][z + 1 - i][1];
                                    defaultVal = defaultVal == -1 ? lightVals[1][1][1][1] : defaultVal;
                                    val = lightVals[x][y][z + 1 - i][1];
                                    blockLight += val == -1 ? defaultVal : val;
                                    val = lightVals[x + 1][y][z + 1 - i][1];
                                    blockLight += val == -1 ? defaultVal : val;
                                    val = lightVals[x][y + 1][z + 1 - i][1];
                                    blockLight += val == -1 ? defaultVal : val;
                                    val = lightVals[x + 1][y + 1][z + 1 - i][1];
                                    blockLight += val == -1 ? defaultVal : val;
                                    
                                    byte AO = AOMode(lightVals[x * 2][1][z + 1 - i][1] == -1, lightVals[x * 2][y * 2][z + 1 - i][1] == -1, lightVals[1][y * 2][z + 1 - i][1] == -1);
                                    
                                    light.write(z * 4 + y * 2 + x, 2 + i * 3, (byte) skyLight, (byte) blockLight, AO);
                                }
                            }
                        }
                    }
                }
            });
            quartzMatrix = Quartz.createDynamicMatrix((matrix, nanoSinceLastFrame, partialTicks, playerBlock, playerPartialBlock) -> {
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
            instanceID = Quartz.registerStaticMeshInstance(mesh, new Vector3i(getBlockPos().getX(), getBlockPos().getY() + 1, getBlockPos().getZ()), quartzMatrix, new Matrix4f().translate(0, 0, 0), quartzLight);
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
            Quartz.unregisterStaticMeshInstance(instanceID);
            instanceID = -1;
            quartzMatrix.dispose();
            quartzLight.dispose();
        }
    }
}
