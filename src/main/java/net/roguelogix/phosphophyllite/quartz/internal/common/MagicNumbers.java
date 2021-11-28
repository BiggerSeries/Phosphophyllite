package net.roguelogix.phosphophyllite.quartz.internal.common;

import static org.lwjgl.opengl.GL46C.*;

public class MagicNumbers {
    public static final int VERTEX_BYTE_SIZE = 32;
    public static final int FLOAT_BYTE_SIZE = 4;
    public static final int INT_BYTE_SIZE = 4;
    public static final int VEC4_BYTE_SIZE = FLOAT_BYTE_SIZE * 4;
    public static final int MATRIX_4F_BYTE_SIZE = 64;
    public static final int MATRIX_4F_BYTE_SIZE_2 = MATRIX_4F_BYTE_SIZE * 2;
    
    // 1 byte per direction per vertex per light state, so 1 * 6 * 8 * 2, but then some empty space because alignment
    public static final int DYNAMIC_LIGHT_BYTE_SIZE = 128;
    
    public static final int POSITION_LOCATION = 0;
    public static final int COLOR_LOCATION = 1;
    public static final int TEX_COORD_LOCATION = 2;
    public static final int LIGHTINFO_LOCATION = 3;
    
    public static class GL33 {
        public static final int ATLAS_TEXTURE_UNIT = 0;
        public static final int LIGHTMAP_TEXTURE_UNIT = 1;
        
        public static final int WORLD_POSITIONS_TEXTURE_UNIT = 2;
        
        public static final int DYNAMIC_MATRIX_ID_TEXTURE_UNIT = 3;
        public static final int DYNAMIC_MATRIX_TEXTURE_UNIT = 4;
        
        public static final int STATIC_MATRIX_TEXTURE_UNIT = 5;
        
        public static final int DYNAMIC_LIGHT_ID_TEXTURE_UNIT = 6;
        public static final int DYNAMIC_LIGHT_TEXTURE_UNIT = 7;
        
        public static final int ATLAS_TEXTURE_UNIT_GL = GL_TEXTURE0 + ATLAS_TEXTURE_UNIT;
        public static final int LIGHTMAP_TEXTURE_UNIT_GL = GL_TEXTURE0 + LIGHTMAP_TEXTURE_UNIT;
        public static final int WORLD_POSITIONS_TEXTURE_UNIT_GL = GL_TEXTURE0 + WORLD_POSITIONS_TEXTURE_UNIT;
        public static final int DYNAMIC_MATRIX_ID_TEXTURE_UNIT_GL = GL_TEXTURE0 + DYNAMIC_MATRIX_ID_TEXTURE_UNIT;
        public static final int DYNAMIC_MATRIX_TEXTURE_UNIT_GL = GL_TEXTURE0 + DYNAMIC_MATRIX_TEXTURE_UNIT;
        public static final int STATIC_MATRIX_TEXTURE_UNIT_GL = GL_TEXTURE0 + STATIC_MATRIX_TEXTURE_UNIT;
        public static final int DYNAMIC_LIGHT_ID_TEXTURE_UNIT_GL = GL_TEXTURE0 + DYNAMIC_LIGHT_ID_TEXTURE_UNIT;
        public static final int DYNAMIC_LIGHT_TEXTURE_UNIT_GL = GL_TEXTURE0 + DYNAMIC_LIGHT_TEXTURE_UNIT;
    }
}
