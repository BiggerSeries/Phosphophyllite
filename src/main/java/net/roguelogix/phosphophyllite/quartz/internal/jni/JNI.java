package net.roguelogix.phosphophyllite.quartz.internal.jni;

import net.minecraft.util.ResourceLocation;
import sun.nio.ch.DirectBuffer;

import static net.roguelogix.phosphophyllite.util.Util.readBinaryResourceLocation;
import static net.roguelogix.phosphophyllite.util.Util.readTextResourceLocation;


/**
 * this may look unused, you would be wrong
 * used from C++ for accessing Java specific functions (im looking at you forge)
 */
public class JNI {
    public static String loadTextFile(String resourceString){
        ResourceLocation location = new ResourceLocation(resourceString);
        return readTextResourceLocation(location);
    }
    
    public static DirectBuffer loadBinaryFile(String resourceString){
        ResourceLocation location = new ResourceLocation(resourceString);
        return readBinaryResourceLocation(location);
    }
}
