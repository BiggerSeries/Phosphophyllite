#pragma once

#include <vector>
#include <string>

namespace Phosphophyllite::Quartz::JNI {
    void attachThread();

    void detachThread();

    std::string loadTextFile(std::string resourceLocation);

    std::vector<std::uint8_t> loadBinaryFile(std::string resourceLocation);
}