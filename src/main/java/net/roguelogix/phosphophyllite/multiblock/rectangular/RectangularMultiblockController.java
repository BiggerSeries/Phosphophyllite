package net.roguelogix.phosphophyllite.multiblock.rectangular;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.roguelogix.phosphophyllite.multiblock.IMultiblockTile;
import net.roguelogix.phosphophyllite.multiblock.MultiblockController;
import net.roguelogix.phosphophyllite.multiblock.ValidationError;
import net.roguelogix.phosphophyllite.multiblock.Validator;
import net.roguelogix.phosphophyllite.util.Util;
import org.joml.Vector3i;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

@Deprecated(forRemoval = true)
public class RectangularMultiblockController<
        TileType extends BlockEntity & IRectangularMultiblockTile<TileType, ControllerType>,
        ControllerType extends RectangularMultiblockController<TileType, ControllerType>
        > extends MultiblockController<TileType, ControllerType> {
    
    final Validator<Block> blockTypeValidator;
    
    public RectangularMultiblockController(@Nonnull Level world, @Nonnull Validator<IMultiblockTile<?, ?>> tileTypeValidator, @Nonnull Validator<Block> blockTypeValidator) {
        super(world, tileTypeValidator, true);
        this.blockTypeValidator = blockTypeValidator;
    }
    
    protected boolean orientationAgnostic = true;
    protected boolean xzAgnostic = true;
    
    protected final Vector3i minSize = new Vector3i();
    protected final Vector3i maxSize = new Vector3i();
    
    protected Validator<Block> cornerValidator = null;
    protected Validator<Block> frameValidator = null;
    protected Validator<Block> exteriorValidator = null;
    protected Validator<Block> interiorValidator = null;
    protected Validator<Block> genericValidator = null;
    protected Consumer<Block> blockValidatedCallback = null;
    protected Runnable validationStartedCallback = null;
    
    private final Validator<ControllerType> mainValidator = controller -> {
        int minX = controller.minCoord().x();
        int minY = controller.minCoord().y();
        int minZ = controller.minCoord().z();
        int maxX = controller.maxCoord().x();
        int maxY = controller.maxCoord().y();
        int maxZ = controller.maxCoord().z();
        
        Vector3i[] allowedOrientations = new Vector3i[controller.orientationAgnostic ? 6 : controller.xzAgnostic ? 2 : 1];
        
        if (controller.orientationAgnostic) {
            allowedOrientations[0] = new Vector3i(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
            allowedOrientations[1] = new Vector3i(maxX - minX + 1, maxZ - minZ + 1, maxY - minY + 1);
            
            allowedOrientations[2] = new Vector3i(maxY - minY + 1, maxX - minX + 1, maxZ - minZ + 1);
            allowedOrientations[3] = new Vector3i(maxY - minY + 1, maxZ - minZ + 1, maxX - minX + 1);
            
            allowedOrientations[4] = new Vector3i(maxZ - minZ + 1, maxX - minX + 1, maxY - minY + 1);
            allowedOrientations[5] = new Vector3i(maxZ - minZ + 1, maxY - minY + 1, maxX - minX + 1);
        } else if (controller.xzAgnostic) {
            allowedOrientations[0] = new Vector3i(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
            allowedOrientations[1] = new Vector3i(maxZ - minZ + 1, maxY - minY + 1, maxX - minX + 1);
        } else {
            allowedOrientations[0] = new Vector3i(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);
        }
        
        Vector3i dimensions = null;
        for (Vector3i allowedOrientation : allowedOrientations) {
            // if all are positive, technically zero is valid for them
            // dont know why you would use zero, but that's not my problem
            // i guess to lock out using the machine?
            if ((controller.minSize.x | controller.minSize.y | controller.minSize.z) >= 0) {
                if (
                        allowedOrientation.x < controller.minSize.x ||
                                allowedOrientation.y < controller.minSize.y ||
                                allowedOrientation.z < controller.minSize.z
                ) {
                    continue;
                }
            }
            // you can also just set one of these lower than the above
            // see the below bounds checks
            if ((controller.maxSize.x | controller.maxSize.y | controller.maxSize.z) >= 0) {
                if (
                        allowedOrientation.x > controller.maxSize.x ||
                                allowedOrientation.y > controller.maxSize.y ||
                                allowedOrientation.z > controller.maxSize.z
                ) {
                    continue;
                }
            }
            dimensions = allowedOrientation;
            break;
        }
        // dimension check failed in all orientations
        if (dimensions == null) {
            throw new ValidationError(Component.translatable("multiblock.error.phosphophyllite.dimensions",
                    allowedOrientations[0].x, allowedOrientations[0].y, allowedOrientations[0].z,
                    controller.minSize.x, controller.minSize.y, controller.minSize.z,
                    controller.maxSize.x, controller.maxSize.y, controller.maxSize.z));
        }
        // or it didnt, at this point i dont really know, and you dont either, works(tm)
    
        if (validationStartedCallback != null) {
            validationStartedCallback.run();
        }
        Util.chunkCachedBlockStateIteration(controller.minCoord(), controller.maxCoord(), controller.world, (blockState, pos) -> {
            Block block = blockState.getBlock();
            int extremes = 0;
            if (pos.x == minX || pos.x == maxX) {
                extremes++;
            }
            if (pos.y == minY || pos.y == maxY) {
                extremes++;
            }
            if (pos.z == minZ || pos.z == maxZ) {
                extremes++;
            }
            switch (extremes) {
                case 3: {
                    if (block instanceof IRectangularMultiblockBlock && controller.blockTypeValidator.validate(block)) {
                        if (!((IRectangularMultiblockBlock) block).isGoodForCorner()) {
                            throw new InvalidBlock(block, pos, "corner");
                        } else {
                            break;
                        }
                    } else if (controller.cornerValidator != null) {
                        // can you be a corner?
                        if (!controller.cornerValidator.validate(block)) {
                            throw new InvalidBlock(block, pos, "corner");
                        } else {
                            break;
                        }
                    }
                }
                case 2: {
                    if (block instanceof IRectangularMultiblockBlock && controller.blockTypeValidator.validate(block)) {
                        if (!((IRectangularMultiblockBlock) block).isGoodForFrame()) {
                            throw new InvalidBlock(block, pos, "frame");
                        } else {
                            break;
                        }
                    } else if (controller.frameValidator != null) {
                        // dont care whats on the corners, but we do on the frame as a whole
                        if (!controller.frameValidator.validate(block)) {
                            throw new InvalidBlock(block, pos, "frame");
                        } else {
                            break;
                        }
                    }
                }
                case 1: {
                    if (block instanceof IRectangularMultiblockBlock && controller.blockTypeValidator.validate(block)) {
                        if (!((IRectangularMultiblockBlock) block).isGoodForExterior()) {
                            throw new InvalidBlock(block, pos, "exterior");
                        } else {
                            break;
                        }
                    } else if (controller.exteriorValidator != null) {
                        // oh, so you dont give a fuck about the frame either, do you even care are the exterior
                        if (!controller.exteriorValidator.validate(block)) {
                            throw new InvalidBlock(block, pos, "exterior");
                        } else {
                            break;
                        }
                    }
                }
                default: {
                    if (extremes == 0) {
                        if (block instanceof IRectangularMultiblockBlock && controller.blockTypeValidator.validate(block)) {
                            if (!((IRectangularMultiblockBlock) block).isGoodForInterior()) {
                                throw new InvalidBlock(block, pos, "interior");
                            } else {
                                break;
                            }
                        } else if (controller.interiorValidator != null) {
                            // you must care about the inside, right?
                            if (!controller.interiorValidator.validate(block)) {
                                throw new InvalidBlock(block, pos, "interior");
                            } else {
                                break;
                            }
                        }
                    }
                    if (controller.genericValidator != null) {
                        // anything at all?
                        if (!controller.genericValidator.validate(block)) {
                            throw new InvalidBlock(block, pos, "generic");
                        } else {
                            break;
                        }
                    }
                    throw new InvalidBlock(block, pos, "generic");
                }
            }
            if (blockValidatedCallback != null) {
                blockValidatedCallback.accept(block);
            }
        });
        return true;
    };
    
    @Override
    protected final void setAssemblyValidator(@Nullable Validator<ControllerType> validator) {
        if (validator == null) {
            super.setAssemblyValidator(mainValidator);
            return;
        }
        super.setAssemblyValidator(Validator.and(mainValidator, validator));
    }
}
