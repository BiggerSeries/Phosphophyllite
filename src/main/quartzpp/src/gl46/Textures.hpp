#pragma once

#include <string>

namespace Phosphophyllite::Quartz::GL46::Textures {

    void startup();

    void shutdown();

    std::uint32_t loadTexture(std::string location);

    void activateTexture(std::uint32_t id);

    void inactivateTexture(std::uint32_t id);

    void bind(std::uint32_t program);
}