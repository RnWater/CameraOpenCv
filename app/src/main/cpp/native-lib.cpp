#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>
#include <pthread.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <iostream>
#include <android/log.h>
#include <android/native_window_jni.h>
#include <android/log.h>
#include <opencv2/imgproc/types_c.h>
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,"摄像头操作",__VA_ARGS__)
using namespace cv;
ANativeWindow *window = 0;
extern "C"
JNIEXPORT void JNICALL
Java_com_henry_rtmp_cameraopencv_camera_Camera2Manager_setSurface(JNIEnv *env, jobject thiz,
                                                                  jobject surface) {
    if (window) {
        ANativeWindow_release(window);
        window = 0;
    }
//        渲染surface   --->window   --->windwo
    window = ANativeWindow_fromSurface(env, surface);
}
void UpdateFrameBuffer(ANativeWindow_Buffer *buf, uint8_t *src) {
    // src is either null: to blank the screen
    //     or holding exact pixels with the same fmt [stride is the SAME]
    uint8_t *dst = reinterpret_cast<uint8_t *> (buf->bits);
    uint32_t bpp;
    switch (buf->format) {
        case WINDOW_FORMAT_RGB_565:
            bpp = 2;
            break;
        case WINDOW_FORMAT_RGBA_8888:
        case WINDOW_FORMAT_RGBX_8888:
            bpp = 4;
            break;
        default:
            assert(0);
            return;
    }
    uint32_t stride, width;
    stride = buf->stride * bpp;
    width = buf->width * bpp;
    if (src) {
        for (auto height = 0; height < buf->height; ++height) {
            memcpy(dst, src, width);
            dst += stride, src += width;
        }
    } else {
        for (auto height = 0; height < buf->height; ++height) {
            memset(dst, 0, width);
            dst += stride;
        }
    }
}

int index = 0;
Mat gray;
Mat rgba;
ANativeWindow_Buffer buffer;
uint8_t *dstData;
extern "C"
JNIEXPORT void JNICALL
Java_com_henry_rtmp_cameraopencv_camera_Camera2Manager_postData(JNIEnv *env, jobject thiz,
                                                                jbyteArray data_, jint w, jint h,
                                                                jint cameraId) {
    jbyte *data = env->GetByteArrayElements(data_, NULL);
    Mat src(h + h / 2, w, CV_8UC1, data);
    cvtColor(src, src, COLOR_YUV2RGBA_NV21);
    if (cameraId == 1) {
        rotate(src, src, ROTATE_90_COUNTERCLOCKWISE);
        flip(src, src, 1);
    } else {
        //顺时针旋转90度
        rotate(src, src, ROTATE_90_CLOCKWISE);
    }
//    灰色
    cvtColor(src, gray, COLOR_RGBA2GRAY);
    //增强对比度 (直方图均衡)
    equalizeHist(gray, gray);
    cvtColor(gray,rgba, COLOR_GRAY2RGBA);
//    char p[100];
//    mkdir("/sdcard/camera2/", 0777);
//    sprintf(p, "/sdcard/camera2/%d.jpg", index++);
//    imwrite(p, result);
    if (window) {

        ANativeWindow_setBuffersGeometry(window, rgba.cols,rgba.rows, WINDOW_FORMAT_RGBA_8888);
//            缓冲区    得到
        if (ANativeWindow_lock(window, &buffer, 0)) {
            ANativeWindow_release(window);
            window = 0;
        }
        dstData  =   static_cast<uint8_t *>(buffer.bits);
        //  知道为什么*4   rgba
        int srclineSize = rgba.cols*4 ;
        int dstlineSize = buffer.stride *4;
        for (int i = 0; i < buffer.height; ++i) {
            memcpy(dstData+i*dstlineSize, rgba.data+i*srclineSize, srclineSize);
        }
        ANativeWindow_unlockAndPost(window);
    }
    src.release();
    gray.release();
    rgba.release();
    env->ReleaseByteArrayElements(data_, data, 0);
}
