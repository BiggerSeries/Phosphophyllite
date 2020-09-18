#pragma once

#include <RogueLib/Threading/WorkQueue.hpp>

namespace Phosphophyllite::Quartz {

    extern RogueLib::Threading::WorkQueue* primaryQueue;
    extern RogueLib::Threading::WorkQueue* secondaryQueue;
    extern RogueLib::Threading::WorkQueue* tertiaryQueue;

    void initQueues();
    void shutdownQueues();

    void captureSecondaryThread();
}