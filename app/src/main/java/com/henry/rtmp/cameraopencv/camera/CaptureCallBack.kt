package com.henry.rtmp.cameraopencv.camera
import android.graphics.Bitmap
interface CaptureCallBack {
    fun onSucceed(bitmap: Bitmap)
    fun onFailed(e: Throwable)
}