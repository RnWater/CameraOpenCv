cmake_minimum_required(VERSION 3.10.2)
project("cameraopencv")
include_directories(include)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -L${CMAKE_SOURCE_DIR}/../../../libs/${ANDROID_ABI}")
add_library(
        cameraopencv
        SHARED
        native-lib.cpp)
find_library(
        log-lib
        log)
target_link_libraries(
        cameraopencv
        ${log-lib}
        opencv_java4
        jnigraphics
        android
)