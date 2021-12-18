package net.roguelogix.phosphophyllite.quartz.internal.common;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.roguelogix.phosphophyllite.quartz.DynamicLight;
import net.roguelogix.phosphophyllite.quartz.internal.QuartzCore;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3ic;
import net.roguelogix.phosphophyllite.threading.Queues;
import net.roguelogix.phosphophyllite.util.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.LongConsumer;

import static net.roguelogix.phosphophyllite.repack.org.joml.Math.abs;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LightEngine {
    
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Long2ObjectOpenHashMap<SoftReference<Chunk>> liveChunks = new Long2ObjectOpenHashMap<>();
    
    public LightEngine() {
    }
    
    private Chunk getChunkFor(long longPos) {
        boolean readLocked = false;
        boolean writeLocked = false;
        try {
            readLocked = true;
            lock.readLock().lock();
            var chunkRef = liveChunks.get(longPos);
            lock.readLock().unlock();
            readLocked = false;
        
            Chunk chunk = null;
            if (chunkRef != null) {
                chunk = chunkRef.get();
            }
            if (chunk == null) {
                chunk = new Chunk(longPos, lonk -> {
                    try {
                        lock.writeLock().lock();
                        liveChunks.remove(lonk);
                    } finally {
                        lock.writeLock().unlock();
                    }
                });
                writeLocked = true;
                lock.writeLock().lock();
                liveChunks.put(longPos, new SoftReference<>(chunk));
                lock.writeLock().unlock();
                writeLocked = false;
            }
            return chunk;
        } finally {
            if (writeLocked) {
                lock.writeLock().unlock();
            }
            if (readLocked) {
                lock.readLock().unlock();
            }
        }
    }
    
    public void update(BlockAndTintGetter blockAndTintGetter) {
        try {
            lock.readLock().lock();
            liveChunks.forEach(((aLong, lightChunk) -> {
                var chunk = lightChunk.get();
                if (chunk == null) {
                    return;
                }
                chunk.runUpdate(blockAndTintGetter);
            }));
        } finally {
            lock.readLock().unlock();
        }
    }
    
    public void sectionDirty(int x, int y, int z) {
        long pos = SectionPos.asLong(x, y, z);
        SoftReference<Chunk> chunkRef;
        try {
            lock.readLock().lock();
            chunkRef = liveChunks.get(pos);
        } finally {
            lock.readLock().unlock();
        }
        Chunk chunk = null;
        if (chunkRef != null) {
            chunk = chunkRef.get();
        }
        if (chunk != null) {
            chunk.markDirty();
        }
    }
    
    public DynamicLight createLightForPos(Vector3ic pos, DynamicLight.Manager lightManager, DynamicLight.Type lightType) {
        long longPos = BlockPos.asLong(pos.x(), pos.y(), pos.z());
        longPos = SectionPos.blockToSection(longPos);
        var chunk = getChunkFor(longPos);
        return chunk.createLightForPos(pos, lightManager, lightType);
    }
    
    private static class Chunk {
        private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        private final ObjectArrayList<WeakReference<DynamicLight>> lights = new ObjectArrayList<>();
        
        private boolean dirty = true; // this may need to be volatile
        
        public Chunk(long longPos, LongConsumer deathCallback) {
            QuartzCore.CLEANER.register(this, createCleanerFunc(longPos, deathCallback));
        }
        
        private static Runnable createCleanerFunc(long pos, LongConsumer callback) {
            return () -> callback.accept(pos);
        }
        
        void markDirty() {
            dirty = true;
        }
        
        public void runUpdate(BlockAndTintGetter blockAndTintGetter) {
            // this dirty flag check is thread safe
            // if marked for update while updating
            // a new update will be triggered next frame
            if (!dirty) {
                return;
            }
            dirty = false;
            boolean hasNullElement = false;
            try {
                lock.readLock().lock();
                for (final var dynamicLightWeakReference : lights) {
                    var light = dynamicLightWeakReference.get();
                    if (light == null) {
                        hasNullElement = true;
                        continue;
                    }
                    light.update(blockAndTintGetter);
                }
            } finally {
                lock.readLock().unlock();
            }
            if (hasNullElement) {
                Queues.offThread.enqueue(() -> {
                    try {
                        lock.writeLock().lock();
                        for (int i = 0; i < lights.size(); i++) {
                            final var lightRef = lights.get(i);
                            var light = lightRef.get();
                            while (light == null) {
                                var end = lights.pop();
                                var endLight = end.get();
                                if (endLight != null) {
                                    light = endLight;
                                    lights.set(i, end);
                                }
                            }
                        }
                    } finally {
                        lock.writeLock().unlock();
                    }
                });
            }
        }
        
        DynamicLight createLightForPos(Vector3ic pos, DynamicLight.Manager lightManager, DynamicLight.Type lightType) {
            var light = lightManager.createLight(createLightUpdateFunc(pos, lightType));
            var lightRef = new WeakReference<>(light);
            QuartzCore.CLEANER.register(light, () -> this.onLightDeleted(lightRef));
            try {
                lock.writeLock().lock();
                lights.add(lightRef);
                //  mojang directly uses the level as a light engine on multiple threads, so, im going to assume this is safe on multiple threads too
                light.update(Minecraft.getInstance().level);
            } finally {
                lock.writeLock().unlock();
            }
            return light;
        }
        
        private void onLightDeleted(WeakReference<DynamicLight> lightRef) {
            try {
                lock.writeLock().lock();
                lights.remove(lightRef);
            } finally {
                lock.writeLock().unlock();
            }
        }
        
        static DynamicLight.UpdateFunc createLightUpdateFunc(Vector3ic pos, DynamicLight.Type lightType) {
            return switch (lightType) {
                case SMOOTH -> createSmoothLightUpdateFunc(pos);
                case FLAT -> createFlatLightUpdateFunc(pos);
                case INTERNAL -> createInternalLightUpdateFunc(pos);
            };
        }
        
        private static DynamicLight.UpdateFunc createSmoothLightUpdateFunc(Vector3ic pos) {
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
        
        private static DynamicLight.UpdateFunc createFlatLightUpdateFunc(Vector3ic pos) {
            final var lightVals = new int[3][3][3][2];
            final var mutableBlockPos = new BlockPos.MutableBlockPos();
            final var blockPos = new BlockPos(pos.x(), pos.y(), pos.z());
            return (light, blockAndTintGetter) -> {
                int skyLight = blockAndTintGetter.getBrightness(LightLayer.SKY, blockPos);
                int blockLight = blockAndTintGetter.getBrightness(LightLayer.BLOCK, blockPos);
                for (int i = -1; i < 2; i++) {
                    for (int j = -1; j < 2; j++) {
                        for (int k = -1; k < 2; k++) {
                            int absSum = abs(i) + abs(j) + abs(k);
                            if (absSum > 1) {
                                continue;
                            }
                            mutableBlockPos.set(blockPos);
                            mutableBlockPos.move(i, j, k);
                            if (blockAndTintGetter.getBlockState(mutableBlockPos).isViewBlocking(blockAndTintGetter, mutableBlockPos)) {
                                lightVals[i + 1][j + 1][k + 1][0] = skyLight;
                                lightVals[i + 1][j + 1][k + 1][1] = blockLight;
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
                                    var sky = lightVals[x + 1 - i][1][1][0];
                                    var block = lightVals[x + 1 - i][1][1][1];
                                    light.write(z * 4 + y * 2 + x, i * 3, (byte) sky, (byte) block, (byte) 0);
                                }
                                {
                                    var sky = lightVals[1][y + 1 - i][1][0];
                                    var block = lightVals[1][y + 1 - i][1][1];
                                    light.write(z * 4 + y * 2 + x, 1 + i * 3, (byte) sky, (byte) block, (byte) 0);
                                }
                                {
                                    var sky = lightVals[1][1][z + 1 - i][0];
                                    var block = lightVals[1][1][z + 1 - i][1];
                                    light.write(z * 4 + y * 2 + x, 2 + i * 3, (byte) sky, (byte) block, (byte) 0);
                                }
                            }
                        }
                    }
                }
            };
        }
        
        private static DynamicLight.UpdateFunc createInternalLightUpdateFunc(Vector3ic pos) {
            final var blockPos = new BlockPos(pos.x(), pos.y(), pos.z());
            return (light, blockAndTintGetter) -> {
                int skyLight = blockAndTintGetter.getBrightness(LightLayer.SKY, blockPos);
                int blockLight = blockAndTintGetter.getBrightness(LightLayer.BLOCK, blockPos);
                light.write((byte) (skyLight * 4), (byte) (blockLight * 4), (byte) 0);
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
    
}
