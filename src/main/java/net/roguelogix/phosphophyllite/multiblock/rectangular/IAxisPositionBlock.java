package net.roguelogix.phosphophyllite.multiblock.rectangular;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.roguelogix.phosphophyllite.modular.api.BlockModule;
import net.roguelogix.phosphophyllite.modular.api.IModularBlock;
import net.roguelogix.phosphophyllite.modular.api.ModuleRegistry;
import net.roguelogix.phosphophyllite.registry.OnModLoad;
import net.roguelogix.phosphophyllite.util.NonnullDefault;

import java.util.Locale;

@NonnullDefault
public interface IAxisPositionBlock extends IModularBlock {
    enum AxisPosition implements StringRepresentable {
        LOWER("lower"),
        MIDDLE("middle"),
        UPPER("upper");
        
        private final String name;
        
        AxisPosition(String name) {
            this.name = name;
        }
        
        @Override
        public String getSerializedName() {
            return toString().toLowerCase(Locale.US);
        }
        
        @Override
        public String toString() {
            return name;
        }
        
        public static final EnumProperty<AxisPosition> X_AXIS_POSITION = EnumProperty.create("x_axis", AxisPosition.class);
        public static final EnumProperty<AxisPosition> Y_AXIS_POSITION = EnumProperty.create("y_axis", AxisPosition.class);
        public static final EnumProperty<AxisPosition> Z_AXIS_POSITION = EnumProperty.create("z_axis", AxisPosition.class);
        
    }
    
    final class Module extends BlockModule<IAxisPositionBlock> {
        
        public Module(IAxisPositionBlock iface) {
            super(iface);
        }
        
        @Override
        public void buildStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(AxisPosition.X_AXIS_POSITION);
            builder.add(AxisPosition.Y_AXIS_POSITION);
            builder.add(AxisPosition.Z_AXIS_POSITION);
        }
        
        @Override
        public BlockState buildDefaultState(BlockState state) {
            state = state.setValue(AxisPosition.X_AXIS_POSITION, AxisPosition.MIDDLE);
            state = state.setValue(AxisPosition.Y_AXIS_POSITION, AxisPosition.MIDDLE);
            state = state.setValue(AxisPosition.Z_AXIS_POSITION, AxisPosition.MIDDLE);
            return state;
        }
        
        @OnModLoad
        static void onModLoad() {
            ModuleRegistry.registerBlockModule(IAxisPositionBlock.class, Module::new);
        }
    }
}
