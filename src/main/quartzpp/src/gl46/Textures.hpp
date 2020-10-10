#pragma once

#include <string>
#include <vector>

namespace Phosphophyllite::Quartz::GL46::Textures {

    void startup();

    void shutdown();

//    std::uint32_t _loadTexture(const std::string& location);

    std::vector<std::uint32_t> loadTextures(std::vector<std::string> locations);

    void activateTexture(std::uint32_t id);

    void inactivateTexture(std::uint32_t id);

    void bind(std::uint32_t program);
}