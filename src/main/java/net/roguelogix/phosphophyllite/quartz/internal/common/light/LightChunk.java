package net.roguelogix.phosphophyllite.quartz.internal.common.light;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.roguelogix.phosphophyllite.quartz.Quartz;
import net.roguelogix.phosphophyllite.quartz.QuartzDynamicLight;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3ic;
import net.roguelogix.phosphophyllite.util.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.LongConsumer;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LightChunk {
    
    private final long longPos;
    private final LongConsumer deathCallback;
    private final ObjectArrayList<DynamicLightManager.DynamicLight> lights = new ObjectArrayList<>();
    
    private boolean dirty = true;
    
    public LightChunk(long longPos, LongConsumer deathCallback) {
        this.longPos = longPos;
        this.deathCallback = deathCallback;
    }
    
    void markDirty() {
        dirty = true;
    }
    
    public void runUpdate(BlockAndTintGetter blockAndTintGetter) {
        if (!dirty) {
            return;
        }
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < lights.size(); i++) {
            lights.get(i).update(blockAndTintGetter);
        }
        dirty = false;
    }
    
    DynamicLightManager.DynamicLight createLightForPos(Vector3ic pos, DynamicLightManager lightManager, QuartzDynamicLight.Type lightType) {
        var light = lightManager.alloc(createLightUpdateFunc(pos, lightType), this::onLightDeleted);
        lights.add(light);
        markDirty(); // TODO: cache the world and update just this light
        return light;
    }
    
    private void onLightDeleted(DynamicLightManager.DynamicLight light) {
        lights.remove(light);
        if (lights.size() == 0) {
            deathCallback.accept(longPos);
        }
    }
    
    static Quartz.DynamicLightUpdateFunc createLightUpdateFunc(Vector3ic pos, QuartzDynamicLight.Type lightType) {
        return switch (lightType) {
            case SMOOTH -> createSmoothLightUpdateFunc(pos);
            case FLAT -> createSmoothLightUpdateFunc(pos); // TODO: flat, it isn't supported right now, *succ*
            case INTERNAL -> createInternalLightUpdateFunc(pos);
        };
    }
    
    private static Quartz.DynamicLightUpdateFunc createInternalLightUpdateFunc(Vector3ic pos) {
        final var blockPos = new BlockPos(pos.x(), pos.y(), pos.z());
        return (light, blockAndTintGetter) -> {
            int skyLight = blockAndTintGetter.getBrightness(LightLayer.SKY, blockPos);
            int blockLight = blockAndTintGetter.getBrightness(LightLayer.BLOCK, blockPos);
            light.write((byte) (skyLight * 4), (byte) (blockLight * 4), (byte) 0);
        };
    }
    
    static Quartz.DynamicLightUpdateFunc createSmoothLightUpdateFunc(Vector3ic pos) {
        final var lightVals = new int[3][3][3][2];
        final var mutableBlockPos = new BlockPos.MutableBlockPos();
        final var blockPos = new BlockPos(pos.x(), pos.y(), pos.z());
        return (light, blockAndTintGetter) -> {
            for (int i = -1; i < 2; i++) {
                for (int j = -1; j < 2; j++) {
                    for (int k = -1; k < 2; k++) {
                        mutableBlockPos.set(blockPos);
                        mutableBlockPos.move(i, j, k);
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
        };
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
}
