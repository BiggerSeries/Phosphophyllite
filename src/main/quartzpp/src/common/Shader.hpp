#pragma once

#include <memory>
#include <string>

namespace Phosphophyllite::Quartz {

    class Shader {
    public:
        class IMPL;

    private:
        std::shared_ptr<IMPL> impl;
    public:
        Shader();

        Shader(std::string name);

        void reload();

        void bind();

        std::uint32_t handle();
    };

    void reloadAll();
}

