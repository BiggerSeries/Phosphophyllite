#include "Textures.hpp"

#include <glad/glad.h>


#include <RogueLib/Exceptions/Exceptions.hpp>

#include <common/Queues.hpp>

#include <atomic>
#include <utility>
#include <common/jni.hpp>

#define STB_IMAGE_IMPLEMENTATION

#include <stb_image.h>

namespace Phosphophyllite::Quartz::GL46::Textures {

    void updateBuffer();

    using namespace RogueLib;
    using namespace RogueLib::Threading;

    GLuint64 TBOTextureHandle = 0;
    GLuint TBOTexture = 0;
    GLuint TBO = 0;
    std::size_t TBOsize = 0;
    std::vector<GLuint64> textureHandles;
    std::uint32_t GLSLidLimit = 0;

    struct Texture {
        GLuint glID = 0;
        GLuint64 handle = 0;
        std::string location;
        std::uint64_t uses = 0;
    };

    std::vector<Texture> textures;

    void startup() {
    }

    void shutdown() {
        glDeleteBuffers(1, &TBO);
        glDeleteTextures(1, &TBOTexture);
    }


    std::uint32_t loadTexture(std::string location) {
        ROGUELIB_STACKTRACE
        textures.emplace_back();
        std::uint32_t id = textures.size() - 1;

        // 30 bit limit as i shove rotation in the same 32bit VBO value,
        // still a 1 billion texture limit, i dont see how this will be hit
        if (id >= (1u << 30u)) {
            // how, just fucking how?
            throw Exceptions::FatalInvalidState(ROGUELIB_EXCEPTION_INFO, "Texture id limit exceeded");
        }

        Texture& tex = textures[id];
        tex.location = std::move(location);
        glCreateTextures(GL_TEXTURE_2D, 1, &tex.glID);

        glTextureParameteri(tex.glID, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_NEAREST);
        glTextureParameteri(tex.glID, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        std::vector<std::uint8_t> data = JNI::loadBinaryFile(tex.location);
        int x;
        int y;
        std::uint8_t* imageData =
                stbi_load_from_memory(data.data(), data.size(), &x, &y, nullptr, 4);

        int maxSize = std::max(x, y);
        int levels = (int) std::floor(std::log2(maxSize)) + 1;

        glTextureStorage2D(tex.handle, levels, GL_RGBA8, x, y);
        glTextureSubImage2D(tex.glID, 0, 0, 0, x, y, GL_RGBA, GL_UNSIGNED_INT_8_8_8_8, imageData);
        glGenerateTextureMipmap(tex.glID);

        stbi_image_free(imageData);

        tex.handle = glGetTextureHandleARB(tex.glID);

        // yes i know im on the secondary thread right now,
        // this makes it so that it takes care of all the textures currently enqueued to load before updating the buffer
        secondaryQueue->enqueue([]() {
            updateBuffer();
        });

        return id;
    }

    void reloadTextures() {
        ROGUELIB_STACKTRACE
        for (auto& tex : textures) {
            tertiaryQueue->enqueue([&]() {
                std::vector<std::uint8_t> data = JNI::loadBinaryFile(tex.location);

                int x;
                int y;
                std::uint8_t* imageData =
                        stbi_load_from_memory(data.data(), data.size(), &x, &y, nullptr, 4);

                Event e = secondaryQueue->enqueue(boost::bind<void>([&](int x, int y, std::uint8_t* data) {
                    int glX;
                    int glY;
                    glGetTextureLevelParameteriv(tex.glID, 0, GL_TEXTURE_WIDTH, &glX);
                    glGetTextureLevelParameteriv(tex.glID, 0, GL_TEXTURE_HEIGHT, &glY);

                    if (x != glX || y != glY) {
                        throw Exceptions::InvalidArgument(ROGUELIB_EXCEPTION_INFO,
                                                          "Texture size mismatch. "
                                                          "Expected " + std::to_string(glX) + ":" + std::to_string(glY)
                                                          + " Got " + std::to_string(x) + ":" + std::to_string(y));
                    }

                    glTextureSubImage2D(tex.glID, 0, 0, 0, x, y, GL_RGBA, GL_UNSIGNED_INT_8_8_8_8, data);
                    glGenerateTextureMipmap(tex.glID);
                }, x, y, imageData));

                tertiaryQueue->enqueue([=]() {
                    stbi_image_free(imageData);
                }, {e});
            });
        }
    }

    void activateTexture(std::uint32_t id) {
        ROGUELIB_STACKTRACE
        if(id == -1){
            return;
        }
        if (textures[id].uses++ == 0) {
            glMakeTextureHandleResidentARB(textures[id].handle);
        }
    }

    void inactivateTexture(std::uint32_t id) {
        ROGUELIB_STACKTRACE
        if(id == -1){
            return;
        }
        if (--textures[id].uses == 0) {
            glMakeTextureHandleNonResidentARB(textures[id].handle);
        }
    }

    // ahh yes, binding bindless textures
    // this is actually just setting a uniform
    // well, two actually, as it updates the id limit too
    void bind(std::uint32_t program) {
        if(program == 0){
            glGetIntegerv(GL_CURRENT_PROGRAM, reinterpret_cast<GLint*>(&program));
        }
        // oh you bet im using a bindless TBO for the array of texture handles,
        glProgramUniformHandleui64ARB(program, 16, TBOTextureHandle);
        glProgramUniform1ui(program, 17, GLSLidLimit);
    }

    void updateBuffer() {
        if (TBOsize < textures.size()) {
            // aight, expandong time
            GLuint oldTBO = TBO;
            GLuint newTBO;
            glCreateBuffers(1, &newTBO);
            std::size_t newTBOsize;
            do {
                newTBOsize = TBOsize * 2;
                if (newTBOsize == 0) {
                    newTBOsize = 1;
                }
            } while (newTBOsize < textures.size());

            glNamedBufferStorage(newTBO, newTBOsize * sizeof(GLuint64), nullptr, GL_DYNAMIC_STORAGE_BIT);

            glCopyNamedBufferSubData(oldTBO, newTBO, 0, 0, TBOsize * sizeof(GLuint64));


            GLuint oldTBOTexture = TBOTexture;
            GLuint newTBOTexture;
            glCreateTextures(GL_TEXTURE_BUFFER, 1, &newTBOTexture);
            glTextureBuffer(newTBOTexture, GL_RG32UI, newTBO);
            GLuint64 newTBOTextureHandle = glGetTextureHandleARB(newTBOTexture);
            glMakeTextureHandleResidentARB(newTBOTextureHandle);

            // swap them out now, they are at this point equivalent, but one is bigger
            TBO = newTBO;
            TBOsize = newTBOsize;
            TBOTexture = newTBOTexture;
            TBOTextureHandle = newTBOTextureHandle;

            primaryQueue->enqueue([=]() {
                // oh yea, do need to update the texture buffer, but i need it to not be in flight client side when i do this
                // so, primary thread it is
                GLuint TBOToDelete = oldTBO;
                GLuint TBOTextureToDelete = oldTBOTexture;
                glDeleteBuffers(1, &TBOToDelete);
                glDeleteTextures(1, &TBOTextureToDelete);
            });
        }
        if (textures.size() > textureHandles.size()) {
            std::size_t oldcount = textureHandles.size();
            for (int i = textureHandles.size(); i < textures.size(); ++i) {
                textureHandles.emplace_back(textures[i].handle);
            }
            glNamedBufferSubData(TBO, oldcount * sizeof(GLuint64), (textures.size() - oldcount) * sizeof(GLuint64),
                                 textureHandles.data() + oldcount);
            std::uint32_t newGLSLidLimit = textureHandles.size();
            primaryQueue->enqueue([=](){
                GLSLidLimit = newGLSLidLimit;
            });
        }
    }
}