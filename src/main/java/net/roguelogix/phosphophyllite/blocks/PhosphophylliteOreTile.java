package net.roguelogix.phosphophyllite.blocks;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.tileentity.TileEntityType;
import net.roguelogix.phosphophyllite.quartz.QuartzTESR;
import net.roguelogix.phosphophyllite.registry.RegisterTileEntity;
import net.roguelogix.phosphophyllite.registry.TileSupplier;
import net.roguelogix.phosphophyllite.tile.PhosphophylliteTile;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@RegisterTileEntity(name = "phosphophyllite_ore")
public class PhosphophylliteOreTile extends PhosphophylliteTile implements QuartzTESR.IQuartzTile {
    
    @RegisterTileEntity.Type
    private static TileEntityType<?> TYPE;
    
    @RegisterTileEntity.Supplier
    private static final TileSupplier SUPPLIER = PhosphophylliteOreTile::new;
    
    public PhosphophylliteOreTile() {
        super(TYPE);
    }
    
    @Override
    public ArrayList<Byte> getRenderInfo() {
        return null;
    }
    
    @Override
    public void setRenderInfo(ArrayList<Byte> info) {
    
    }
    
    @Override
    public boolean shouldRender() {
        return true;
    }
    
    @Override
    public QuartzTESR.Renderer createRenderer() {
        return new PhosphophylliteOreQuartzTESR();
    }
}
