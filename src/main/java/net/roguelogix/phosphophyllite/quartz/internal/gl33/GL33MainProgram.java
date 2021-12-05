package net.roguelogix.phosphophyllite.quartz.internal.gl33;

import net.minecraft.resources.ResourceLocation;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.quartz.internal.common.DrawInfo;
import net.roguelogix.phosphophyllite.quartz.internal.common.gl.Program;

import static net.roguelogix.phosphophyllite.quartz.internal.common.MagicNumbers.*;
import static org.lwjgl.opengl.GL33C.*;
import static org.lwjgl.opengl.ARBSeparateShaderObjects.*;

public class GL33MainProgram extends Program {
    public GL33MainProgram() {
        super(new ResourceLocation(Phosphophyllite.modid, "quartz/shaders/gl33/renderpass"),
                "#define POSITION_LOCATION " + POSITION_LOCATION + "\n" +
                        "#define COLOR_LOCATION " + COLOR_LOCATION + "\n" +
                        "#define TEX_COORD_LOCATION " + TEX_COORD_LOCATION + "\n" +
                        "#define LIGHTINFO_LOCATION " + LIGHTINFO_LOCATION + "\n");
        addReloadCallback(this::onReload);
    }
    
    public int PLAYER_BLOCK_UNIFORM_LOCATION;
    public int PLAYER_SUB_BLOCK_UNIFORM_LOCATION;
    public int PROJECTION_MATRIX_UNIFORM_LOCATION;
    
    public int WORLD_POSITION_ID_OFFSET_UNIFORM_LOCATION;
    public int WORLD_POSITIONS_UNIFORM_LOCATION;
    
    public int DYNAMIC_MATRIX_ID_OFFSET_UNIFORM_LOCATION;
    public int DYNAMIC_MATRIX_IDS_UNIFORM_LOCATION;
    public int DYNAMIC_MATRICES_UNIFORM_LOCATION;
    
    public int STATIC_MATRIX_BASE_ID_UNIFORM_LOCATION;
    public int STATIC_MATRICES_UNIFORM_LOCATION;
    
    public int DYNAMIC_LIGHT_ID_OFFSET_UNIFORM_LOCATION;
    public int DYNAMIC_LIGHT_IDS_UNIFORM_LOCATION;
    public int DYNAMIC_LIGHTS_UNIFORM_LOCATION;
    
    public int FOG_START_END_UNIFORM_LOCATION;
    public int FOG_COLOR_UNIFORM_LOCATION;
    public int ATLAS_TEXTURE_UNIFORM_LOCATION;
    public int LIGHTMAP_TEXTURE_UNIFORM_LOCATION;
    
    public int VERT_QUAD_UNIFORM_LOCATION;
    public int FRAG_QUAD_UNIFORM_LOCATION;
    public int VERT_LIGHTING_UNIFORM_LOCATION;
    public int FRAG_LIGHTING_UNIFORM_LOCATION;
    public int TEXTURE_UNIFORM_LOCATION;
    public int ALPHA_DISCARD_UNIFORM_LOCATION;
    
    private void onReload(Program program) {
        PLAYER_BLOCK_UNIFORM_LOCATION = vertUniformLocation("playerBlock");
        PLAYER_SUB_BLOCK_UNIFORM_LOCATION = vertUniformLocation("playerSubBlock");
        PROJECTION_MATRIX_UNIFORM_LOCATION = vertUniformLocation("projectionMatrix");
        
        WORLD_POSITION_ID_OFFSET_UNIFORM_LOCATION = vertUniformLocation("worldPositionIDOffset");
        WORLD_POSITIONS_UNIFORM_LOCATION = vertUniformLocation("worldPositions");
        
        DYNAMIC_MATRIX_ID_OFFSET_UNIFORM_LOCATION = vertUniformLocation("dynamicMatrixIDOffset");
        DYNAMIC_MATRIX_IDS_UNIFORM_LOCATION = vertUniformLocation("dynamicMatrixIDs");
        DYNAMIC_MATRICES_UNIFORM_LOCATION = vertUniformLocation("dynamicMatrices");
        
        STATIC_MATRIX_BASE_ID_UNIFORM_LOCATION = vertUniformLocation("staticMatrixBaseID");
        STATIC_MATRICES_UNIFORM_LOCATION = vertUniformLocation("staticMatrices");
        
        DYNAMIC_LIGHT_ID_OFFSET_UNIFORM_LOCATION = vertUniformLocation("lightIDOffset");
        DYNAMIC_LIGHT_IDS_UNIFORM_LOCATION = vertUniformLocation("lightIDs");
        DYNAMIC_LIGHTS_UNIFORM_LOCATION = vertUniformLocation("lights");
        
        FOG_START_END_UNIFORM_LOCATION = fragUniformLocation("fogStartEnd");
        FOG_COLOR_UNIFORM_LOCATION = fragUniformLocation("fogColor");
        ATLAS_TEXTURE_UNIFORM_LOCATION = fragUniformLocation("atlasTexture");
        LIGHTMAP_TEXTURE_UNIFORM_LOCATION = fragUniformLocation("lightmapTexture");
        
        VERT_QUAD_UNIFORM_LOCATION = vertUniformLocation("QUAD");
        VERT_LIGHTING_UNIFORM_LOCATION = vertUniformLocation("LIGHTING");
        FRAG_QUAD_UNIFORM_LOCATION = fragUniformLocation("QUAD");
        FRAG_LIGHTING_UNIFORM_LOCATION = fragUniformLocation("LIGHTING");
        TEXTURE_UNIFORM_LOCATION = fragUniformLocation("TEXTURE");
        ALPHA_DISCARD_UNIFORM_LOCATION = fragUniformLocation("ALPHA_DISCARD");
        
        
        glProgramUniform1i(fragHandle(), ATLAS_TEXTURE_UNIFORM_LOCATION, GL33.ATLAS_TEXTURE_UNIT);
        glProgramUniform1i(fragHandle(), LIGHTMAP_TEXTURE_UNIFORM_LOCATION, GL33.LIGHTMAP_TEXTURE_UNIT);
        
        glProgramUniform1i(vertHandle(), WORLD_POSITIONS_UNIFORM_LOCATION, GL33.WORLD_POSITIONS_TEXTURE_UNIT);
        glProgramUniform1i(vertHandle(), DYNAMIC_MATRIX_IDS_UNIFORM_LOCATION, GL33.DYNAMIC_MATRIX_ID_TEXTURE_UNIT);
        glProgramUniform1i(vertHandle(), DYNAMIC_MATRICES_UNIFORM_LOCATION, GL33.DYNAMIC_MATRIX_TEXTURE_UNIT);
        glProgramUniform1i(vertHandle(), DYNAMIC_LIGHT_IDS_UNIFORM_LOCATION, GL33.DYNAMIC_LIGHT_ID_TEXTURE_UNIT);
        glProgramUniform1i(vertHandle(), DYNAMIC_LIGHTS_UNIFORM_LOCATION, GL33.DYNAMIC_LIGHT_TEXTURE_UNIT);
        glProgramUniform1i(vertHandle(), STATIC_MATRICES_UNIFORM_LOCATION, GL33.STATIC_MATRIX_TEXTURE_UNIT);
    }
    
    private int currentAtlas = -1;
    
    public void setAtlas(int textureHandle) {
        if (textureHandle == currentAtlas) {
            return;
        }
        
        glBindTexture(GL_TEXTURE_2D, textureHandle);
        currentAtlas = textureHandle;
    }
    
    public void clearAtlas() {
        currentAtlas = -1;
    }
    
    public void setupDrawInfo(DrawInfo drawInfo) {
        glProgramUniformMatrix4fv(vertHandle(), PROJECTION_MATRIX_UNIFORM_LOCATION, false, drawInfo.projectionMatrixFloatBuffer);
        glProgramUniform3i(vertHandle(), PLAYER_BLOCK_UNIFORM_LOCATION, drawInfo.playerPosition.x, drawInfo.playerPosition.y, drawInfo.playerPosition.z);
        glProgramUniform3f(vertHandle(), PLAYER_SUB_BLOCK_UNIFORM_LOCATION, drawInfo.playerSubBlock.x, drawInfo.playerSubBlock.y, drawInfo.playerSubBlock.z);
        glProgramUniform2f(fragHandle(), FOG_START_END_UNIFORM_LOCATION, drawInfo.fogStart, drawInfo.fogEnd);
        glProgramUniform4f(fragHandle(), FOG_COLOR_UNIFORM_LOCATION, drawInfo.fogColor.x, drawInfo.fogColor.y, drawInfo.fogColor.z, 1);
    }
    
    public void setupRenderPass(GL33RenderPass renderPass) {
        glProgramUniform1i(vertHandle(), VERT_QUAD_UNIFORM_LOCATION, renderPass.QUAD ? GL_TRUE : GL_FALSE);
        glProgramUniform1i(vertHandle(), VERT_LIGHTING_UNIFORM_LOCATION, renderPass.LIGHTING ? GL_TRUE : GL_FALSE);
        glProgramUniform1i(fragHandle(), FRAG_QUAD_UNIFORM_LOCATION, renderPass.QUAD ? GL_TRUE : GL_FALSE);
        glProgramUniform1i(fragHandle(), FRAG_LIGHTING_UNIFORM_LOCATION, renderPass.LIGHTING ? GL_TRUE : GL_FALSE);
        glProgramUniform1i(fragHandle(), TEXTURE_UNIFORM_LOCATION, renderPass.TEXTURE ? GL_TRUE : GL_FALSE);
        glProgramUniform1i(fragHandle(), ALPHA_DISCARD_UNIFORM_LOCATION, renderPass.ALPHA_DISCARD ? GL_TRUE : GL_FALSE);
        var texture = renderPass.texture();
        if (texture != null) {
            setAtlas(texture.getId());
        }
    }
    
    public void setupDrawComponent(int worldPosBaseID, int staticMatrixBaseID, int dynamicMatrixIdOffset, int lightIDOffset) {
        glProgramUniform1i(vertHandle(), WORLD_POSITION_ID_OFFSET_UNIFORM_LOCATION, worldPosBaseID);
        glProgramUniform1i(vertHandle(), STATIC_MATRIX_BASE_ID_UNIFORM_LOCATION, staticMatrixBaseID);
        glProgramUniform1i(vertHandle(), DYNAMIC_MATRIX_ID_OFFSET_UNIFORM_LOCATION, dynamicMatrixIdOffset);
        glProgramUniform1i(vertHandle(), DYNAMIC_LIGHT_ID_OFFSET_UNIFORM_LOCATION, lightIDOffset);
    }
}
