#include "jni.hpp"
#include "jni.h"
#include "JNI/net_roguelogix_phosphophyllite_quartz_client_gl46cpp_JNI.h"
#include <gl46/GL46.hpp>

#include <cstring>
#include <iostream>

extern "C" {

JNIEXPORT void JNICALL Java_net_roguelogix_phosphophyllite_quartz_client_gl46cpp_JNI_setupGL
        (JNIEnv* env, jclass, jlong getProcAddress) {
    Phosphophyllite::Quartz::GL46::GLSetup((void*) getProcAddress);
}

JNIEXPORT void JNICALL Java_net_roguelogix_phosphophyllite_quartz_client_gl46cpp_JNI_shutdownGL(JNIEnv*, jclass) {
    Phosphophyllite::Quartz::GL46::GLShutdown();
}

JNIEXPORT void JNICALL
Java_net_roguelogix_phosphophyllite_quartz_client_gl46cpp_JNI_drawGL(JNIEnv*, jclass, jdouble x, jdouble y, jdouble z,
                                                                     jdouble yaw, jdouble pitch) {
    Phosphophyllite::Quartz::GL46::draw({x, y, z}, {yaw, pitch});
}

JNIEXPORT void JNICALL
Java_net_roguelogix_phosphophyllite_quartz_client_gl46cpp_JNI_updateBlockRenderInfo(JNIEnv* env, jclass,
                                                                                    jobject directBuffer) {
    std::vector<std::byte> buffer{};
    buffer.resize(env->GetDirectBufferCapacity(directBuffer));
    std::memcpy(buffer.data(), env->GetDirectBufferAddress(directBuffer), buffer.size());
    Phosphophyllite::Quartz::GL46::setDrawInfo(buffer);
}
JNIEXPORT jint JNICALL Java_net_roguelogix_phosphophyllite_quartz_client_gl46cpp_JNI_loadTexture
        (JNIEnv* env, jclass, jstring string) {
    auto* characters = env->GetStringUTFChars(string, nullptr);

    auto id = Phosphophyllite::Quartz::GL46::loadTexture({characters});

    env->ReleaseStringUTFChars(string, characters);

    return id;
}

}

namespace Phosphophyllite::Quartz::JNI {
    JavaVM* vm;
    thread_local JNIEnv* env;
    jclass JNIclass;

    jmethodID loadTextFileID;
    jmethodID loadBinaryFileID;

    void attachThread() {
        vm->AttachCurrentThreadAsDaemon((void**) &env, nullptr);
    }

    void detachThread() {
        vm->DetachCurrentThread();
    }

    std::string loadTextFile(std::string resourceLocation) {
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

JNIEXPORT void JNICALL
Java_net_roguelogix_phosphophyllite_quartz_client_gl46cpp_JNI_captureSecondaryThread(JNIEnv* env, jclass) {
    JNI::env = env;
    GL46::captureSecondaryThread();
}

using namespace Phosphophyllite::Quartz::JNI;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNI::vm = vm;
    vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_8);
    JNIclass = env->FindClass("Lnet/roguelogix/phosphophyllite/quartz/client/jni/JNI;");
    loadTextFileID = env->GetStaticMethodID(JNIclass, "loadTextFile", "(Ljava/lang/String;)Ljava/lang/String;");
    loadBinaryFileID = env->GetStaticMethodID(JNIclass, "loadBinaryFile",
                                              "(Ljava/lang/String;)Lsun/nio/ch/DirectBuffer;");
    std::cout << "Quartz++ loaded" << std::endl;
    return JNI_VERSION_1_8;
}

