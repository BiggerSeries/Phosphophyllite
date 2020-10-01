#include <RogueLib/Logging/Log.hpp>
#include <glad/glad.h>
#include <GLFW/glfw3.h>
#include <RogueLib/Threading/Thread.hpp>
#include <gl46/WorldManager.hpp>
#include <glm/gtc/type_ptr.hpp>

#include "gl46/GL46.hpp"
#include "common//Queues.hpp"

using namespace Phosphophyllite::Quartz;
using namespace Phosphophyllite::Quartz::GL46;


int main() {
    RogueLib::Logging::Log log;

    glfwInit();

//    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
//    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6);
//    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
//    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_COMPAT_PROFILE);
    GLFWwindow* window = glfwCreateWindow(1920, 1080, "test", nullptr, nullptr);
    glfwMakeContextCurrent(window);
//    gladLoadGLLoader(reinterpret_cast<GLADloadproc>(glfwGetProcAddress));

    bool secondaryThreadRunning = true;

    GLSetup((void*) glfwGetProcAddress);

    auto callbac = [](GLenum source,
                      GLenum type,
                      GLuint id,
                      GLenum severity,
                      GLsizei length,
                      const GLchar* message,
                      const void* userParam) {
        RogueLib::Logging::warning({message});
    };
    glDebugMessageCallback(callbac, nullptr);

    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
    GLFWwindow* secondaryWindow = glfwCreateWindow(800, 600, "test1", nullptr, window);
    RogueLib::Threading::Thread thread([&]() {
        glfwMakeContextCurrent(secondaryWindow);
        GL46::captureSecondaryThread();
        glfwMakeContextCurrent(nullptr);
        secondaryThreadRunning = false;
    });
    thread.start();


    auto id = loadTexture("phosphophyllite:textures/block/phosphophyllite_ore.png");
    QuartzBlockRenderInfo renderInfo;
    renderInfo.textureIDWest = id;
    renderInfo.textureIDEast = id;
    renderInfo.textureIDBottom = id;
    renderInfo.textureIDTop = id;
    renderInfo.textureIDSouth = id;
    renderInfo.textureIDNorth = id;
    World::setDrawInfo({renderInfo});
    renderInfo.x = 4;
    World::setDrawInfo({renderInfo});
    renderInfo.x = 16;
    World::setDrawInfo({renderInfo});

    while (!glfwWindowShouldClose(window) && secondaryThreadRunning) {
        glfwPollEvents();
        glEnable(GL_DEPTH_TEST);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

//        glDisable(GL_CULL_FACE);
        draw({0, 0, -10}, {0, 0});

//        glDisable(GL_DEPTH_TEST);

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadMatrixf(glm::value_ptr(modelViewProjectionMatrix));
        glBegin(GL_TRIANGLES);
        glVertex3f(-0.5f, -0.5f, -1);
        glVertex3f(+0.0f, +0.5f, -1);
        glVertex3f(+0.5f, -0.5f, -1);
        glEnd();
        glPopMatrix();

        glfwSwapBuffers(window);
    }

    if (secondaryThreadRunning) {
        GLShutdown();
    }


    glfwMakeContextCurrent(nullptr);
    glfwDestroyWindow(window);
    glfwTerminate();
}