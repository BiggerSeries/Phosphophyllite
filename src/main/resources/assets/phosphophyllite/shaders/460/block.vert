#version 460

#extension GL_ARB_separate_shader_objects : enable
#extension GL_ARB_bindless_texture : enable

layout(location = 0) in ivec3 vertexPosition;
layout(location = 1) in uvec2 vertexTextureCoordinate;
layout(location = 2) in uint faceVertexID;

layout(location = 0) uniform vec3 playerPosition;
layout(location = 1) uniform ivec3 playerChunkPosition;

layout(location = 4) uniform mat4 modelViewMatrix;
layout(location = 5) uniform mat4 projectionMatrix;

layout(std430, binding = 0) buffer SSBOA{
    uint chunkIDs[];
};

layout(std430, binding = 1) buffer SSBOB{
    ivec4 chunkPositions[];
};

struct PerBlockData{
    uint blockPositionPacked;
    uint textureIDRotation[6];
    uint lightmap[3][4];
};

layout(location = 6) uniform int blockPositionsPerChunkStride;

layout(std430, binding = 2) buffer SSBOC{
    PerBlockData BlockData[];
};

// TODO: should i change this an SSBO?
layout(location = 16, bindless_sampler) uniform usamplerBuffer textureHandleBuff;
layout(location = 17) uniform uint idLimit;


layout(location = 0) out vec2 textureCoordinate;
layout(location = 1) out flat uvec2 textureHandle;
layout(location = 2) out vec2 lightmapCoordinate;
layout(location = 3) out flat float diffuseMultiplier;
layout(location = 4) out float fogCoord;
layout(location = 5) out flat uvec4 lightmapValues;

vec2 rotateUV(vec2 uv, uint rotation) {
    rotation &= 3u;// 0b11
    // NO BRANCHING!!!! no, really, dont
    uv -= 0.5;
    {
        float newX = uv.x * (~rotation & 1u) + uv.y * (rotation & 1u);
        newX *= .5 * float(~rotation & 2u) + -.5 * float(rotation & 2u);
        float newY = uv.y * (~rotation & 1u) + uv.x * (rotation & 1u);
        uint negateY = (rotation >> 1u) ^ (rotation & 1u);
        newY *= -1 * float(negateY) + 1 * (~negateY & 1u);
        uv = vec2(newX, newY);
    }
    uv += 0.5;
    return uv;
}

layout(location = 128) uniform int DrawID;

void main(){
    //    gl_Position = vec4(vertexPosition - playerPosition, 1);
    //    gl_Position.y += gl_DrawID;
    //    gl_Position = projectionMatrix * modelViewMatrix * gl_Position;


    uint chunkID = chunkIDs[DrawID];
    uint globalBlockID = gl_InstanceID + (chunkID * blockPositionsPerChunkStride);

    PerBlockData blockData = BlockData[globalBlockID];

    ivec3 chunkPosition = chunkPositions[chunkID].xyz;
    uint blockPositionPacked = blockData.blockPositionPacked;
    ivec3 blockPosition = ivec3((blockPositionPacked >> 16u) & 0xFFu, (blockPositionPacked >> 8u) & 0xFFu, (blockPositionPacked >> 0u) & 0xFFu);
    blockPosition &= 0xF;

    ivec3 vertexBlockPosition = vertexPosition + blockPosition;
    ivec3 vertexChunkPosition = vertexBlockPosition + chunkPosition;
    ivec3 vertexChunkViewPosition = vertexChunkPosition - playerChunkPosition;

    vec3 vertexTranslatedLocation = vertexChunkViewPosition - playerPosition;

    vec4 worldPos = vec4(vertexTranslatedLocation, 1);
    vec4 viewPos = modelViewMatrix * worldPos;
    gl_Position = projectionMatrix * viewPos;

    fogCoord = length(viewPos);

    uint faceID = uint(faceVertexID) & 0xFu;
    uint vertexID = (faceVertexID >> 4u) & 0xFu;

    uint faceBit = 1u << faceID;

    // diffuse
    float diffuse = 0;
    // west
    diffuse += float((faceBit >> 0u) & 1u) * 0.6;
    diffuse += float((faceBit >> 1u) & 1u) * 0.6;
    diffuse += float((faceBit >> 2u) & 1u) * 0.5;
    diffuse += float((faceBit >> 3u) & 1u) * 1.0;
    diffuse += float((faceBit >> 4u) & 1u) * 0.8;
    diffuse += float((faceBit >> 5u) & 1u) * 0.8;
    diffuseMultiplier = diffuse;

    lightmapCoordinate = vertexTextureCoordinate;

    lightmapValues.x = blockData.lightmap[faceID >> 1][0] >> (16u * (faceID & 1u));
    lightmapValues.y = blockData.lightmap[faceID >> 1][1] >> (16u * (faceID & 1u));
    lightmapValues.z = blockData.lightmap[faceID >> 1][2] >> (16u * (faceID & 1u));
    lightmapValues.w = blockData.lightmap[faceID >> 1][3] >> (16u * (faceID & 1u));

    uint textureIndexRotationPacked = BlockData[globalBlockID].textureIDRotation[faceID];
    uint rotation = textureIndexRotationPacked & 0x3u;
    vec2 rotatedCoordinate = rotateUV(vertexTextureCoordinate, rotation);
    textureCoordinate = rotatedCoordinate;

    uint textureID = textureIndexRotationPacked >> 2;
    textureID *= uint(textureID < idLimit);
    textureHandle = texelFetch(textureHandleBuff, int(textureID)).xy;

    uint hideFace = uint(uint(textureIndexRotationPacked >> 2) > uint((1 << 30) - 1));
    gl_Position.z = (hideFace * 2) + ((1 - hideFace) * gl_Position.z);
    gl_Position.w = (hideFace * 1) + ((1 - hideFace) * gl_Position.w);
}