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
        if (!context.getLevel().isClientSide()) {
            if (context.getLevel().getBlockEntity(context.getClickedPos()) instanceof IDebuggable debuggable) {
                var player = context.getPlayer();
                if (player != null) {
                    var debugString = debuggable.getDebugString();
                    if (debugString == null) {
                        debugString = "Null string returned";
                    }
                    player.sendMessage(new TextComponent(debugString), Util.NIL_UUID);
                    System.out.println(debugString);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }
}
