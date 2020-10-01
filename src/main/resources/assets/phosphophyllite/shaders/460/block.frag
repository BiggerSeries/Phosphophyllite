#version 460

#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_bindless_texture : enable

layout(location = 0) in vec2 textureCoordinate;
layout(location = 1) in flat uvec2 textureHandle;
layout(location = 2) in vec2 lightingAveragePos;
layout(location = 3) in flat float diffuseMultiplier;
layout(location = 4) in float fogCoord;
layout(location = 5) in flat uvec4 lightmapValues;

layout(location = 12) uniform sampler2D lightmapTexture;

layout(location = 13) uniform vec4 fogColor;
layout(location = 14) uniform vec2 fogScaleEnd;

out vec4 fragColor;

void main(){

    fragColor = vec4(1, 1, 1, 1) * diffuseMultiplier;

    sampler2D textureSampler = sampler2D(textureHandle);
    fragColor *= texture(textureSampler, textureCoordinate);

    {
        // TODO: 6 bit input for the two lighting coords, space is there already
        // TODO: AO color multiplier input, use two of above extra bits
        vec2 lightmapPos0 = vec2((lightmapValues.x >> 8) & 0x3Fu, lightmapValues.x & 0x3Fu);
        vec2 lightmapPos1 = vec2((lightmapValues.y >> 8) & 0x3Fu, lightmapValues.y & 0x3Fu);
        vec2 lightmapPos2 = vec2((lightmapValues.z >> 8) & 0x3Fu, lightmapValues.z & 0x3Fu);
        vec2 lightmapPos3 = vec2((lightmapValues.w >> 8) & 0x3Fu, lightmapValues.w & 0x3Fu);

        vec2 lightmapPosX0 = lightmapPos0 * lightingAveragePos.x + lightmapPos1 * (1 - lightingAveragePos.x);
        vec2 lightmapPosX1 = lightmapPos2 * lightingAveragePos.x + lightmapPos3 * (1 - lightingAveragePos.x);
        vec2 lightmapPosAVG = lightmapPosX0 * lightingAveragePos.y + lightmapPosX1 * (1 - lightingAveragePos.y);


        lightmapPosAVG /= 4.0;
        lightmapPosAVG = clamp(lightmapPosAVG, 0.0, 15.0);
        lightmapPosAVG += 1.0/2.0;
        lightmapPosAVG /= 16.0;

        //        lightmapPos0.x += 1;
        vec4 lightmapColor = texture(lightmapTexture, lightmapPosAVG);

        fragColor *= lightmapColor;
    }

}