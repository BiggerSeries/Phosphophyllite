package net.roguelogix.phosphophyllite.registry;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RegisterBlock {
    
    String modid() default "";
    
    String name();
    
    boolean registerItem() default true;
    
    boolean creativeTab() default true;
    
    Class<?> tileEntityClass() default RegisterBlock.class;
    
    @Retention(RetentionPolicy.RUNTIME)
    @interface Instance {
    
    }
    
    @Retention(RetentionPolicy.RUNTIME)
    @interface RenderLayer {
    }
}
