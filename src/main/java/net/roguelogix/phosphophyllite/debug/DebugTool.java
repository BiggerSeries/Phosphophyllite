package net.roguelogix.phosphophyllite.debug;

import net.minecraft.Util;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.roguelogix.phosphophyllite.registry.RegisterItem;

@RegisterItem(name = "debug_tool")
public class DebugTool extends Item {
    
    @RegisterItem.Instance
    public static DebugTool INSTANCE;
    
    public DebugTool(Properties properties) {
        super(properties.stacksTo(1));
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
                player.sendMessage(new TextComponent(debugString), Util.NIL_UUID);
                System.out.println(debugString);
            } else {
                player.sendMessage(new TextComponent("Non-debuggable block"), Util.NIL_UUID);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
