package net.roguelogix.phosphophyllite.quartz.internal.common;

import net.minecraft.client.renderer.RenderStateShard;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static net.minecraft.client.renderer.RenderStateShard.*;
import static net.minecraft.client.renderer.RenderStateShard.RENDERTYPE_ENTITY_CUTOUT_SHADER;

public class ShaderInfo {
    
    private static final Map<RenderStateShard.ShaderStateShard, ShaderInfo> infos = new HashMap<>();
    
    @Nullable
    public static ShaderInfo get(RenderStateShard.ShaderStateShard shaderSateShard) {
        return infos.get(shaderSateShard);
    }
    
    static {
        // TODO: rest of the default shaders
        infos.put(RENDERTYPE_SOLID_SHADER, new ShaderInfo(false));
        infos.put(RENDERTYPE_CUTOUT_MIPPED_SHADER, new ShaderInfo(true));
        infos.put(RENDERTYPE_CUTOUT_SHADER, new ShaderInfo(true));
        infos.put(RENDERTYPE_ENTITY_CUTOUT_SHADER, new ShaderInfo(true));
        // TODO: ability to register a custom shader
    }
    
    public ShaderInfo(boolean alpha_discard) {
        ALPHA_DISCARD = alpha_discard;
    }
    
    public final boolean ALPHA_DISCARD;
}
