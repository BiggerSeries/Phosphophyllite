package net.roguelogix.phosphophyllite.registry;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)

public @interface RegisterTileEntity {
    
    String modid() default "";
    
    String name();
    
    @Retention(RetentionPolicy.RUNTIME)
    @interface Type {
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @interface Renderer {
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @interface Supplier {
    }
}
