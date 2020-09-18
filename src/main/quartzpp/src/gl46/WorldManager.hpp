#pragma once

#include "common/QuartzBlockRenderInfo.hpp"
#include <vector>
#include <glm/glm.hpp>

namespace Phosphophyllite::Quartz::GL46::World {

    void startup();

    void shutdown();

    void setDrawInfo(std::vector<QuartzBlockRenderInfo> infos);

    void draw();
}