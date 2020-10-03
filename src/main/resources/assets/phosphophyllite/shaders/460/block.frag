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

    fragColor = vec4(vec3(diffuseMultiplier), 1);

    {
        // TODO: 6 bit input for the two lighting coords, space is there already
        // TODO: AO color multiplier input, use two of above extra bits
        vec3 lightmapPos0 = vec3((lightmapValues.x >> 8) & 0x3Fu, lightmapValues.x & 0x3Fu, (lightmapValues.x >> 6) & 0x3u);
        vec3 lightmapPos1 = vec3((lightmapValues.y >> 8) & 0x3Fu, lightmapValues.y & 0x3Fu, (lightmapValues.y >> 6) & 0x3u);
        vec3 lightmapPos2 = vec3((lightmapValues.z >> 8) & 0x3Fu, lightmapValues.z & 0x3Fu, (lightmapValues.z >> 6) & 0x3u);
        vec3 lightmapPos3 = vec3((lightmapValues.w >> 8) & 0x3Fu, lightmapValues.w & 0x3Fu, (lightmapValues.w >> 6) & 0x3u);

        vec3 lightmapPosX0 = lightmapPos0 * lightingAveragePos.x + lightmapPos1 * (1 - lightingAveragePos.x);
        vec3 lightmapPosX1 = lightmapPos2 * lightingAveragePos.x + lightmapPos3 * (1 - lightingAveragePos.x);
        vec3 lightmapPosAVG = lightmapPosX0 * lightingAveragePos.y + lightmapPosX1 * (1 - lightingAveragePos.y);

        float AOMultiplier = 1.0 - (lightmapPosAVG.z * 0.2);

        vec2 lightmapPos = lightmapPosAVG.xy;
        lightmapPos /= 4.0;
        lightmapPos = clamp(lightmapPos, 0.0, 15.0);
        lightmapPos += 1.0/2.0;
        lightmapPos /= 16.0;

        //        lightmapPos0.x += 1;
        vec4 lightmapColor = texture(lightmapTexture, lightmapPos);

        fragColor *= lightmapColor;
        fragColor *= AOMultiplier;
    }

    sampler2D textureSampler = sampler2D(textureHandle);
    vec4 textureValue = texture(textureSampler, textureCoordinate);
    fragColor *= textureValue;

    if(textureValue.a < 0.5){
        discard;
    }

    fragColor.a = 1;

}