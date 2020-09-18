
#include "GL46.hpp"
#include "common/jni.hpp"
#include <glad/glad.h>
#include <iostream>
#include <utility>
#include <glm/gtc/type_ptr.hpp>
#include <common/OperationMode.hpp>
#include <common/Queues.hpp>

#include "common/QuartzBlockRenderInfo.hpp"
#include "WorldManager.hpp"
#include "Textures.hpp"

namespace Phosphophyllite::Quartz::GL46 {

    glm::dvec3 playerPosition = {};
    glm::dvec3 playerOffset = {};
    glm::mat4 projectionMatrix = {};
    glm::mat4 modelViewMatrix = {};
    glm::mat4 modelViewProjectionMatrix = {};
    glm::mat4 inverseModelViewProjectionMatrix = {};

    using namespace RogueLib;
    using namespace RogueLib::Logging;

    void GLSetup(void* glfwGetProcAddress) {
        ROGUELIB_STACKTRACE

        initQueues();

        opMode = OperationMode::GL46;

        std::cout << "Hello from C++!" << std::endl;

        Log globalLog{std::string{"Phosphophyllite"}};
        globalLog = Log{std::string{"Quartz++"}}.addOutput(globalLog);
        OutputStreamOutput coutOutput{&std::cout, {}, false, false};
        globalLog.addOutput(coutOutput);
        Logging::setGlobalLog(globalLog);
        Logging::info("Global log setup");

        Log threadLog{std::string{"Primary Thread"}};
        threadLog.addOutput(globalLog);
        Logging::pushThreadLog(threadLog);
        Logging::info("Primary thread log setup");

        // by this point, MC will have already initialized GLFW, and this ptr is from LWJGL anyway

        if (!gladLoadGLLoader((GLADloadproc) glfwGetProcAddress)) {
            Logging::fatal("GLAD failed to load!");
            return;
        } else {
            Logging::info("GLAD loaded GL: " + std::to_string(GLVersion.major) + "." + std::to_string(GLVersion.minor));
        }

        Textures::startup();
        World::startup();
    }


    void GLShutdown() {
        ROGUELIB_STACKTRACE

        Textures::shutdown();
        World::shutdown();
        shutdownQueues();
    }


    void draw(glm::dvec3 position, glm::dvec2 looking) {
        ROGUELIB_STACKTRACE

        playerPosition = position;
        playerOffset = -position;

        glGetFloatv(GL_PROJECTION_MATRIX, glm::value_ptr(projectionMatrix));
        modelViewMatrix = glm::identity<glm::mat4>();
        modelViewMatrix *= glm::rotate(glm::identity<glm::mat4>(), (float) glm::radians(looking[1]),
                                       glm::vec3{1, 0, 0});
        modelViewMatrix *= glm::rotate(glm::identity<glm::mat4>(), (float) glm::radians(looking[0] + 180),
                                       glm::vec3{0, 1, 0});

        modelViewProjectionMatrix = projectionMatrix * modelViewMatrix;
//        inverseModelViewProjectionMatrix = glm::inverse(modelViewProjectionMatrix);

        World::draw();
    }

    void setDrawInfo(std::vector<std::byte> buffer) {
        ROGUELIB_STACKTRACE
        auto infos = RogueLib::ROBN::fromROBN<std::vector<QuartzBlockRenderInfo>>(std::move(buffer));
    }

    void captureSecondaryThread() {
        ROGUELIB_STACKTRACE
        // have the queue capture it
        Log threadLog{std::string{"Secondary Thread"}};
        threadLog.addOutput(*globalLog);
        Logging::pushThreadLog(threadLog);
        Logging::info("Secondary thread setup");
        Quartz::captureSecondaryThread();
        Logging::info("Secondary thread shutdown");
    }

    std::uint32_t loadTexture(std::string textureLocation) {
        return Textures::loadTexture(textureLocation);
    }
}


