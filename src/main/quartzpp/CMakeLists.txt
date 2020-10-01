cmake_minimum_required(VERSION 3.15)
project(quartzpp)

set(CMAKE_CXX_STANDARD 17)

execute_process(WORKING_DIRECTORY "${CMAKE_CURRENT_LIST_DIR}" COMMAND javac "-h" "./src/common/JNI" "../java/net/roguelogix/phosphophyllite/quartz/internal/gl46cpp/JNI.java")

file(GLOB_RECURSE LIB_SRC ${CMAKE_CURRENT_LIST_DIR}/src/*.c* ${CMAKE_CURRENT_LIST_DIR}/src/*.h*)

add_library(quartzpp SHARED ${LIB_SRC})
target_include_directories(quartzpp PUBLIC include)
target_include_directories(quartzpp PUBLIC src)

find_package(JNI REQUIRED)
target_include_directories(quartzpp PUBLIC ${JNI_INCLUDE_DIRS})

add_subdirectory(RogueLib)
target_link_libraries(quartzpp PUBLIC RogueLib_ROBN RogueLib_Logging RogueLib_Threading)

add_executable(test test.cpp)
target_link_libraries(test PUBLIC quartzpp dl glfw)