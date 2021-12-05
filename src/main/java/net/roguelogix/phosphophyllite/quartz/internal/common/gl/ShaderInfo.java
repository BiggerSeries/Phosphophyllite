package net.roguelogix.phosphophyllite.quartz.internal.common.gl;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;

import static net.minecraft.client.renderer.RenderStateShard.*;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ShaderInfo {
    
    private static final Map<ShaderStateShard, ShaderInfo> infos = new HashMap<>();
    
    @Nullable
    public static ShaderInfo get(ShaderStateShard shaderSateShard){
        return infos.get(shaderSateShard);
    }
    
    static {
        infos.put(RENDERTYPE_SOLID_SHADER, new ShaderInfo(false));
        infos.put(RENDERTYPE_CUTOUT_MIPPED_SHADER, new ShaderInfo(true));
        infos.put(RENDERTYPE_CUTOUT_SHADER, new ShaderInfo(true));
        infos.put(RENDERTYPE_ENTITY_CUTOUT_SHADER, new ShaderInfo(true));
    }
    
    public ShaderInfo(boolean alpha_discard) {
        ALPHA_DISCARD = alpha_discard;
    }
    
    public final boolean ALPHA_DISCARD;
}
