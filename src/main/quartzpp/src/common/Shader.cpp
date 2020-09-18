#include "Shader.hpp"
#include "Queues.hpp"
#include "OperationMode.hpp"
#include "jni.hpp"

#include <glad/glad.h>
#include <map>
#include <shared_mutex>
#include <RogueLib/Logging/Log.hpp>

using namespace RogueLib;
using namespace RogueLib::Threading;

namespace Phosphophyllite::Quartz {
    class Shader::IMPL {
        std::string location;
        std::string vertexShaderLocation;
        std::string fragmentShaderLocation;

        Event loadEvent;
        GLuint programHandle = 0;
        GLuint newHandle = 0;
    public:
        IMPL(std::string name);

        void reload();

        void bind();

        std::int32_t handle();
    };
}
namespace Phosphophyllite::Quartz {
    Shader::IMPL::IMPL(std::string name) {
        ROGUELIB_STACKTRACE
        location = "phosphophyllite:shaders/" + std::to_string(std::uint32_t(opMode)) + "/" + name;
        vertexShaderLocation = location + ".vert";
        fragmentShaderLocation = location + ".frag";
        reload();
    }

    void Shader::IMPL::reload() {
        ROGUELIB_STACKTRACE
        loadEvent = secondaryQueue->enqueue([&]() {
            ROGUELIB_LAMBDATRACE
            std::string vertexCode;
            std::string fragmentCode;
            try{
                ROGUELIB_RESTACKTRACE
                vertexCode = JNI::loadTextFile(vertexShaderLocation);
                ROGUELIB_RESTACKTRACE
                fragmentCode = JNI::loadTextFile(fragmentShaderLocation);
            } catch (std::runtime_error& e) {
                Logging::warning(e.what());
                if (programHandle == 0){
                    throw Exceptions::FatalFileNotFound(ROGUELIB_EXCEPTION_INFO, e.what());
                }
            }

            auto vertexShader = glCreateShader(GL_VERTEX_SHADER);
            auto fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);

            const char* vertexSource = vertexCode.c_str();
            const char* fragmentSource = fragmentCode.c_str();

            glShaderSource(vertexShader, 1, &vertexSource, nullptr);
            glShaderSource(fragmentShader, 1, &fragmentSource, nullptr);

            glCompileShader(vertexShader);
            glCompileShader(fragmentShader);

            GLint vertexStatus;
            glGetShaderiv(vertexShader, GL_COMPILE_STATUS, &vertexStatus);
            GLint fragmentStatus;
            glGetShaderiv(fragmentShader, GL_COMPILE_STATUS, &fragmentStatus);

            ROGUELIB_RESTACKTRACE
            if (vertexStatus != GL_TRUE || fragmentStatus != GL_TRUE) {
                if (vertexStatus != GL_TRUE) {
                    glGetShaderiv(vertexShader, GL_INFO_LOG_LENGTH, &vertexStatus);
                    std::vector<char> infoLog;
                    infoLog.resize(vertexStatus);
                    glGetShaderInfoLog(vertexShader, vertexStatus, nullptr, infoLog.data());
                    Logging::error("Vertex shader compilation failed!\n"
                                   "Location: " + vertexShaderLocation +
                                   "\n"
                                   "GL error log: \n" +
                                   std::string(infoLog.data()) +
                                   "\n");
                }
                if (fragmentStatus != GL_TRUE) {
                    glGetShaderiv(fragmentShader, GL_INFO_LOG_LENGTH, &fragmentStatus);
                    std::vector<char> infoLog;
                    infoLog.resize(fragmentStatus);
                    glGetShaderInfoLog(fragmentShader, fragmentStatus, nullptr, infoLog.data());
                    Logging::error("Fragment shader compilation failed!\n"
                                   "Location: " + fragmentShaderLocation +
                                   "\n"
                                   "GL error log: \n" +
                                   std::string(infoLog.data()) +
                                   "\n");
                }
                glDeleteShader(vertexShader);
                glDeleteShader(fragmentShader);

                if(programHandle == 0){
                    throw Exceptions::FatalInitFailure(ROGUELIB_EXCEPTION_INFO, "Shader compilation failed");
                }

                return;
            }

            auto program = glCreateProgram();
            glAttachShader(program, vertexShader);
            glAttachShader(program, fragmentShader);
            glLinkProgram(program);

            GLint programStatus;
            glGetProgramiv(program, GL_LINK_STATUS, &programStatus);
            if (programStatus != GL_TRUE) {
                glGetProgramiv(programStatus, GL_INFO_LOG_LENGTH, &programStatus);
                std::vector<char> infoLog;
                infoLog.resize(programStatus);
                glGetProgramInfoLog(program, programStatus, nullptr, infoLog.data());
                Logging::error("Fragment program compilation failed!\n"
                               "Location: " + location +
                               "\n"
                               "GL error log: \n" +
                               std::string(infoLog.data()) +
                               "\n");
                glDeleteProgram(program);
                glDeleteShader(vertexShader);
                glDeleteShader(fragmentShader);

                if(programHandle == 0){
                    throw Exceptions::FatalInitFailure(ROGUELIB_EXCEPTION_INFO, "Shader program linking failed");
                }

                return;
            }

            Logging::info("Program loaded from " + std::string(location));

//            glDetachShader(program, vertexShader);
//            glDetachShader(program, fragmentShader);

            glDeleteShader(vertexShader);
            glDeleteShader(fragmentShader);

            GLuint oldProgram = this->programHandle;
            programHandle = program;
            glDeleteProgram(oldProgram);

        });
    }

    void Shader::IMPL::bind() {
        ROGUELIB_STACKTRACE
        if (programHandle == 0) {
            loadEvent.wait();
            if (programHandle == 0) {
                throw Exceptions::FatalInitFailure(ROGUELIB_EXCEPTION_INFO, "Shader failed to load");
            }
        }
        glUseProgram(programHandle);
    }

    std::int32_t Shader::IMPL::handle() {
        return programHandle;
    }
}
namespace Phosphophyllite::Quartz {

    static std::map<std::string, std::shared_ptr<Shader::IMPL>> shaderCache;
    static std::shared_mutex cacheMutex;

    void reloadAll() {
        ROGUELIB_STACKTRACE
        for (auto& shader : shaderCache) {
            shader.second->reload();
        }
    }

    Shader::Shader() {
    }

    Shader::Shader(std::string name) {
        ROGUELIB_STACKTRACE
        std::shared_lock slk(cacheMutex);
        auto iter = shaderCache.find(name);
        if (iter != shaderCache.end()) {
            impl = iter->second;
            return;
        }

        slk.unlock();
        impl = std::make_shared<IMPL>(name);
        std::unique_lock ulk(cacheMutex);
        shaderCache.emplace(name, impl);
    }

    void Shader::reload() {
        ROGUELIB_STACKTRACE
        impl->reload();
    }

    void Shader::bind() {
        ROGUELIB_STACKTRACE
        impl->bind();
    }

    std::uint32_t Shader::handle() {
        return impl->handle();
    }
}