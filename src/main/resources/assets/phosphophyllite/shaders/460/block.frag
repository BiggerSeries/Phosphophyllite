#version 460

#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_bindless_texture : enable

layout(location = 0) in vec2 textureCoordinate;
layout(location = 1) in flat uvec2 textureHandle;
layout(location = 2) in vec2 lightingAveragePos;
layout(location = 3) in flat float diffuseMultiplier;
layout(location = 4) in float fogCoord;
layout(location = 5) in flat uvec4 lightmapValues;

layout(location = 12, bindless_sampler) uniform sampler2D lightmapTexture;

layout(location = 13) uniform vec4 fogColor;
layout(location = 14) uniform vec2 fogScaleEnd;

layout(location = 0) out vec4 fragColor;

void main(){
    fragColor = vec4(1, 1, 1, 1) * diffuseMultiplier;

    sampler2D textureSampler = sampler2D(textureHandle);
    fragColor *= texture(textureSampler, textureCoordinate);

    {
        // TODO: 6 bit input for the two lighting coords, space is there already
        // TODO: AO color multiplier input, use two of above extra 4 bits
        vec2 lightmapPos0 = vec2((lightmapValues.x >> 6) & 0x3Fu, lightmapValues.x & 0x3Fu);
        vec2 lightmapPos1 = vec2((lightmapValues.y >> 6) & 0x3Fu, lightmapValues.y & 0x3Fu);
        vec2 lightmapPos2 = vec2((lightmapValues.z >> 6) & 0x3Fu, lightmapValues.z & 0x3Fu);
        vec2 lightmapPos3 = vec2((lightmapValues.w >> 6) & 0x3Fu, lightmapValues.w & 0x3Fu);

        lightmapPos0 = lightmapPos0 * lightingAveragePos.x + lightmapPos1 * (1 - lightingAveragePos.x);
        lightmapPos2 = lightmapPos2 * lightingAveragePos.x + lightmapPos3 * (1 - lightingAveragePos.x);
        lightmapPos0 = lightmapPos0 * lightingAveragePos.y + lightmapPos2 * (1 - lightingAveragePos.y);


        lightmapPos0 /= 4.0;
        lightmapPos0 = clamp(lightmapPos0, 0.0, 15.0);
        lightmapPos0 += 1.0/2.0;
        lightmapPos0 /= 16.0;

        //        lightmapPos0.x += 1;
        vec4 lightmapColor = texture(lightmapTexture, lightmapPos0);
    }
}