#include "jni.hpp"
#include "jni.h"
#include "JNI/net_roguelogix_phosphophyllite_quartz_internal_rendering_gl46cpp_JNI.h"
#include <gl46/GL46.hpp>

#include <csignal>
#include <cstring>
#include <iostream>

extern "C" {

JNIEXPORT void JNICALL Java_net_roguelogix_phosphophyllite_quartz_internal_rendering_gl46cpp_JNI_setupGL
        (JNIEnv* env, jclass, jlong getProcAddress) {
    Phosphophyllite::Quartz::GL46::GLSetup((void*) getProcAddress);
}

JNIEXPORT void JNICALL
Java_net_roguelogix_phosphophyllite_quartz_internal_rendering_gl46cpp_JNI_shutdownGL(JNIEnv*, jclass) {
    Phosphophyllite::Quartz::GL46::GLShutdown();
}

JNIEXPORT void JNICALL
Java_net_roguelogix_phosphophyllite_quartz_internal_rendering_gl46cpp_JNI_drawGL(JNIEnv*, jclass, jdouble x, jdouble y,
                                                                                 jdouble z,
                                                                                 jdouble yaw, jdouble pitch) {
    Phosphophyllite::Quartz::GL46::draw({x, y, z}, {yaw, pitch});
}

JNIEXPORT void JNICALL
Java_net_roguelogix_phosphophyllite_quartz_internal_rendering_gl46cpp_JNI_updateBlockRenderInfo(JNIEnv* env, jclass,
                                                                                                jobject directBuffer) {
    std::vector<std::byte> buffer{};
    buffer.resize(env->GetDirectBufferCapacity(directBuffer));
    std::memcpy(buffer.data(), env->GetDirectBufferAddress(directBuffer), buffer.size());
    Phosphophyllite::Quartz::GL46::setDrawInfo(buffer);
}
JNIEXPORT void JNICALL Java_net_roguelogix_phosphophyllite_quartz_internal_rendering_gl46cpp_JNI_loadTextures
        (JNIEnv* env, jclass, jobject input, jobject output) {

    std::vector<std::byte> inputBuffer{};
    inputBuffer.resize(env->GetDirectBufferCapacity(input));
    std::memcpy(inputBuffer.data(), env->GetDirectBufferAddress(input), inputBuffer.size());

    auto outputBuffer = Phosphophyllite::Quartz::GL46::loadTextures(inputBuffer);

    auto capacity = env->GetDirectBufferCapacity(output);
    if (capacity < outputBuffer.size()) {
        if(capacity >= 8) {
            auto* outputPtr = static_cast<uint64_t*>(env->GetDirectBufferAddress(output));
            *outputPtr = 0;
        }
        return;
    }
    std::memcpy(env->GetDirectBufferAddress(output), outputBuffer.data(), outputBuffer.size());
}

void Java_net_roguelogix_phosphophyllite_quartz_internal_rendering_gl46cpp_JNI_reloadShaders(JNIEnv*, jclass) {
    Phosphophyllite::Quartz::GL46::reloadShaders();
}

}

namespace Phosphophyllite::Quartz::JNI {
    JavaVM* vm = nullptr;
    thread_local JNIEnv* env = nullptr;
    jclass JNIclass;

    jmethodID loadTextFileID;
    jmethodID loadBinaryFileID;

    void attachThread() {
        if (vm) {
            vm->AttachCurrentThreadAsDaemon((void**) &env, nullptr);
        }
    }

    void detachThread() {
        if (vm) {
            vm->DetachCurrentThread();
        }
    }

    std::string loadTextFile(std::string resourceLocation) {
        if (!env) {
            resourceLocation.replace(resourceLocation.find(':'), 1, "/");
            resourceLocation = "./resources/assets/" + resourceLocation;
            std::ifstream instream;
            instream.open(resourceLocation);
            if (!instream.is_open()) {
                throw std::runtime_error("Unable to read file: " + resourceLocation);
            }
            std::stringstream stringstream;
            stringstream << instream.rdbuf();
            return stringstream.str();
        }
        jstring jResourceLocation = env->NewStringUTF(resourceLocation.data());
        auto retObject = reinterpret_cast<jstring>(env->CallStaticObjectMethod(JNIclass, loadTextFileID,
                                                                               jResourceLocation));
        env->DeleteLocalRef(jResourceLocation);

        if (retObject == nullptr) {
            throw std::runtime_error("Unable to read file: " + resourceLocation);
        }

        const char* jStringChars = env->GetStringUTFChars(retObject, nullptr);
        std::string retString(jStringChars);
        env->ReleaseStringUTFChars(retObject, jStringChars);
        env->DeleteLocalRef(retObject);
        return retString;
    }

    std::vector<std::uint8_t> loadBinaryFile(std::string resourceLocation) {
        if (!env) {
            resourceLocation.replace(resourceLocation.find(':'), 1, "/");
            resourceLocation = "./resources/assets/" + resourceLocation;
            std::ifstream instream;
            instream.open(resourceLocation, std::ios::binary | std::ios::in | std::ios::ate);
            if (!instream.is_open()) {
                throw std::runtime_error("Unable to read file: " + resourceLocation);
            }
            std::vector<std::uint8_t> vec;
            vec.resize(instream.tellg());
            instream.seekg(0);
            instream.read(reinterpret_cast<char*>(vec.data()), vec.size());
            return vec;
        }
        jstring jResourceLocation = env->NewStringUTF(resourceLocation.data());
        auto retObject = reinterpret_cast<jstring>(env->CallStaticObjectMethod(JNIclass, loadBinaryFileID,
                                                                               jResourceLocation));
        env->DeleteLocalRef(jResourceLocation);

        if (retObject == nullptr) {
            return {};
        }

        std::vector<std::uint8_t> vector;

        std::size_t size = env->GetDirectBufferCapacity(retObject);
        vector.resize(size);
        std::memcpy(vector.data(), env->GetDirectBufferAddress(retObject), size);

        return vector;
    }
}

using namespace Phosphophyllite::Quartz;
extern "C"
JNIEXPORT void JNICALL
Java_net_roguelogix_phosphophyllite_quartz_internal_rendering_gl46cpp_JNI_captureSecondaryThread(JNIEnv* env, jclass) {
    JNI::env = env;
    GL46::captureSecondaryThread();
}

using namespace Phosphophyllite::Quartz::JNI;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNI::vm = vm;
    vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_8);
    JNIclass = env->FindClass("Lnet/roguelogix/phosphophyllite/quartz/internal/rendering/jni/JNI;");
    loadTextFileID = env->GetStaticMethodID(JNIclass, "loadTextFile", "(Ljava/lang/String;)Ljava/lang/String;");
    loadBinaryFileID = env->GetStaticMethodID(JNIclass, "loadBinaryFile",
                                              "(Ljava/lang/String;)Lsun/nio/ch/DirectBuffer;");
    std::cout << "Quartz++ loaded" << std::endl;
    return JNI_VERSION_1_8;
}

