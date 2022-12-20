package net.roguelogix.phosphophyllite.debug;

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
                var debugInfo = debuggable.getDebugInfo();
                if (debugInfo == null) {
                    var toPrint = "Null debug info returned on " + (context.getLevel().isClientSide() ? " Client" : " Server");
                    player.sendSystemMessage(Component.literal("Null debug info returned"));
                    System.out.println();
                    return InteractionResult.SUCCESS;
                }
                var toPrint = new DebugInfo(debugInfo.name() + (context.getLevel().isClientSide() ? " (Client)" : " (Server)"), debugInfo).toString();
                player.sendSystemMessage(Component.literal(toPrint));
                System.out.println(toPrint);
            } else {
                player.sendSystemMessage(Component.literal("Non-debuggable block"));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
