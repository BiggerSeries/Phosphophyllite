package net.roguelogix.phosphophyllite.quartz.internal;

import static org.lwjgl.opengl.GL46C.GL_TEXTURE0;

public class MagicNumbers {
    public static final int VERTEX_BYTE_SIZE = 32;
    public static final int FLOAT_BYTE_SIZE = 4;
    public static final int INT_BYTE_SIZE = 4;
    public static final int VEC4_BYTE_SIZE = FLOAT_BYTE_SIZE * 4;
    public static final int IVEC4_BYTE_SIZE = INT_BYTE_SIZE * 4;
    public static final int MATRIX_4F_BYTE_SIZE = 64;
    public static final int MATRIX_4F_BYTE_SIZE_2 = MATRIX_4F_BYTE_SIZE * 2;
    
    // 1 byte per direction per vertex per light state, so 1 * 6 * 8 * 2, but then some empty space because alignment
    public static final int DYNAMIC_LIGHT_BYTE_SIZE = 128;
    
    public static class GL {
        // VEC4_BYTE_SIZE + INT_BYTE_SIZE + INT_BYTE_SIZE + MATRIX_4F_BYTE_SIZE_2 == 152, padded to 256
        public static final int INSTANCE_DATA_BYTE_SIZE = 256;
        public static final int WORLD_POSITION_OFFSET = 0;
        public static final int DYNAMIC_MATRIX_ID_OFFSET = WORLD_POSITION_OFFSET + VEC4_BYTE_SIZE;
        public static final int DYNAMIC_LIGHT_ID_OFFSET = DYNAMIC_MATRIX_ID_OFFSET + INT_BYTE_SIZE;
        public static final int STATIC_MATRIX_OFFSET = DYNAMIC_LIGHT_ID_OFFSET + INT_BYTE_SIZE;
        public static final int STATIC_NORMAL_MATRIX_OFFSET = STATIC_MATRIX_OFFSET + MATRIX_4F_BYTE_SIZE;
        
        public static final int POSITION_LOCATION = 0;
        public static final int COLOR_LOCATION = 1;
        public static final int TEX_COORD_LOCATION = 2;
        public static final int LIGHTINFO_LOCATION = 3;
        public static final int WORLD_POSITION_LOCATION = 4;
        public static final int DYNAMIC_MATRIX_ID_LOCATION = 5;
        public static final int DYNAMIC_LIGHT_ID_LOCATION = 6;
        // location 7 open
        public static final int STATIC_MATRIX_LOCATION = 8;
        public static final int STATIC_NORMAL_MATRIX_LOCATION = 12;
        // 16 locations available, so, none left, if more are needed, will need to pack values
        // lightInfo and colorIn could be packed together
        // light/matrix IDs could be packed together
        
        public static final int ATLAS_TEXTURE_UNIT = 0;
        public static final int LIGHTMAP_TEXTURE_UNIT = 1;
        public static final int DYNAMIC_MATRIX_TEXTURE_UNIT = 2;
        public static final int DYNAMIC_LIGHT_TEXTURE_UNIT = 3;
        
        public static final int ATLAS_TEXTURE_UNIT_GL = GL_TEXTURE0 + ATLAS_TEXTURE_UNIT;
        public static final int LIGHTMAP_TEXTURE_UNIT_GL = GL_TEXTURE0 + LIGHTMAP_TEXTURE_UNIT;
        public static final int DYNAMIC_MATRIX_TEXTURE_UNIT_GL = GL_TEXTURE0 + DYNAMIC_MATRIX_TEXTURE_UNIT;
        public static final int DYNAMIC_LIGHT_TEXTURE_UNIT_GL = GL_TEXTURE0 + DYNAMIC_LIGHT_TEXTURE_UNIT;
    }
}
