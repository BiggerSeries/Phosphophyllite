#include <RogueLib/Logging/Log.hpp>
#include "Queues.hpp"
#include "jni.hpp"

namespace Phosphophyllite::Quartz {

    using namespace RogueLib::Threading;

    WorkQueue* primaryQueue = nullptr;
    WorkQueue* secondaryQueue = nullptr;
    WorkQueue* tertiaryQueue = nullptr;

    void initQueues() {
        ROGUELIB_STACKTRACE
        primaryQueue = new WorkQueue();
        secondaryQueue = new WorkQueue();
        tertiaryQueue = new WorkQueue();
        // TODO make the number of threads here configurable
        for (int i = 0; i < 4; ++i) {
            addQueueProcessingThread(*tertiaryQueue, []() {
                JNI::attachThread();
            }, []() {
                JNI::detachThread();
            });
        }
    }

    void shutdownQueues() {
        ROGUELIB_STACKTRACE
        WorkQueue* queue;
        queue = primaryQueue;
        primaryQueue = nullptr;
        delete queue;
        queue = secondaryQueue;
//        queue->enqueue([]() {}).wait();
        delete queue;
        delete tertiaryQueue;
    }

    void captureSecondaryThread() {
        ROGUELIB_STACKTRACE
        if (secondaryQueue != nullptr) {
            WorkQueue::Dequeue dequeue = secondaryQueue->dequeue();
            while (dequeue) {
                auto item = dequeue.dequeue();
                try {
                    item.process();
                } catch (std::exception& eVal) { // NOLINT(misc-throw-by-value-catch-by-reference)
                    std::exception* ePtr = &eVal;
                    if (dynamic_cast<RogueLib::Exceptions::FatalError*>(ePtr)) {
                        auto* e = dynamic_cast<RogueLib::Exceptions::FatalError*>(ePtr);
                        RogueLib::Logging::fatal("Secondary thread fatal exception rolled back");
                        RogueLib::Logging::fatal(std::string{e->printMsg()});
                        return;
                    } else if (dynamic_cast<RogueLib::Exceptions::Error*>(ePtr)) {
                        auto* e = dynamic_cast<RogueLib::Exceptions::Error*>(ePtr);
                        RogueLib::Logging::error("Secondary thread non-fatal exception rolled back");
                        RogueLib::Logging::error(std::string{e->printMsg()});
                    } else {
                        RogueLib::Logging::error("Secondary thread unknown exception rolled back");
                        RogueLib::Logging::error(std::string{ePtr->what()});
                    }

                }
            }
        }
    }
}

