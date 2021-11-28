package net.roguelogix.phosphophyllite.quartz.internal.gl33;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.roguelogix.phosphophyllite.quartz.QuartzEvent;
import net.roguelogix.phosphophyllite.quartz.internal.QuartzCore;
import net.roguelogix.phosphophyllite.quartz.internal.common.GLDeletable;
import net.roguelogix.phosphophyllite.quartz.internal.common.ShaderInfo;

import static net.roguelogix.phosphophyllite.quartz.Quartz.EVENT_BUS;
import static org.lwjgl.opengl.GL33C.*;

public class GL33RenderPass implements GLDeletable {
    
    public final boolean QUAD;
    public final boolean TEXTURE;
    public final boolean LIGHTING;
    public final boolean ALPHA_DISCARD;
    
    public final int VERTICES_PER_PRIMITIVE;
    public final int GL_MODE;
    
    public final RenderType.CompositeRenderType renderType;
    public final GL33MainProgram program;
    
    private final ResourceLocation textureResourceLocation;
    private AbstractTexture texture;
    
    GL33RenderPass(RenderType rawRenderType, GL33MainProgram program) {
        this.program = program;
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
        try {
            program.reload();
        } catch (IllegalStateException e) {
            QuartzCore.LOGGER.warn("Failed to reload shader " + program.baseResourceLocation);
        }
    }
    
    @Override
    public void delete() {
        EVENT_BUS.unregister(this);
        program.delete();
    }
    
    void setUniforms() {
        glUniform1i(program.ATLAS_TEXTURE_UNIFORM_LOCATION, 0);
        glUniform1i(program.QUAD_UNIFORM_LOCATION, QUAD ? GL_TRUE : GL_FALSE);
        glUniform1i(program.TEXTURE_UNIFORM_LOCATION, TEXTURE ? GL_TRUE : GL_FALSE);
        glUniform1i(program.LIGHTING_UNIFORM_LOCATION, LIGHTING ? GL_TRUE : GL_FALSE);
        glUniform1i(program.ALPHA_DISCARD_UNIFORM_LOCATION, ALPHA_DISCARD ? GL_TRUE : GL_FALSE);
        if (texture != null) {
            program.setAtlas(texture.getId());
        }
    }
}
