#pragma once

#include <vector>
#include <glm/glm.hpp>
#include <RogueLib/Logging/Log.hpp>

namespace Phosphophyllite::Quartz::GL46 {

    extern glm::dvec3 playerPosition;
    extern glm::dvec3 playerOffset;
    extern glm::mat4 projectionMatrix;
    extern glm::mat4 modelViewMatrix;
    extern glm::mat4 modelViewProjectionMatrix;
//    extern glm::mat4 inverseModelViewProjectionMatrix;


    void GLSetup(void* glfwGetProcAddress);

    void GLShutdown();

    void draw(glm::dvec3 position, glm::dvec2 looking);

    void setDrawInfo(std::vector<std::byte> buffer);

    void captureSecondaryThread();

    std::vector<std::byte> loadTextures(std::vector<std::byte> buffer);

    void reloadShaders();
}