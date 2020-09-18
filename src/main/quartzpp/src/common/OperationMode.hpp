#pragma once

namespace Phosphophyllite::Quartz {

    enum class OperationMode{
        UNKNOWN = 0,
        GL46 = 460,
        GL33 = 330,
    };

    extern OperationMode opMode;
}


