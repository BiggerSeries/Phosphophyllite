#include <glad/glad.h>

#include "WorldManager.hpp"

#include <glm/gtc/type_ptr.hpp>

#include <common/Shader.hpp>
#include <common/Queues.hpp>

#include "GL46.hpp"
#include "Textures.hpp"

#include <atomic>

namespace glm {

    // todo, <=>?
    constexpr bool operator<(const glm::ivec3& a, const glm::ivec3& b) {
        if (a.x < b.x) {
            return true;
        } else if (a.x == b.x) {
            if (a.y < b.y) {
                return true;
            } else if (a.y == b.y) {
                if (a.z < b.z) {
                    return true;
                }
            }
        }
        return false;
    }
}


namespace Phosphophyllite::Quartz::GL46::World {

    // cube to get actual size, must be <= 16 and a power of 2
    // so, basiclaly, 1, 2, 4, 8, 16
    // 8 is default, this is *technically* configurable, not sure if i will expose it
    // it is a GL 4.6 specific option
    constexpr std::uint8_t RENDERCHUNK_SIZE = 8;
    constexpr std::uint32_t RENDERCHUNK_LINEAR_SIZE = RENDERCHUNK_SIZE * RENDERCHUNK_SIZE * RENDERCHUNK_SIZE;

    struct DrawElementsIndirectCommand {
        uint count = 36;
        uint instanceCount = 0;
        uint firstIndex = 0;
        uint baseVertex = 0;
        uint baseInstance = 0;
    };

    Shader blockProgram; // NOLINT(cert-err58-cpp)

    std::vector<QuartzBlockRenderInfo> toUpdate;
    std::mutex toUpdateMutex;

    GLuint cubeVertexBuffer;
    GLuint cubeElementBuffer;

    std::atomic<GLsync> bufferChangFence = {nullptr};
    std::int32_t buffersSize;
    std::vector<std::int32_t> availableBufferSlots;

    struct Chunk {

        std::int32_t slot = -1;
        DrawElementsIndirectCommand drawCommand = {};
        std::vector<QuartzBlockRenderInfo> blocks;
        std::map<glm::ivec3, std::uint32_t> blockPosMap;

        Chunk() {
            blocks.reserve(RENDERCHUNK_LINEAR_SIZE);
        }
    };

    std::map<glm::ivec3, std::shared_ptr<Chunk>> chunks;

    GLuint buffers[3];
    GLuint chunkIDBuffer;
    GLuint chunkPositionsBuffer;
    GLuint blockPositionsBuffer;

    GLuint cubeVAO;

    void startup() {
        ROGUELIB_STACKTRACE
        blockProgram = {"block"};

        // i know single calls are slow, *meh*
        glCreateBuffers(1, &cubeVertexBuffer);
        glCreateBuffers(1, &cubeElementBuffer);

        std::uint8_t cubeVertexBufferData[] = {
                // x, y, z, u, v, faceID/vertexID

                // west face
                0, 0, 0, 0, 1, 0x00,
                0, 1, 0, 0, 0, 0x10,
                0, 0, 1, 1, 1, 0x20,
                0, 1, 1, 1, 0, 0x30,

                // east face
                1, 0, 0, 1, 1, 0x01,
                1, 1, 0, 1, 0, 0x11,
                1, 0, 1, 0, 1, 0x21,
                1, 1, 1, 0, 0, 0x31,

                // bottom face
                0, 0, 0, 0, 1, 0x02,
                1, 0, 0, 1, 1, 0x12,
                0, 0, 1, 0, 0, 0x22,
                1, 0, 1, 1, 0, 0x32,

                // top face
                0, 1, 0, 0, 0, 0x03,
                1, 1, 0, 1, 0, 0x13,
                0, 1, 1, 0, 1, 0x23,
                1, 1, 1, 1, 1, 0x33,

                // north face
                0, 0, 0, 1, 1, 0x04,
                1, 0, 0, 0, 1, 0x14,
                0, 1, 0, 1, 0, 0x24,
                1, 1, 0, 0, 0, 0x34,

                // south face
                0, 0, 1, 0, 1, 0x05,
                1, 0, 1, 1, 1, 0x15,
                0, 1, 1, 0, 0, 0x25,
                1, 1, 1, 1, 0, 0x35,
        };

        glNamedBufferStorage(cubeVertexBuffer, sizeof(cubeVertexBufferData), cubeVertexBufferData, 0);
        // elements for it all in CCW order
        std::uint8_t cubeElementBufferData[]{
                3, 1, 0, 0, 2, 3,
                1 + 4, 3 + 4, 2 + 4, 2 + 4, 0 + 4, 1 + 4,
                3 + 8, 2 + 8, 0 + 8, 0 + 8, 1 + 8, 3 + 8,
                1 + 12, 0 + 12, 2 + 12, 2 + 12, 3 + 12, 1 + 12,
                2 + 16, 3 + 16, 1 + 16, 1 + 16, 0 + 16, 2 + 16,
                3 + 20, 2 + 20, 0 + 20, 0 + 20, 1 + 20, 3 + 20,
        };
        glNamedBufferStorage(cubeElementBuffer, sizeof(cubeElementBufferData), cubeElementBufferData, 0);

        glCreateVertexArrays(1, &cubeVAO);

        glVertexArrayElementBuffer(cubeVAO, cubeElementBuffer);

        glVertexArrayVertexBuffer(cubeVAO, 0, cubeVertexBuffer, 0, 6);
        glVertexArrayAttribBinding(cubeVAO, 0, 0);
        glVertexArrayAttribBinding(cubeVAO, 1, 0);
        glVertexArrayAttribBinding(cubeVAO, 2, 0);

        glVertexArrayAttribIFormat(cubeVAO, 0, 3, GL_UNSIGNED_BYTE, 0);
        glVertexArrayAttribIFormat(cubeVAO, 1, 2, GL_UNSIGNED_BYTE, 3);
        glVertexArrayAttribIFormat(cubeVAO, 2, 1, GL_UNSIGNED_BYTE, 5);

        glEnableVertexArrayAttrib(cubeVAO, 0);
        glEnableVertexArrayAttrib(cubeVAO, 1);
        glEnableVertexArrayAttrib(cubeVAO, 2);

        glCreateBuffers(3, buffers);
        buffersSize = 1;
        glNamedBufferStorage(chunkIDBuffer = buffers[0], sizeof(std::uint32_t), nullptr, GL_DYNAMIC_STORAGE_BIT);
        glNamedBufferStorage(chunkPositionsBuffer = buffers[1], 4 * sizeof(std::uint32_t), nullptr,
                             GL_DYNAMIC_STORAGE_BIT);
        glNamedBufferStorage(blockPositionsBuffer = buffers[2],
                             RENDERCHUNK_LINEAR_SIZE * sizeof(QuartzBlockRenderInfo::Packed), nullptr,
                             GL_DYNAMIC_STORAGE_BIT);
        availableBufferSlots.push_back(0);
    }

    void shutdown() {
        ROGUELIB_STACKTRACE
        blockProgram = {};
    }

    void setDrawInfo(std::vector<QuartzBlockRenderInfo> infos) {
        ROGUELIB_STACKTRACE
        std::unique_lock lk(toUpdateMutex);
        toUpdate.insert(toUpdate.end(), infos.begin(), infos.end());
    }

    void expandBuffers() {
        auto oldBuffersSize = buffersSize;
        buffersSize *= 2;
        // buffers are auto allocated at size 1

        glCreateBuffers(3, buffers);

        glNamedBufferStorage(buffers[0], (buffersSize) * sizeof(std::uint32_t), nullptr, GL_DYNAMIC_STORAGE_BIT);
        glNamedBufferStorage(buffers[1], (buffersSize) * 4 * sizeof(std::uint32_t), nullptr, GL_DYNAMIC_STORAGE_BIT);
        glNamedBufferStorage
                (buffers[2], (buffersSize) * RENDERCHUNK_LINEAR_SIZE * sizeof(QuartzBlockRenderInfo::Packed),
                 nullptr, GL_DYNAMIC_STORAGE_BIT);

        glCopyNamedBufferSubData(chunkIDBuffer, buffers[0], 0, 0, (oldBuffersSize) * sizeof(std::uint32_t));
        glCopyNamedBufferSubData(chunkPositionsBuffer, buffers[1], 0, 0, (oldBuffersSize) * 4 * sizeof(std::uint32_t));
        glCopyNamedBufferSubData(blockPositionsBuffer, buffers[2], 0, 0,
                                 (oldBuffersSize) * RENDERCHUNK_LINEAR_SIZE * sizeof(QuartzBlockRenderInfo::Packed));

//        GLsync sync = glFenceSync(GL_SYNC_GPU_COMMANDS_COMPLETE, 0);
//        glFlush();
//        bufferChangFence = sync;

        GLuint oldChunkIDBuffer = chunkIDBuffer;
        GLuint oldChunkPositionsBuffer = chunkPositionsBuffer;
        GLuint oldClockPositionsBuffer = blockPositionsBuffer;
        chunkIDBuffer = buffers[0];
        chunkPositionsBuffer = buffers[1];
        blockPositionsBuffer = buffers[2];
        primaryQueue->enqueue([=]() {
            glDeleteBuffers(1, &oldChunkIDBuffer);
            glDeleteBuffers(1, &oldChunkPositionsBuffer);
            glDeleteBuffers(1, &oldClockPositionsBuffer);
        });

        // reverse order so it should group more towards the front of the GL buffer
        for (int i = buffersSize - 1; i >= oldBuffersSize; --i) {
            availableBufferSlots.push_back(i);
        }
    }

    void updateBuffer() {
        std::vector<QuartzBlockRenderInfo> changes;
        {
            std::unique_lock lk(toUpdateMutex);
            toUpdate.swap(changes);
        }

        for (const auto& item : changes) {
            // these are powers of two, so this should be super fast, if the compiler optimizes it right
            glm::ivec3 blockChunk =
                    {((item.x + (item.x < 0)) / RENDERCHUNK_SIZE) - (item.x < 0),
                     ((item.y + (item.y < 0)) / RENDERCHUNK_SIZE) - (item.y < 0),
                     ((item.z + (item.z < 0)) / RENDERCHUNK_SIZE) - (item.z < 0)};
            glm::ivec3 blockSubChunk = {item.x, item.y, item.z};
            blockSubChunk -= blockChunk * 8;

            if ((item.textureIDWest & item.textureIDEast &
                 item.textureIDBottom & item.textureIDTop &
                 item.textureIDSouth & item.textureIDNorth) >= (2 << 30)) {
                // remove the block, if it exists

                auto iter = chunks.find(blockChunk);
                if (iter != chunks.end()) {
                    auto chunk = iter->second;
                    auto blockIter = chunk->blockPosMap.find(blockSubChunk);
                    if (blockIter == chunk->blockPosMap.end()) {
                        // so, it doesnt have the block? idfk
                        continue;
                    } else {

                        // deactivate the old textures, dont need them around anymore
                        auto itemSlot = blockIter->second;
                        auto itemOldRenderInfo = chunk->blocks[itemSlot];
                        Textures::inactivateTexture(itemOldRenderInfo.textureIDWest);
                        Textures::inactivateTexture(itemOldRenderInfo.textureIDEast);
                        Textures::inactivateTexture(itemOldRenderInfo.textureIDBottom);
                        Textures::inactivateTexture(itemOldRenderInfo.textureIDTop);
                        Textures::inactivateTexture(itemOldRenderInfo.textureIDSouth);
                        Textures::inactivateTexture(itemOldRenderInfo.textureIDNorth);

                        auto blockLocalPos = blockIter->second;
                        if (blockLocalPos != (chunk->blocks.size() - 1)) {
                            // it wasn't the last block, so i need to swap blocks around

                            auto chunkBaseBlockSlot = chunk->slot * RENDERCHUNK_LINEAR_SIZE;
                            auto newBlockSlot = chunkBaseBlockSlot + blockLocalPos;
                            auto oldBlockSlot = chunkBaseBlockSlot + chunk->blocks.size() - 1;

                            auto newBlockSlotBytePos = newBlockSlot * sizeof(QuartzBlockRenderInfo::Packed);
                            auto oldBlockSlotBytePos = oldBlockSlot * sizeof(QuartzBlockRenderInfo::Packed);

                            // first, update CPU side info
                            auto newBlockInfo = chunk->blocks.back();
                            chunk->blocks.pop_back();
                            chunk->blocks[blockLocalPos] = newBlockInfo;
                            glm::ivec3 newBlockInfoSubChunkPos = {
                                    std::uint32_t(newBlockInfo.x) % RENDERCHUNK_SIZE,
                                    std::uint32_t(newBlockInfo.y) % RENDERCHUNK_SIZE,
                                    std::uint32_t(newBlockInfo.z) % RENDERCHUNK_SIZE
                            };
                            chunk->blockPosMap[newBlockInfoSubChunkPos] = blockLocalPos;
                            chunk->blockPosMap.erase(blockSubChunk);

                            chunk->drawCommand.instanceCount--;

                            // ok, now to update GL's buffer
                            glCopyNamedBufferSubData(blockPositionsBuffer, blockPositionsBuffer,
                                                     oldBlockSlotBytePos,
                                                     newBlockSlotBytePos, sizeof(QuartzBlockRenderInfo::Packed));
                        } else {
                            // last block, just yeet it
                            chunk->drawCommand.instanceCount--;
                            chunk->blockPosMap.erase(blockSubChunk);
                            chunk->blocks.pop_back();
                        }
                    }

                    if (chunk->blocks.empty()) {
                        chunks.erase(blockChunk);
                        availableBufferSlots.emplace_back(chunk->slot);
                    }
                }
                continue;
            }

            // update or add the block

            // make current texture's resident
            Textures::activateTexture(item.textureIDWest);
            Textures::activateTexture(item.textureIDEast);
            Textures::activateTexture(item.textureIDBottom);
            Textures::activateTexture(item.textureIDTop);
            Textures::activateTexture(item.textureIDSouth);
            Textures::activateTexture(item.textureIDNorth);

            std::shared_ptr<Chunk> chunk;
            auto chunkPosIter = chunks.find(blockChunk);
            if (chunkPosIter == chunks.end()) {
                if (availableBufferSlots.empty()) {
                    expandBuffers();
                }
                chunk = std::make_shared<Chunk>();
                chunk->slot = availableBufferSlots.back();
                glm::ivec3 chunkPos = blockChunk;
                chunkPos *= RENDERCHUNK_SIZE;
                glNamedBufferSubData(chunkPositionsBuffer, chunk->slot * 4 * sizeof(std::uint32_t),
                                     3 * sizeof(std::uint32_t), glm::value_ptr(chunkPos));
                availableBufferSlots.pop_back();
                chunks[blockChunk] = chunk;
            } else {
                chunk = chunkPosIter->second;
            }

            std::uint32_t blockPos;
            auto blockIter = chunk->blockPosMap.find(blockSubChunk);
            if (blockIter == chunk->blockPosMap.end()) {
                blockPos = chunk->blocks.size();
                chunk->blocks.emplace_back();
                chunk->blockPosMap[blockSubChunk] = blockPos;
                chunk->drawCommand.instanceCount++;
            } else {
                blockPos = blockIter->second;

                auto oldBlockRenderData = chunk->blocks[blockPos];

                Textures::inactivateTexture(oldBlockRenderData.textureIDWest);
                Textures::inactivateTexture(oldBlockRenderData.textureIDEast);
                Textures::inactivateTexture(oldBlockRenderData.textureIDBottom);
                Textures::inactivateTexture(oldBlockRenderData.textureIDTop);
                Textures::inactivateTexture(oldBlockRenderData.textureIDSouth);
                Textures::inactivateTexture(oldBlockRenderData.textureIDNorth);
            }

            chunk->blocks[blockPos] = item;

            QuartzBlockRenderInfo itemToPack = item;
            itemToPack.x = blockSubChunk.x;
            itemToPack.y = blockSubChunk.y;
            itemToPack.z = blockSubChunk.z;

            QuartzBlockRenderInfo::Packed packedData = itemToPack.pack();
            glNamedBufferSubData(blockPositionsBuffer,
                                 ((chunk->slot * RENDERCHUNK_LINEAR_SIZE) + blockPos) *
                                 sizeof(QuartzBlockRenderInfo::Packed),
                                 sizeof(QuartzBlockRenderInfo::Packed), &packedData);
        }
    }

    bool getLightmapHandle = true;
    GLuint64 lightmapHandle = -1;

    void draw() {
        ROGUELIB_STACKTRACE

        primaryQueue->enqueue([]() {
            std::unique_lock lk(toUpdateMutex);
            if (!toUpdate.empty()) {
                lk.unlock();
                updateBuffer();
            }
        });

        static GLsync lastFence = nullptr;
        GLsync fence = bufferChangFence;
        if (fence != lastFence) {
//            glFlush();
//            glWaitSync(fence, 0, GL_TIMEOUT_IGNORED);
            lastFence = fence;
        }

        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);

        GLint activeTexture;
        glGetIntegerv(GL_ACTIVE_TEXTURE, &activeTexture);
        glActiveTexture(GL_TEXTURE2);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glActiveTexture(activeTexture);
        
        blockProgram.bind();

        Textures::bind(blockProgram.handle());

        glm::ivec3 playerChunkPosition = playerPosition;
        playerChunkPosition &= ~(RENDERCHUNK_SIZE - 1);
        glm::vec3 playerSubChunkPos = playerPosition - glm::dvec3(playerChunkPosition);

        glUniform3fv(0, 1, glm::value_ptr(playerSubChunkPos));
        glUniform3iv(1, 1, glm::value_ptr(playerChunkPosition));
        glUniform1i(6, RENDERCHUNK_LINEAR_SIZE);
        glUniformMatrix4fv(4, 1, false, glm::value_ptr(modelViewMatrix));
        glUniformMatrix4fv(5, 1, false, glm::value_ptr(projectionMatrix));

        // cant use bindless for the lightmap, thx mojang
        glUniform1i(12, 2);

        // todo: compute shader?
        std::vector<DrawElementsIndirectCommand> drawCommands;
        std::vector<GLuint> chunkIDs;

        for (auto& chunkMapPair : chunks) {
            glm::ivec3 chunkPos = chunkMapPair.first;
            chunkPos *= RENDERCHUNK_SIZE;
            auto chunk = chunkMapPair.second;


            auto chunkClipVector = glm::vec4(glm::dvec3(chunkPos) + playerOffset, 1);
            chunkClipVector = modelViewProjectionMatrix * chunkClipVector;
            chunkClipVector /= chunkClipVector.w;

            auto chunkClipVectorMin = chunkClipVector;
            auto chunkClipVectorMax = chunkClipVector;

            for (std::uint32_t i = 1; i < 8; i++) {
                chunkClipVector = glm::vec4(glm::dvec3(chunkPos) + playerOffset, 1);
                chunkClipVector += glm::vec4(((i & 1u) * RENDERCHUNK_SIZE), (((i & 2u) * RENDERCHUNK_SIZE) / 2),
                                             (((i & 4u) * RENDERCHUNK_SIZE) / 4), 0);

                chunkClipVector = modelViewProjectionMatrix * chunkClipVector;
                chunkClipVector /= chunkClipVector.w;

                chunkClipVectorMin = glm::min(chunkClipVectorMin, chunkClipVector);
                chunkClipVectorMax = glm::max(chunkClipVectorMax, chunkClipVector);
            }

            if (chunkClipVectorMin.x <= 1 && chunkClipVectorMax.x >= -1 &&
                chunkClipVectorMin.y <= 1 && chunkClipVectorMax.y >= -1 &&
                chunkClipVectorMin.z <= 1 && chunkClipVectorMax.z >= -1) {
                drawCommands.emplace_back(chunk->drawCommand);
                chunkIDs.emplace_back(chunk->slot);
            }
        }

        glNamedBufferSubData(chunkIDBuffer, 0, chunkIDs.size() * sizeof(GLuint), chunkIDs.data());

        glBindVertexArray(cubeVAO);

        glBindBuffersBase(GL_SHADER_STORAGE_BUFFER, 0, 3, buffers);
        // just a sanity check
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);

        // AMD, fix your drivers
        //        glMultiDrawElementsIndirect(GL_TRIANGLES, GL_UNSIGNED_BYTE, drawCommands.data(), drawCommands.size(), sizeof(DrawElementsIndirectCommand));
        for (int i = 0; i < drawCommands.size(); ++i) {
            DrawElementsIndirectCommand drawCommand = drawCommands[i];
            glUniform1i(128, i);
            glDrawElementsInstancedBaseVertexBaseInstance(GL_TRIANGLES, drawCommand.count, GL_UNSIGNED_BYTE,
                                                          reinterpret_cast<const void*>(drawCommand.firstIndex),
                                                          drawCommand.instanceCount, drawCommand.baseVertex,
                                                          drawCommand.baseInstance);
        }


        glBindBuffersBase(GL_SHADER_STORAGE_BUFFER, 0, 3, nullptr);
        glBindVertexArray(0);

        glUseProgram(0);
    }
}
