package net.roguelogix.phosphophyllite.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector2i;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3ic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class Util {
    public static String readResourceLocation(ResourceLocation location) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Minecraft.getInstance().getResourceManager().getResource(location).getInputStream()))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
            }
            return stringBuilder.toString();
        } catch (IOException ignored) {
        }
        return null;
    }
    
    public static JsonObject readJSONFile(ResourceLocation location) {
        if (location.getPath().lastIndexOf(".json") != location.getPath().length() - 5) {
            location = new ResourceLocation(location.getNamespace(), location.getPath() + ".json");
        }
        String jsonString = readResourceLocation(location);
        if (jsonString == null) {
            return null;
        }
        JsonElement element = new JsonParser().parse(jsonString);
        if (element instanceof JsonObject) {
            return (JsonObject) element;
        }
        return null;
    }
    
    public static void chunkCachedBlockStateIteration(Vector3ic start, Vector3ic end, Level world, BiConsumer<BlockState, Vector3i> func) {
        chunkCachedBlockStateIteration(start, end, world, func, new Vector3i());
    }
    
    public static void chunkCachedBlockStateIteration(Vector3ic start, Vector3ic end, Level world, BiConsumer<BlockState, Vector3i> func, Vector3i scratchVector) {
        // ChunkSource implementations are indexed [z][x]
        for (int Z = start.z(); Z < ((end.z() + 16) & 0xFFFFFFF0); Z += 16) {
            for (int X = start.x(); X < ((end.x() + 16) & 0xFFFFFFF0); X += 16) {
                int chunkX = X >> 4;
                int chunkZ = Z >> 4;
                LevelChunk chunk = (LevelChunk) world.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                LevelChunkSection[] chunkSections = chunk != null ? chunk.getSections() : null;
                int chunkMinSection = chunk != null ? chunk.getMinSection() : 0;
                for (int Y = start.y(); Y < ((end.y() + 16) & 0xFFFFFFF0); Y += 16) {
                    int chunkSectionIndex = Y >> 4 - chunkMinSection;
                    LevelChunkSection chunkSection = chunkSections != null ? chunkSections[chunkSectionIndex] : null;
                    int sectionMinX = Math.max((X) & 0xFFFFFFF0, start.x());
                    int sectionMinY = Math.max((Y) & 0xFFFFFFF0, start.y());
                    int sectionMinZ = Math.max((Z) & 0xFFFFFFF0, start.z());
                    int sectionMaxX = Math.min((X + 16) & 0xFFFFFFF0, end.x() + 1);
                    int sectionMaxY = Math.min((Y + 16) & 0xFFFFFFF0, end.y() + 1);
                    int sectionMaxZ = Math.min((Z + 16) & 0xFFFFFFF0, end.z() + 1);
                    for (int x = sectionMinX; x < sectionMaxX; x++) {
                        for (int y = sectionMinY; y < sectionMaxY; y++) {
                            for (int z = sectionMinZ; z < sectionMaxZ; z++) {
                                scratchVector.set(x, y, z);
                                BlockState state = Blocks.AIR.defaultBlockState();
                                if (chunkSection != null) {
                                    state = chunkSection.getBlockState(x & 15, y & 15, z & 15);
                                }
                                func.accept(state, scratchVector);
                            }
                        }
                    }
                    // PalettedContainers are indexed [y][z][x]
                    for (int y = sectionMinY; y < sectionMaxY; y++) {
						for (int z = sectionMinZ; z < sectionMaxZ; ++z) {
							for (int x = sectionMinX; x < sectionMaxX; x++) {
                                scratchVector.set(x, y, z);
                                BlockState state = Blocks.AIR.defaultBlockState();
                                if (chunkSection != null) {
                                    state = chunkSection.getBlockState(x & 15, y & 15, z & 15);
                                }
								func.accept(state, scratchVector);
							}
						}
					}
                }
            }
        }
    }
    
    public static void markRangeDirty(Level world, Vector2i start, Vector2i end) {
        for (int X = start.x; X < ((end.x + 16) & 0xFFFFFFF0); X += 16) {
            for (int Z = start.y; Z < ((end.y + 16) & 0xFFFFFFF0); Z += 16) {
                int chunkX = X >> 4;
                int chunkZ = Z >> 4;
                LevelChunk chunk = world.getChunk(chunkX, chunkZ);
                chunk.markUnsaved();
            }
        }
    }
    
    public static void setBlockStates(Map<BlockPos, BlockState> newStates, Level world) {
        HashMap<BlockPos, HashMap<BlockPos, BlockState>> stateChunks = new HashMap<>();
        BlockPos.MutableBlockPos chunkPos = new BlockPos.MutableBlockPos();
        newStates.forEach((pos, state) -> {
            chunkPos.set(pos.getX() >> 4, 0, pos.getZ() >> 4);
            HashMap<BlockPos, BlockState> chunksNewStates = stateChunks.get(chunkPos);
            if (chunksNewStates == null) {
                chunksNewStates = new HashMap<>();
                stateChunks.put(chunkPos.immutable(), chunksNewStates);
            }
            chunksNewStates.put(pos, state);
        });
        stateChunks.forEach((cPos, states) -> {
            LevelChunk chunk = world.getChunk(cPos.getX(), cPos.getZ());
            LevelChunkSection[] chunkSections = chunk.getSections();
            states.forEach((bPos, state) -> {
                LevelChunkSection section = chunkSections[bPos.getY() >> 4];
                if (section != null) {
                    BlockState oldState = section.setBlockState(bPos.getX() & 15, bPos.getY() & 15, bPos.getZ() & 15, state);
                    world.sendBlockUpdated(bPos, oldState, state, 0);
                }
            });
            chunk.markUnsaved();
        });
    }
    
    public static Direction directionFromPositions(BlockPos reference, BlockPos neighbor) {
        int xDifference = reference.getX() - neighbor.getX();
        int yDifference = reference.getY() - neighbor.getY();
        int zDifference = reference.getZ() - neighbor.getZ();
        if(Math.abs(xDifference) + Math.abs(yDifference) + Math.abs(zDifference) > 1){
            throw new IllegalArgumentException("positions not neighbors");
        }
        if (xDifference == -1) {
            return Direction.WEST;
        } else if (xDifference == 1) {
            return Direction.EAST;
        } else if (yDifference == -1) {
            return Direction.DOWN;
        } else if (yDifference == 1) {
            return Direction.UP;
        } else if (zDifference == -1) {
            return Direction.NORTH;
        } else if (zDifference == 1) {
            return Direction.SOUTH;
        }
        throw new IllegalArgumentException("identical positions gives");
    }
}
