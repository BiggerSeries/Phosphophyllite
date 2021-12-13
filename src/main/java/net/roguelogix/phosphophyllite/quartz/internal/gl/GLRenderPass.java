package net.roguelogix.phosphophyllite.quartz.internal.gl;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.roguelogix.phosphophyllite.quartz.internal.common.ShaderInfo;

import javax.annotation.Nullable;

import static org.lwjgl.opengl.GL32C.*;

public class GLRenderPass {
    
    public final RenderType renderType;
    
    public final boolean QUAD;
    public final boolean TEXTURE;
    public final boolean LIGHTING;
    public final boolean ALPHA_DISCARD;
    
    public final int VERTICES_PER_PRIMITIVE;
    public final int GL_MODE;
    
    private final ResourceLocation textureResourceLocation;
    private AbstractTexture texture;
    
    GLRenderPass(RenderType rawRenderType) {
        this.renderType = rawRenderType;
        if (!(rawRenderType instanceof RenderType.CompositeRenderType renderType)) {
            throw new IllegalArgumentException("RenderType must be composite type");
        }
        
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
        
        resourceReload();
    }
    
    @Nullable
    public AbstractTexture texture() {
        return texture;
    }
    
    public void resourceReload() {
        texture = Minecraft.getInstance().getTextureManager().getTexture(textureResourceLocation);
    }
}