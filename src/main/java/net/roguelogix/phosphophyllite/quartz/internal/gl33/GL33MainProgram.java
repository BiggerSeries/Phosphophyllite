package net.roguelogix.phosphophyllite.quartz.internal.gl33;

import net.minecraft.resources.ResourceLocation;
import net.roguelogix.phosphophyllite.Phosphophyllite;
import net.roguelogix.phosphophyllite.quartz.internal.common.Program;

import static net.roguelogix.phosphophyllite.quartz.internal.common.MagicNumbers.*;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL20C.*;

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
    
    public int QUAD_UNIFORM_LOCATION;
    public int TEXTURE_UNIFORM_LOCATION;
    public int LIGHTING_UNIFORM_LOCATION;
    public int ALPHA_DISCARD_UNIFORM_LOCATION;
    
    private void onReload(Program program) {
        int programHandle = handle();
    
        PLAYER_BLOCK_UNIFORM_LOCATION = glGetUniformLocation(programHandle, "playerBlock");
        PLAYER_SUB_BLOCK_UNIFORM_LOCATION = glGetUniformLocation(programHandle, "playerSubBlock");
        PROJECTION_MATRIX_UNIFORM_LOCATION = glGetUniformLocation(programHandle, "projectionMatrix");
    
        WORLD_POSITION_ID_OFFSET_UNIFORM_LOCATION = glGetUniformLocation(programHandle, "worldPositionIDOffset");
        WORLD_POSITIONS_UNIFORM_LOCATION = glGetUniformLocation(programHandle, "worldPositions");
    
        DYNAMIC_MATRIX_ID_OFFSET_UNIFORM_LOCATION = glGetUniformLocation(programHandle, "dynamicMatrixIDOffset");
        DYNAMIC_MATRIX_IDS_UNIFORM_LOCATION = glGetUniformLocation(programHandle, "dynamicMatrixIDs");
        DYNAMIC_MATRICES_UNIFORM_LOCATION = glGetUniformLocation(programHandle, "dynamicMatrices");
        
        STATIC_MATRIX_BASE_ID_UNIFORM_LOCATION = glGetUniformLocation(programHandle, "staticMatrixBaseID");
        STATIC_MATRICES_UNIFORM_LOCATION = glGetUniformLocation(programHandle, "staticMatrices");
        
        DYNAMIC_LIGHT_ID_OFFSET_UNIFORM_LOCATION = glGetUniformLocation(programHandle, "lightIDOffset");
        DYNAMIC_LIGHT_IDS_UNIFORM_LOCATION = glGetUniformLocation(programHandle, "lightIDs");
        DYNAMIC_LIGHTS_UNIFORM_LOCATION = glGetUniformLocation(programHandle, "lights");
    
        FOG_START_END_UNIFORM_LOCATION = glGetUniformLocation(programHandle, "fogStartEnd");
        FOG_COLOR_UNIFORM_LOCATION = glGetUniformLocation(programHandle, "fogColor");
        ATLAS_TEXTURE_UNIFORM_LOCATION = glGetUniformLocation(programHandle, "atlasTexture");
        LIGHTMAP_TEXTURE_UNIFORM_LOCATION = glGetUniformLocation(programHandle, "lightmapTexture");
    
        QUAD_UNIFORM_LOCATION = glGetUniformLocation(programHandle, "QUAD");
        TEXTURE_UNIFORM_LOCATION = glGetUniformLocation(programHandle, "TEXTURE");
        LIGHTING_UNIFORM_LOCATION = glGetUniformLocation(programHandle, "LIGHTING");
        ALPHA_DISCARD_UNIFORM_LOCATION = glGetUniformLocation(programHandle, "ALPHA_DISCARD");
    
        glUseProgram(programHandle);
    
        glUniform1i(ATLAS_TEXTURE_UNIFORM_LOCATION, GL33.ATLAS_TEXTURE_UNIT);
        glUniform1i(LIGHTMAP_TEXTURE_UNIFORM_LOCATION, GL33.LIGHTMAP_TEXTURE_UNIT);
        
        glUniform1i(WORLD_POSITIONS_UNIFORM_LOCATION, GL33.WORLD_POSITIONS_TEXTURE_UNIT);
        
        glUniform1i(DYNAMIC_MATRIX_IDS_UNIFORM_LOCATION, GL33.DYNAMIC_MATRIX_ID_TEXTURE_UNIT);
        glUniform1i(DYNAMIC_MATRICES_UNIFORM_LOCATION, GL33.DYNAMIC_MATRIX_TEXTURE_UNIT);
    
        glUniform1i(DYNAMIC_LIGHT_IDS_UNIFORM_LOCATION, GL33.DYNAMIC_LIGHT_ID_TEXTURE_UNIT);
        glUniform1i(DYNAMIC_LIGHTS_UNIFORM_LOCATION, GL33.DYNAMIC_LIGHT_TEXTURE_UNIT);
        
        glUniform1i(STATIC_MATRICES_UNIFORM_LOCATION, GL33.STATIC_MATRIX_TEXTURE_UNIT);
        
        glUseProgram(0);
    }
    
    private int currentAtlas = -1;
    
    public void setAtlas(int textureHandle){
        if(textureHandle == currentAtlas){
            return;
        }
        
        glBindTexture(GL_TEXTURE_2D, textureHandle);
        currentAtlas = textureHandle;
    }
    
    public void clearAtlas(){
        currentAtlas = -1;
    }
}
