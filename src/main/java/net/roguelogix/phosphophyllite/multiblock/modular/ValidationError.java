package net.roguelogix.phosphophyllite.multiblock.modular;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class ValidationError extends IllegalStateException {
    
    public ValidationError() {
        super();
    }
    
    public ValidationError(String s) {
        super(s);
    }
    
    public ValidationError(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ValidationError(Throwable cause) {
        super(cause);
    }
    
    Component cause;
    
    public ValidationError(Component cause) {
        super();
        this.cause = cause;
    }
    
    public Component getTextComponent() {
        if (cause != null) {
            return cause;
        }
        return new TranslatableComponent(getMessage());
    }
}
