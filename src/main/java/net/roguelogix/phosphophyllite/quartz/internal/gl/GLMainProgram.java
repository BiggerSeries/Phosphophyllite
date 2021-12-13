package net.roguelogix.phosphophyllite.quartz.internal.gl;

import net.minecraft.resources.ResourceLocation;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.quartz.internal.QuartzCore;
import net.roguelogix.phosphophyllite.quartz.internal.common.DrawInfo;
import net.roguelogix.phosphophyllite.util.Util;

import static net.roguelogix.phosphophyllite.quartz.internal.MagicNumbers.GL.*;
import static org.lwjgl.opengl.ARBSeparateShaderObjects.*;
import static org.lwjgl.opengl.GL32C.*;

public class GLMainProgram {
    
    public final ResourceLocation baseResourceLocation = new ResourceLocation(Phosphophyllite.modid, "quartz/shaders/gl/main");
    private final ResourceLocation vertexShaderLocation = new ResourceLocation(baseResourceLocation.getNamespace(), baseResourceLocation.getPath() + ".vert");
    private final ResourceLocation fragmentShaderLocation = new ResourceLocation(baseResourceLocation.getNamespace(), baseResourceLocation.getPath() + ".frag");
    
    private static class Info {
        private int vertexShader = 0;
        private int opaqueFragmentShader = 0;
        private int cutoutFragmentShader = 0;
        
        private int opaquePipeline = 0;
        private int cutoutPipeline = 0;
        
        void clean() {
            glDeleteProgram(vertexShader);
            glDeleteProgram(opaqueFragmentShader);
            glDeleteProgram(cutoutFragmentShader);
            glDeleteProgramPipelines(opaquePipeline);
            glDeleteProgramPipelines(cutoutPipeline);
        }
    }
    
    private final Info info = new Info();
    
    public GLMainProgram() {
        Info info = this.info;
        QuartzCore.CLEANER.register(this, () -> GLCore.deletionQueue.enqueue(info::clean));
    }
    
    public void reload() {
        int vertexProgram = 0;
        int opaqueFragmentProgram = 0;
        int cutoutFragmentProgram = 0;
        
        try {
            var vertexShaderCode = Util.readResourceLocation(vertexShaderLocation);
            var opaqueFragShaderCode = Util.readResourceLocation(fragmentShaderLocation);
            if (vertexShaderCode == null || opaqueFragShaderCode == null) {
                throw new IllegalStateException("Failed to load shader code for " + baseResourceLocation);
            }
            
            String vertPrepend =
                    "#define POSITION_LOCATION " + POSITION_LOCATION + "\n" +
                            "#define COLOR_LOCATION " + COLOR_LOCATION + "\n" +
                            "#define TEX_COORD_LOCATION " + TEX_COORD_LOCATION + "\n" +
                            "#define LIGHTINFO_LOCATION " + LIGHTINFO_LOCATION + "\n" +
                            "#define WORLD_POSITION_LOCATION " + WORLD_POSITION_LOCATION + "\n" +
                            "#define DYNAMIC_MATRIX_ID_LOCATION " + DYNAMIC_MATRIX_ID_LOCATION + "\n" +
                            "#define DYNAMIC_LIGHT_ID_LOCATION " + DYNAMIC_LIGHT_ID_LOCATION + "\n" +
                            "#define STATIC_MATRIX_LOCATION " + STATIC_MATRIX_LOCATION + "\n" +
                            "#define STATIC_NORMAL_MATRIX_LOCATION " + STATIC_NORMAL_MATRIX_LOCATION + "\n" +
                            "";
            vertexShaderCode = new StringBuilder(vertexShaderCode).insert(vertexShaderCode.indexOf('\n') + 1, vertPrepend).toString();
            var cutoutFragShaderCode = new StringBuilder(opaqueFragShaderCode).insert(vertexShaderCode.indexOf('\n') + 1, "#define ALPHA_DISCARD\n").toString();
            
            vertexProgram = glCreateShaderProgramv(GL_VERTEX_SHADER, vertexShaderCode);
            opaqueFragmentProgram = glCreateShaderProgramv(GL_FRAGMENT_SHADER, opaqueFragShaderCode);
            cutoutFragmentProgram = glCreateShaderProgramv(GL_FRAGMENT_SHADER, cutoutFragShaderCode);
            
            int vertLinked = glGetProgrami(vertexProgram, GL_LINK_STATUS);
            int opaqueFragLinked = glGetProgrami(opaqueFragmentProgram, GL_LINK_STATUS);
            int cutoutFragLinked = glGetProgrami(cutoutFragmentProgram, GL_LINK_STATUS);
            if (vertLinked != GL_TRUE || opaqueFragLinked != GL_TRUE || cutoutFragLinked != GL_TRUE) {
                var error = new StringBuilder();
                if (vertLinked != GL_TRUE) {
                    error.append("Vertex shader compilation failed for ").append(vertexShaderLocation).append('\n').append(glGetProgramInfoLog(vertexProgram)).append('\n');
                }
                if (opaqueFragmentProgram != GL_TRUE) {
                    error.append("Opaque fragment shader compilation failed for ").append(fragmentShaderLocation).append('\n').append(glGetProgramInfoLog(opaqueFragmentProgram)).append('\n');
                }
                if (cutoutFragmentProgram != GL_TRUE) {
                    error.append("Cutout fragment shader compilation failed for ").append(fragmentShaderLocation).append('\n').append(glGetProgramInfoLog(cutoutFragmentProgram)).append('\n');
                }
                throw new IllegalStateException(error.toString());
            }
            
            glDeleteProgramPipelines(info.opaquePipeline);
            glDeleteProgramPipelines(info.cutoutPipeline);
            
            info.opaquePipeline = glGenProgramPipelines();
            glUseProgramStages(info.opaquePipeline, GL_VERTEX_SHADER_BIT, vertexProgram);
            glUseProgramStages(info.opaquePipeline, GL_FRAGMENT_SHADER_BIT, opaqueFragmentProgram);
            
            info.cutoutPipeline = glGenProgramPipelines();
            glUseProgramStages(info.cutoutPipeline, GL_VERTEX_SHADER_BIT, vertexProgram);
            glUseProgramStages(info.cutoutPipeline, GL_FRAGMENT_SHADER_BIT, cutoutFragmentProgram);
            
            info.vertexShader ^= vertexProgram;
            vertexProgram ^= info.vertexShader;
            info.vertexShader ^= vertexProgram;
            
            info.opaqueFragmentShader ^= opaqueFragmentProgram;
            opaqueFragmentProgram ^= info.opaqueFragmentShader;
            info.opaqueFragmentShader ^= opaqueFragmentProgram;
            
            info.cutoutFragmentShader ^= cutoutFragmentProgram;
            cutoutFragmentProgram ^= info.cutoutFragmentShader;
            info.cutoutFragmentShader ^= cutoutFragmentProgram;
            
            onReloaded();
        } finally {
            glDeleteProgram(vertexProgram);
            glDeleteProgram(opaqueFragmentProgram);
            glDeleteProgram(cutoutFragmentProgram);
        }
    }
    
    private int PLAYER_BLOCK_UNIFORM_LOCATION;
    private int PLAYER_SUB_BLOCK_UNIFORM_LOCATION;
    private int PROJECTION_MATRIX_UNIFORM_LOCATION;
    private int VERT_QUAD_UNIFORM_LOCATION;
    private int VERT_LIGHTING_UNIFORM_LOCATION;
    private int DYNAMIC_MATRICES_UNIFORM_LOCATION;
    private int DYNAMIC_LIGHTS_UNIFORM_LOCATION;
    
    public int OPAQUE_FOG_START_END_UNIFORM_LOCATION;
    public int OPAQUE_FOG_COLOR_UNIFORM_LOCATION;
    public int OPAQUE_QUAD_UNIFORM_LOCATION;
    public int OPAQUE_LIGHTING_UNIFORM_LOCATION;
    public int OPAQUE_TEXTURE_UNIFORM_LOCATION;
    public int OPAQUE_ATLAS_TEXTURE_UNIFORM_LOCATION;
    public int OPAQUE_LIGHTMAP_TEXTURE_UNIFORM_LOCATION;
    
    public int CUTOUT_FOG_START_END_UNIFORM_LOCATION;
    public int CUTOUT_FOG_COLOR_UNIFORM_LOCATION;
    public int CUTOUT_QUAD_UNIFORM_LOCATION;
    public int CUTOUT_LIGHTING_UNIFORM_LOCATION;
    public int CUTOUT_TEXTURE_UNIFORM_LOCATION;
    public int CUTOUT_ATLAS_TEXTURE_UNIFORM_LOCATION;
    public int CUTOUT_LIGHTMAP_TEXTURE_UNIFORM_LOCATION;
    
    private void onReloaded() {
        PLAYER_BLOCK_UNIFORM_LOCATION = glGetUniformLocation(info.vertexShader, "playerBlock");
        PLAYER_SUB_BLOCK_UNIFORM_LOCATION = glGetUniformLocation(info.vertexShader, "playerSubBlock");
        PROJECTION_MATRIX_UNIFORM_LOCATION = glGetUniformLocation(info.vertexShader, "projectionMatrix");
        VERT_QUAD_UNIFORM_LOCATION = glGetUniformLocation(info.vertexShader, "QUAD");
        VERT_LIGHTING_UNIFORM_LOCATION = glGetUniformLocation(info.vertexShader, "LIGHTING");
        DYNAMIC_MATRICES_UNIFORM_LOCATION = glGetUniformLocation(info.vertexShader, "dynamicMatrices");
        DYNAMIC_LIGHTS_UNIFORM_LOCATION = glGetUniformLocation(info.vertexShader, "dynamicLights");
        
        OPAQUE_FOG_START_END_UNIFORM_LOCATION = glGetUniformLocation(info.opaqueFragmentShader, "fogStartEnd");
        OPAQUE_FOG_COLOR_UNIFORM_LOCATION = glGetUniformLocation(info.opaqueFragmentShader, "fogColor");
        OPAQUE_QUAD_UNIFORM_LOCATION = glGetUniformLocation(info.opaqueFragmentShader, "QUAD");
        OPAQUE_LIGHTING_UNIFORM_LOCATION = glGetUniformLocation(info.opaqueFragmentShader, "LIGHTING");
        OPAQUE_TEXTURE_UNIFORM_LOCATION = glGetUniformLocation(info.opaqueFragmentShader, "TEXTURE");
        OPAQUE_ATLAS_TEXTURE_UNIFORM_LOCATION = glGetUniformLocation(info.opaqueFragmentShader, "atlasTexture");
        OPAQUE_LIGHTMAP_TEXTURE_UNIFORM_LOCATION = glGetUniformLocation(info.opaqueFragmentShader, "lightmapTexture");
        
        CUTOUT_FOG_START_END_UNIFORM_LOCATION = glGetUniformLocation(info.cutoutFragmentShader, "fogStartEnd");
        CUTOUT_FOG_COLOR_UNIFORM_LOCATION = glGetUniformLocation(info.cutoutFragmentShader, "fogColor");
        CUTOUT_QUAD_UNIFORM_LOCATION = glGetUniformLocation(info.cutoutFragmentShader, "QUAD");
        CUTOUT_LIGHTING_UNIFORM_LOCATION = glGetUniformLocation(info.cutoutFragmentShader, "LIGHTING");
        CUTOUT_TEXTURE_UNIFORM_LOCATION = glGetUniformLocation(info.cutoutFragmentShader, "TEXTURE");
        CUTOUT_ATLAS_TEXTURE_UNIFORM_LOCATION = glGetUniformLocation(info.cutoutFragmentShader, "atlasTexture");
        CUTOUT_LIGHTMAP_TEXTURE_UNIFORM_LOCATION = glGetUniformLocation(info.cutoutFragmentShader, "lightmapTexture");
        
        glProgramUniform1i(info.vertexShader, DYNAMIC_MATRICES_UNIFORM_LOCATION, DYNAMIC_MATRIX_TEXTURE_UNIT);
        glProgramUniform1i(info.vertexShader, DYNAMIC_LIGHTS_UNIFORM_LOCATION, DYNAMIC_LIGHT_TEXTURE_UNIT);
        
        glProgramUniform1i(info.opaqueFragmentShader, OPAQUE_ATLAS_TEXTURE_UNIFORM_LOCATION, ATLAS_TEXTURE_UNIT);
        glProgramUniform1i(info.opaqueFragmentShader, OPAQUE_LIGHTMAP_TEXTURE_UNIFORM_LOCATION, LIGHTMAP_TEXTURE_UNIT);
        
        glProgramUniform1i(info.cutoutFragmentShader, CUTOUT_ATLAS_TEXTURE_UNIFORM_LOCATION, ATLAS_TEXTURE_UNIT);
        glProgramUniform1i(info.cutoutFragmentShader, CUTOUT_LIGHTMAP_TEXTURE_UNIFORM_LOCATION, LIGHTMAP_TEXTURE_UNIT);
    }
    
    public void setupDrawInfo(DrawInfo drawInfo) {
        resetBinds();
        glProgramUniformMatrix4fv(info.vertexShader, PROJECTION_MATRIX_UNIFORM_LOCATION, false, drawInfo.projectionMatrixFloatBuffer);
        glProgramUniform3i(info.vertexShader, PLAYER_BLOCK_UNIFORM_LOCATION, drawInfo.playerPosition.x, drawInfo.playerPosition.y, drawInfo.playerPosition.z);
        glProgramUniform3f(info.vertexShader, PLAYER_SUB_BLOCK_UNIFORM_LOCATION, drawInfo.playerSubBlock.x, drawInfo.playerSubBlock.y, drawInfo.playerSubBlock.z);
        
        glProgramUniform2f(info.opaqueFragmentShader, OPAQUE_FOG_START_END_UNIFORM_LOCATION, drawInfo.fogStart, drawInfo.fogEnd);
        glProgramUniform4f(info.opaqueFragmentShader, OPAQUE_FOG_COLOR_UNIFORM_LOCATION, drawInfo.fogColor.x, drawInfo.fogColor.y, drawInfo.fogColor.z, 1);
        
        glProgramUniform2f(info.cutoutFragmentShader, CUTOUT_FOG_START_END_UNIFORM_LOCATION, drawInfo.fogStart, drawInfo.fogEnd);
        glProgramUniform4f(info.cutoutFragmentShader, CUTOUT_FOG_COLOR_UNIFORM_LOCATION, drawInfo.fogColor.x, drawInfo.fogColor.y, drawInfo.fogColor.z, 1);
    }
    
    private int currentAtlas = 0;
    private int currentPipeline = 0;
    
    public void setupRenderPass(GLRenderPass renderPass) {
        glProgramUniform1i(info.vertexShader, VERT_QUAD_UNIFORM_LOCATION, renderPass.QUAD ? GL_TRUE : GL_FALSE);
        glProgramUniform1i(info.vertexShader, VERT_LIGHTING_UNIFORM_LOCATION, renderPass.LIGHTING ? GL_TRUE : GL_FALSE);
        
        final var OPAQUE = !renderPass.ALPHA_DISCARD;
        glProgramUniform1i(OPAQUE ? info.opaqueFragmentShader : info.cutoutFragmentShader, OPAQUE ? OPAQUE_QUAD_UNIFORM_LOCATION : CUTOUT_QUAD_UNIFORM_LOCATION, renderPass.QUAD ? GL_TRUE : GL_FALSE);
        glProgramUniform1i(OPAQUE ? info.opaqueFragmentShader : info.cutoutFragmentShader, OPAQUE ? OPAQUE_LIGHTING_UNIFORM_LOCATION : CUTOUT_LIGHTING_UNIFORM_LOCATION, renderPass.LIGHTING ? GL_TRUE : GL_FALSE);
        glProgramUniform1i(OPAQUE ? info.opaqueFragmentShader : info.cutoutFragmentShader, OPAQUE ? OPAQUE_TEXTURE_UNIFORM_LOCATION : CUTOUT_TEXTURE_UNIFORM_LOCATION, renderPass.TEXTURE ? GL_TRUE : GL_FALSE);
        
        final var pipeline = OPAQUE ? info.opaquePipeline : info.cutoutPipeline;
        if (pipeline != currentPipeline) {
            currentAtlas = 0;
            glBindProgramPipeline(pipeline);
            currentPipeline = pipeline;
        }
        
        var texture = renderPass.texture();
        if (texture != null) {
            int id = texture.getId();
            if (id != currentAtlas) {
                glBindTexture(GL_TEXTURE_2D, id);
                currentAtlas = id;
            }
        }
    }
    
    public void resetBinds() {
        currentPipeline = 0;
        glBindProgramPipeline(0);
    }
}
