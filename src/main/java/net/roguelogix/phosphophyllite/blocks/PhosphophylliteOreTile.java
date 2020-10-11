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
    public void onLoad() {
        if(this.getPos().getX() % 2 == 0){
            this.getQuartzState().with("statetest", "true");
        }
        super.onLoad();
    }
    
    @Override
    public void tick() {

    }
}
