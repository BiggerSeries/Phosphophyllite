package net.roguelogix.phosphophyllite.quartz.internal.gl;

import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.roguelogix.phosphophyllite.quartz.internal.common.ShaderInfo;
import net.roguelogix.phosphophyllite.util.MethodsReturnNonnullByDefault;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static org.lwjgl.opengl.GL32C.GL_LINE;
import static org.lwjgl.opengl.GL32C.GL_TRIANGLES;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GLRenderPass {
    
    public final boolean QUAD;
    public final boolean TEXTURE;
    public final boolean LIGHTING;
    public final boolean ALPHA_DISCARD;
    
    public final int VERTICES_PER_PRIMITIVE;
    public final int GL_MODE;
    
    private final ResourceLocation textureResourceLocation;
    private AbstractTexture texture;
    
    private static final Object2ObjectOpenHashMap<RenderType, GLRenderPass> renderPasses = new Object2ObjectOpenHashMap<>();
    
    public static void resourcesReloaded(){
        for (GLRenderPass value : renderPasses.values()) {
            value.resourceReload();
        }
    }
    
    public static GLRenderPass renderPassForRenderType(RenderType renderType) {
        return renderPasses.computeIfAbsent(renderType, (RenderType type) -> {
            final var renderPass = new GLRenderPass(renderType);
            for (final var potentialPass : renderPasses.values()) {
                if(potentialPass.compatible(renderPass)){
                    return potentialPass;
                }
            }
            return renderPass;
        });
    }
    
    private GLRenderPass(RenderType rawRenderType) {
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
    
    public boolean compatible(GLRenderPass otherPass) {
        return QUAD == otherPass.QUAD &&
                TEXTURE == otherPass.TEXTURE &&
                LIGHTING == otherPass.LIGHTING &&
                ALPHA_DISCARD == otherPass.ALPHA_DISCARD &&
                VERTICES_PER_PRIMITIVE == otherPass.VERTICES_PER_PRIMITIVE &&
                GL_MODE == otherPass.GL_MODE &&
                textureResourceLocation.equals(otherPass.textureResourceLocation);
    }
    
    @Nullable
    public AbstractTexture texture() {
        return texture;
    }
    
    private void resourceReload() {
        texture = Minecraft.getInstance().getTextureManager().getTexture(textureResourceLocation);
    }
}