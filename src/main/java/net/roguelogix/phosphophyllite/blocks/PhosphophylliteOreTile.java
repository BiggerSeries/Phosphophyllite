package net.roguelogix.phosphophyllite.blocks;

import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.roguelogix.phosphophyllite.quartz.api.QuartzTile;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;

@RegisterTileEntity(name = "phosphophyllite_ore")
public class PhosphophylliteOreTile extends QuartzTile implements ITickableTileEntity {
    
    @RegisterTileEntity.Type
    public static TileEntityType<?> TYPE;
    
    public PhosphophylliteOreTile() {
        super(TYPE);
    }
    
    @Override
    public void tick() {

    }
}
