package net.roguelogix.phosphophyllite.multiblock2.rectangular;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.roguelogix.phosphophyllite.multiblock2.IMultiblockTile;
import net.roguelogix.phosphophyllite.multiblock2.MultiblockController;
import net.roguelogix.phosphophyllite.multiblock2.ValidationException;
import net.roguelogix.phosphophyllite.multiblock2.modular.IModularMultiblockController;
import net.roguelogix.phosphophyllite.multiblock2.modular.MultiblockControllerModule;
import net.roguelogix.phosphophyllite.multiblock2.modular.MultiblockControllerModuleRegistry;
import net.roguelogix.phosphophyllite.registry.OnModLoad;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3i;
import net.roguelogix.phosphophyllite.repack.org.joml.Vector3ic;
import net.roguelogix.phosphophyllite.util.NonnullDefault;
import net.roguelogix.phosphophyllite.util.Util;

import javax.annotation.Nullable;

@NonnullDefault
public interface IRectangularMultiblock<
        TileType extends BlockEntity & IRectangularMultiblockTile<TileType, BlockType, ControllerType>,
        BlockType extends Block & IRectangularMultiblockBlock,
        ControllerType extends MultiblockController<TileType, BlockType, ControllerType> & IRectangularMultiblock<TileType, BlockType, ControllerType>
        > extends IModularMultiblockController<TileType, BlockType, ControllerType> {
    
    @Nullable
    default Vector3ic minSize() {
        return null;
    }
    
    @Nullable
    default Vector3ic maxSize() {
        return null;
    }
    
    default boolean orientationAgnostic() {
        return true;
    }
    
    default boolean xzAgnostic() {
        return true;
    }
    
    default void rectangularValidationStarted() {
    }
    
    default void rectangularBlockValidated(Block block) {
    }
    
    default boolean allowedInteriorBlock(Block block) {
        return false;
    }
    
    final class Module<
            TileType extends BlockEntity & IRectangularMultiblockTile<TileType, BlockType, ControllerType>,
            BlockType extends Block & IRectangularMultiblockBlock,
            ControllerType extends MultiblockController<TileType, BlockType, ControllerType> & IRectangularMultiblock<TileType, BlockType, ControllerType>
            > extends MultiblockControllerModule<TileType, BlockType, ControllerType> {
        
        @OnModLoad
        public static void register() {
            MultiblockControllerModuleRegistry.registerModule(IRectangularMultiblock.class, Module::new);
        }
        
        public Module(IRectangularMultiblock<TileType, BlockType, ControllerType> controller) {
            super(controller);
        }
        
        @Override
        public void preValidate() throws ValidationException {
            final var min = controller.min();
            final var max = controller.max();
            int minX = min.x();
            int minY = min.y();
            int minZ = min.z();
            int maxX = max.x();
            int maxY = max.y();
            int maxZ = max.z();
            
            final var allowedOrientations = new Vector3i[controller.orientationAgnostic() ? 6 : controller.xzAgnostic() ? 2 : 1];
            
            if (controller.orientationAgnostic()) {
                allowedOrientations[0] = new Vector3i(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
                allowedOrientations[1] = new Vector3i(maxX - minX + 1, maxZ - minZ + 1, maxY - minY + 1);
                
                allowedOrientations[2] = new Vector3i(maxY - minY + 1, maxX - minX + 1, maxZ - minZ + 1);
                allowedOrientations[3] = new Vector3i(maxY - minY + 1, maxZ - minZ + 1, maxX - minX + 1);
                
                allowedOrientations[4] = new Vector3i(maxZ - minZ + 1, maxX - minX + 1, maxY - minY + 1);
                allowedOrientations[5] = new Vector3i(maxZ - minZ + 1, maxY - minY + 1, maxX - minX + 1);
            } else if (controller.xzAgnostic()) {
                allowedOrientations[0] = new Vector3i(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
                allowedOrientations[1] = new Vector3i(maxZ - minZ + 1, maxY - minY + 1, maxX - minX + 1);
            } else {
                allowedOrientations[0] = new Vector3i(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
            }
            
            final var minSize = controller.minSize();
            final var maxSize = controller.maxSize();
            
            Vector3i dimensions = null;
            for (Vector3i allowedOrientation : allowedOrientations) {
                if (minSize != null) {
                    if (
                            allowedOrientation.x < minSize.x() ||
                                    allowedOrientation.y < minSize.y() ||
                                    allowedOrientation.z < minSize.z()
                    ) {
                        continue;
                    }
                }
                if (maxSize != null) {
                    if (
                            allowedOrientation.x > maxSize.x() ||
                                    allowedOrientation.y > maxSize.y() ||
                                    allowedOrientation.z > maxSize.z()
                    ) {
                        continue;
                    }
                }
                dimensions = allowedOrientation;
                break;
            }
            // dimension check failed in all orientations
            if (dimensions == null) {
                final var minSizenn = minSize != null ? minSize : new Vector3i();
                final var maxSizenn = maxSize != null ? maxSize : new Vector3i();
                throw new ValidationException(Component.translatable("multiblock.error.phosphophyllite.dimensions",
                        allowedOrientations[0].x, allowedOrientations[0].y, allowedOrientations[0].z,
                        minSizenn.x(), minSizenn.y(), minSizenn.z(),
                        maxSizenn.x(), maxSizenn.y(), maxSizenn.z()));
            }
            // or it didnt, at this point i dont really know, and you dont either, works(tm)
        }
        
        @Override
        public void validate() throws ValidationException {
            controller.rectangularValidationStarted();
            try {
                Util.chunkCachedBlockStateIteration(controller.min(), controller.max(), controller.level, (blockState, pos) -> {
                    try {
                        blockValidation(blockState, pos);
                    } catch (ValidationException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (RuntimeException e) {
                throw (ValidationException) e.getCause();
            }
        }
        
        private void blockValidation(BlockState blockState, Vector3ic pos) throws ValidationException {
            final var min = controller.min();
            final var max = controller.max();
            int minX = min.x();
            int minY = min.y();
            int minZ = min.z();
            int maxX = max.x();
            int maxY = max.y();
            int maxZ = max.z();
            
            Block block = blockState.getBlock();
            int extremes = 0;
            if (pos.x() == minX || pos.x() == maxX) {
                extremes++;
            }
            if (pos.y() == minY || pos.y() == maxY) {
                extremes++;
            }
            if (pos.z() == minZ || pos.z() == maxZ) {
                extremes++;
            }
            switch (extremes) {
                case 3 -> {
                    if (block instanceof IRectangularMultiblockBlock && controller.blockTypeValidator.validate(block)) {
                        if (!((IRectangularMultiblockBlock) block).isGoodForCorner()) {
                            throw new InvalidBlock(block, pos, "corner");
                        } else {
                            break;
                        }
                    }
                    throw new InvalidBlock(block, pos, "corner");
                }
                case 2 -> {
                    if (block instanceof IRectangularMultiblockBlock && controller.blockTypeValidator.validate(block)) {
                        if (!((IRectangularMultiblockBlock) block).isGoodForFrame()) {
                            throw new InvalidBlock(block, pos, "frame");
                        } else {
                            break;
                        }
                    }
                    throw new InvalidBlock(block, pos, "frame");
                }
                case 1 -> {
                    if (block instanceof IRectangularMultiblockBlock && controller.blockTypeValidator.validate(block)) {
                        if (!((IRectangularMultiblockBlock) block).isGoodForExterior()) {
                            throw new InvalidBlock(block, pos, "exterior");
                        } else {
                            break;
                        }
                    }
                    throw new InvalidBlock(block, pos, "exterior");
                }
                default -> {
                    if (block instanceof IRectangularMultiblockBlock && controller.blockTypeValidator.validate(block)) {
                        if (!((IRectangularMultiblockBlock) block).isGoodForInterior()) {
                            throw new InvalidBlock(block, pos, "interior");
                        } else {
                            break;
                        }
                    }
                    
                    if (!controller.allowedInteriorBlock(block)) {
                        throw new InvalidBlock(block, pos, "interior");
                    }
                }
            }
            controller.rectangularBlockValidated(block);
        }
        
    }
}
