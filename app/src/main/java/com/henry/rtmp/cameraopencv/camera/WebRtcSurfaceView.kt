package com.henry.rtmp.cameraopencv.camera
import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.util.Log
import android.view.TextureView

class WebRtcSurfaceView(context: Context,attr: AttributeSet): TextureView(context, attr),
    TextureView.SurfaceTextureListener{
    lateinit var camera2Manager: Camera2Manager
    init {
        surfaceTextureListener=this
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        Log.e("我的摄像头","onSurfaceTextureAvailable")
        camera2Manager = Camera2Manager(context!!,surface)
        camera2Manager.openCamera()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
        Log.e("我的摄像头","onSurfaceTextureSizeChanged")
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        return false
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
    }
}