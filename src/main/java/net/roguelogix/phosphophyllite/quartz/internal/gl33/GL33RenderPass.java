package net.roguelogix.phosphophyllite.quartz.internal.gl33;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.roguelogix.phosphophyllite.quartz.QuartzEvent;
import net.roguelogix.phosphophyllite.quartz.internal.common.GLDeletable;
import net.roguelogix.phosphophyllite.quartz.internal.common.ShaderInfo;

import javax.annotation.Nullable;

import static net.roguelogix.phosphophyllite.quartz.Quartz.EVENT_BUS;
import static org.lwjgl.opengl.GL33C.GL_LINE;
import static org.lwjgl.opengl.GL33C.GL_TRIANGLES;

public class GL33RenderPass implements GLDeletable {
    
    public final boolean QUAD;
    public final boolean TEXTURE;
    public final boolean LIGHTING;
    public final boolean ALPHA_DISCARD;
    
    public final int VERTICES_PER_PRIMITIVE;
    public final int GL_MODE;
    
    public final RenderType.CompositeRenderType renderType;
    
    private final ResourceLocation textureResourceLocation;
    private AbstractTexture texture;
    
    GL33RenderPass(RenderType rawRenderType) {
        if (!(rawRenderType instanceof RenderType.CompositeRenderType)) {
            throw new IllegalArgumentException("RenderType must be composite type");
        }
        this.renderType = (RenderType.CompositeRenderType) rawRenderType;
        
        var compositeState = renderType.state();
        var shaderInfo = ShaderInfo.get(compositeState.shaderState);
        if (shaderInfo == null) {
            throw new IllegalArgumentException("Unsupported RenderType shader" + rawRenderType);
        }
        
        QUAD = renderType.mode() == VertexFormat.Mode.QUADS;
        TEXTURE = compositeState.textureState != RenderStateShard.NO_TEXTURE;
        LIGHTING = compositeState.lightmapState != RenderStateShard.NO_LIGHTMAP;
        ALPHA_DISCARD = shaderInfo.ALPHA_DISCARD;
        
        if (TEXTURE && compositeState.textureState instanceof RenderStateShard.TextureStateShard texShard) {
            textureResourceLocation = texShard.cutoutTexture().orElse(null);
            if (textureResourceLocation == null) {
                throw new IllegalArgumentException("No Texture found for texture state shard");
            }
        } else {
            textureResourceLocation = null;
        }
        
        VERTICES_PER_PRIMITIVE = switch (renderType.mode()) {
            case LINES -> 2;
            case TRIANGLES -> 3;
            case QUADS -> 4;
            default -> throw new IllegalArgumentException("Unsupported primitive type");
        };
        GL_MODE = switch (renderType.mode()) {
            case LINES -> GL_LINE;
            case TRIANGLES, QUADS -> GL_TRIANGLES; // quads too, because element buffer
            default -> throw new IllegalArgumentException("Unsupported primitive type");
        };
        
        EVENT_BUS.register(this);
    }
    
    @SubscribeEvent
    public void reloadListener(QuartzEvent.ResourcesLoaded resourcesReloaded) {
        if (textureResourceLocation != null) {
            texture = Minecraft.getInstance().getTextureManager().getTexture(textureResourceLocation);
        }
    }
    
    @Override
    public void delete() {
        EVENT_BUS.unregister(this);
    }
    
    @Nullable
    public AbstractTexture texture(){
        return texture;
    }
}
