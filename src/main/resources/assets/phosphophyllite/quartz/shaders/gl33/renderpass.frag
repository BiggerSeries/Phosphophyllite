#version 330 core
// gpuinfo says this is supported, so im using it
#extension GL_ARB_separate_shader_objects : enable

layout(location = 0) in float fragmentDistance;
layout(location = 1) in vec4 vertexColor;

layout(location = 2) in vec2 texCoord;

layout(location = 3) flat in vec3 fragmentNormal;
#define LIGHTMAP_MULTIPLIER 0.015625 /* 1 / 64 (6 bit) */
layout(location = 4) in vec2 lightmapCoord;
layout(location = 5) in vec2 lightmapCoords[4];// locations 5 6 7 8

layout(location = 9) in float diffuseMultiplier;
layout(location = 10) in vec3 fragmentModelPos;
layout(location = 11) flat in vec3[8] cornerLightLevels;// locations 11-18

uniform vec2 fogStartEnd;
uniform vec4 fogColor;

uniform bool LIGHTING;
uniform bool QUAD;
uniform bool TEXTURE;
uniform bool ALPHA_DISCARD;

uniform sampler2D atlasTexture;
uniform sampler2D lightmapTexture;

layout(location = 0) out vec4 color;

void main(){
    color = vec4(vec3(diffuseMultiplier), 1);

    color *= vertexColor;

    if (TEXTURE) {
        color *= texture(atlasTexture, texCoord);
    }

    if (LIGHTING){
        vec2 lightPos = lightmapCoord;
        if (QUAD) {
            vec2 vert01Avg = lightmapCoords[0] * lightmapCoord.x + lightmapCoords[1] * (1 - lightmapCoord.x);
            vec2 vert23Avg = lightmapCoords[2] * lightmapCoord.x + lightmapCoords[3] * (1 - lightmapCoord.x);
            lightPos = vert01Avg * lightmapCoord.y + vert23Avg * (1 - lightmapCoord.y);
        }

        vec3 avgArray[4];
        avgArray[0] = cornerLightLevels[4] * fragmentModelPos.z + cornerLightLevels[0] * (1 - fragmentModelPos.z);
        avgArray[1] = cornerLightLevels[5] * fragmentModelPos.z + cornerLightLevels[1] * (1 - fragmentModelPos.z);
        avgArray[2] = cornerLightLevels[6] * fragmentModelPos.z + cornerLightLevels[2] * (1 - fragmentModelPos.z);
        avgArray[3] = cornerLightLevels[7] * fragmentModelPos.z + cornerLightLevels[3] * (1 - fragmentModelPos.z);
        avgArray[0] = avgArray[2] * fragmentModelPos.y + avgArray[0] * (1 - fragmentModelPos.y);
        avgArray[1] = avgArray[3] * fragmentModelPos.y + avgArray[1] * (1 - fragmentModelPos.y);
        avgArray[0] = avgArray[1] * fragmentModelPos.x + avgArray[0] * (1 - fragmentModelPos.x);
        lightPos += avgArray[0].xy;

        float AO = avgArray[0].z;
        float AOMultiplier = 1 - AO * .2;

        color *= texture(lightmapTexture, clamp(lightPos, vec2(0.5 / 16), vec2(15.5 / 16)));
        color = vec4(color.rgb * AOMultiplier, color.a);
    }

    float fogValue = clamp(smoothstep(fogStartEnd.x, fogStartEnd.y, fragmentDistance) * fogColor.a, 0.0, 1.0);
    color = vec4(mix(color.rgb, fogColor.rgb, fogValue), color.a);

    if (ALPHA_DISCARD) {
        #ifndef MIN_ALPHA
        #define MIN_ALPHA 0.5
        #endif
        if (color.a < MIN_ALPHA){
            discard;
        }
    }
}