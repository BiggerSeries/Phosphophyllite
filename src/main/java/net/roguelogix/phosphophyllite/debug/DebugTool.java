package net.roguelogix.phosphophyllite.debug;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.roguelogix.phosphophyllite.registry.RegisterItem;

public class DebugTool extends Item {
    
    @RegisterItem(name = "debug_tool")
    public static final DebugTool INSTANCE = new DebugTool();
    
    public DebugTool() {
        super(new Item.Properties().stacksTo(1));
    }
    
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        var player = context.getPlayer();
        if (player != null) {
            if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof IDebuggable debuggable) {
                
                var debugString = debuggable.getDebugString();
                if (debugString == null) {
                    debugString = "Null debug string returned";
                }
                if (context.getLevel().isClientSide()) {
                    debugString = "\nClient:\n" + debugString;
                }else {
                    debugString = "\nServer:\n" + debugString;
                }
                player.sendSystemMessage(Component.literal(debugString));
                System.out.println(debugString);
            } else {
                player.sendSystemMessage(Component.literal("Non-debuggable block"));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
