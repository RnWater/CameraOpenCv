package com.henry.rtmp.cameraopencv.camera
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import androidx.core.app.ActivityCompat
import android.media.*
import java.nio.ByteBuffer
import android.graphics.ImageFormat
import android.media.Image.Plane

/**
 * 相机的使用管理
 *  1：后置摄像头是逆时针横着放置的
 *  2：前置摄像头数据是左右镜像的 所以再保存的时候需要左右翻转
 */
class Camera2Manager(var context: Context, var captureTexture: SurfaceTexture) {
    private lateinit var mediaCodec: MediaCodec
    private val tag = Camera2Manager::class.java.simpleName
    private var handlerThread = HandlerThread("Camera2Manager")
    private var handler: Handler? = null
    private var mCameraId = "0"//初始为后置摄像头
    private var cameraManager: CameraManager? = null//获取相机的一些参数信息和打开相机的管理类
    private var captureSession: CameraCaptureSession? = null//用来向相机设备发送获取图像的请求
    private var imageReader: ImageReader? = null//获取屏幕渲染数据 可以搭配MediaProjectionManager录屏一起使用
    private var previewWidth = 0//图片预览宽
    private var previewHeight = 0//图片预览高
    private var cameraDevice: CameraDevice? = null
    private var captureCallBack: CaptureCallBack? = null
    private var isFrontCamera: Boolean = false//前置摄像头的判断
    private var phoneDegree = 0;//手机的旋转角度

    init {
        System.loadLibrary("cameraopencv")
        handlerThread.start()//启动工作线程
        handler = Handler(handlerThread.looper)
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        mCameraId = getCamera()[0]
        initDefaultPreviewSize()
        startOrientationListener()
        initCodec()
    }
    private fun initCodec() {
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc")
            var format = MediaFormat.createVideoFormat(
                MediaFormat.MIMETYPE_VIDEO_AVC,
                previewHeight,
                previewWidth
            ).apply {
                setInteger(
                    MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
                )//需要nv12的数据
                setInteger(MediaFormat.KEY_BIT_RATE, 4000_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, 15)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
                setInteger(MediaFormat.KEY_ROTATION, 90);
            }
            mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            mediaCodec.start()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 主要是监听手机旋转角度来保证所拍摄的照片始终是正向的
     */
    private fun startOrientationListener() {
        val mOrEventListener: OrientationEventListener =
            object : OrientationEventListener(context) {
                override fun onOrientationChanged(orientation: Int) {
                    if (((orientation >= 0) && (orientation <= 45)) || (orientation > 315) && (orientation <= 360)) {
                        phoneDegree = 0;
                    } else if ((orientation > 45) && (orientation <= 135)) {
                        phoneDegree = 90;
                    } else if ((orientation > 135) && (orientation <= 225)) {
                        phoneDegree = 180;
                    } else if ((orientation > 225) && (orientation <= 315)) {
                        phoneDegree = 270;
                    }
                }
            }
        mOrEventListener.enable()
    }

    /**
     * 获取CameraId 一台设备会有多个摄像头
     */
    private fun getCamera(): Array<out String> {
        return cameraManager!!.cameraIdList
    }

    /**
     * 获取当前camera的支持预览大小 可以供用户选择分辨率不过一般给与最大分辨率拍摄
     */
    private fun getPreviewSize(): Array<out Size>? {
        var characteristics = cameraManager?.getCameraCharacteristics(mCameraId)
        val configs = characteristics?.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        return configs!!.getOutputSizes(SurfaceTexture::class.java)
    }

    /**
     * 根据cameraId打开指定的摄像头
     */
    fun openCamera() {
        setSurface(Surface(captureTexture))
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createImageReader()
        cameraManager!!.openCamera(mCameraId, cameraStateCallback, handler)
    }

    /**
     *  打开Camera的状态回调
     */
    private var cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(device: CameraDevice) {
            cameraDevice = device
            createCaptureSession()
        }

        override fun onDisconnected(device: CameraDevice) {
            device.close()
        }

        override fun onError(device: CameraDevice, p1: Int) {
            device.close()
            if (p1 == ERROR_CAMERA_IN_USE) {

            }
        }
    }

    /**
     * 创建CameraCaptureSession的状态回调
     */
    private var captureSessionCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            captureSession = session
            var captureSessionRequest =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            captureSessionRequest.apply {
//                addTarget(Surface(captureTexture))
                addTarget(imageReader!!.surface)
                // 设置自动对焦模式
                set(
                    CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                )
                // 设置自动曝光模式
                set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
            }
            Log.e("我的camera状态", "onConfigured")
            var request = captureSessionRequest.build()


            captureSession!!.setRepeatingRequest(request, null, handler)

        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
        }
    }

    /**
     * ImageReader获取到数据时的回调
     */
    private var imageAvailableListener = ImageReader.OnImageAvailableListener {
//        if (capture) {
//            capture=false
        Log.e("我的摄像机","正在执行")
        var image = it.acquireLatestImage()
        if (image != null) {
            var convertPlanes2NV21 = yuv420ToNv21(image)
            postData(convertPlanes2NV21,image.width,image.height,mCameraId.toInt())
            image.close()
        }
    }
    private fun convertPlanes(
        width: Int,
        height: Int,
        yPlane: ByteBuffer,
        uPlane: ByteBuffer,
        vPlane: ByteBuffer
    ): ByteArray? {
        val totalSize = width * height * 2
        val nv21Buffer = ByteArray(totalSize)
        var len = yPlane.capacity()
        yPlane[nv21Buffer, 0, len]
        var capacity = uPlane.capacity()
        uPlane[nv21Buffer, len, capacity]
        len += capacity
        var capacityV = vPlane.capacity()
        vPlane[nv21Buffer, len, capacityV]
        return nv21Buffer
    }

    fun yuv420ToNv21(image: Image): ByteArray? {
        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val size = image.width * image.height
        val nv21 = ByteArray(size * 3 / 2)
        yBuffer[nv21, 0, ySize]
        vBuffer[nv21, ySize, vSize]
        val u = ByteArray(uSize)
        uBuffer[u]

        //每隔开一位替换V，达到VU交替
        var pos = ySize + 1
        for (i in 0 until uSize) {
            if (i % 2 == 0) {
                nv21[pos] = u[i]
                pos += 2
            }
        }
        return nv21
    }

    /**
     * 更改预览，本质上是重新创建CameraCaptureSession和ImageReader
     */
    fun changePreviewSize(width: Int, height: Int) {
        previewWidth = width
        previewHeight = height
        if (cameraDevice != null) {
            createImageReader()
            createCaptureSession()
        }
    }

    /**
     * 创建CameraCaptureSession
     */
    private fun createCaptureSession() {
        captureTexture!!.setDefaultBufferSize(previewWidth,previewHeight);//设置SurfaceTexture缓冲区大小
        if (captureSession != null) {
            captureSession!!.close()
            captureSession = null
        }
        cameraDevice!!.createCaptureSession(
            listOf(
                (imageReader!!.surface)
            ), captureSessionCallback, handler
        )
    }
    /**
     * 创建ImageReader
     */
    private fun createImageReader() {
        if (imageReader != null) {
            imageReader!!.close()
            imageReader = null
        }
        imageReader =
            ImageReader.newInstance(previewWidth,previewHeight, ImageFormat.YUV_420_888, 2)
        imageReader!!.setOnImageAvailableListener(imageAvailableListener, handler)
    }

    /**
     * 重新打开一个相机 打开之前要先做一下清理
     * 每次切换摄像头都需要重新创建
     */
    fun openCamera(cameraId: String) {
        if (cameraDevice != null) {
            releaseCamera()
        }
        mCameraId = cameraId
        isFrontCamera = when (cameraId) {
            "0" -> false
            else -> true
        }
        initDefaultPreviewSize()
        openCamera()
    }

    /**
     * 获取默认的预览尺寸 取第一个尺寸的默认最大
     */
    private fun initDefaultPreviewSize() {
        var previewSize = getPreviewSize()
        for (i in 0 until previewSize?.size!!) {
            var width = previewSize[i].width
            var height = previewSize[i].height
            Log.e("我的预览尺寸$i", "$width  ----  $height")
        }
        var size = previewSize!![0]

        previewWidth = size.width
        previewHeight = size.height
        Log.e("我的视频宽高","宽度=$previewWidth  高度=$previewHeight")
    }

    var capture: Boolean = false

    /**
     * 拍照
     */
    fun capturePic(captureCallBack: CaptureCallBack?) {
        if (cameraDevice == null) {
            return
        }
        this.captureCallBack = captureCallBack
        try {
            var requestBuilder =
                cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            requestBuilder.addTarget(imageReader!!.surface)
            requestBuilder.addTarget(Surface(captureTexture))
            var request = requestBuilder.build()
            captureSession!!.setRepeatingRequest(request, null, handler)
            capture = true
        } catch (e: Exception) {
        }
    }

    /**
     * 释放相机
     */
    fun releaseCamera() {
        captureSession!!.close()
        cameraDevice!!.close()
    }

    /**
     * 释放handlerThread
     */
    fun destroy() {
        handlerThread.quit()
    }
    external fun setSurface(surface: Surface)
    external fun postData(data: ByteArray?, w: Int, h: Int, cameraId: Int)
}