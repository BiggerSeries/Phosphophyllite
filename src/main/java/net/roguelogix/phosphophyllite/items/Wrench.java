package net.roguelogix.phosphophyllite.items;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.roguelogix.phosphophyllite.registry.RegisterItem;

import javax.annotation.Nonnull;

public class Wrench extends Item{
    @RegisterItem(name = "wrench")
    public static final Wrench INSTANCE = new Wrench(new Item.Properties());
    
    @SuppressWarnings("unused")
    public Wrench(@Nonnull Item.Properties properties) {
        super(properties.stacksTo(1));
    }
    
    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return true;
    }
}
