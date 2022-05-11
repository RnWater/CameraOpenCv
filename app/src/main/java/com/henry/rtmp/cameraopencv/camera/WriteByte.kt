package com.henry.rtmp.cameraopencv.camera

import android.os.Environment
import android.util.Log
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.lang.StringBuilder
import kotlin.experimental.and

object WriteByte {
    //webrtc   适合视频通话  不适合短视频   直播
    //    H264  H265   webrtc
    //    david    H265 难度
    open fun writeContent(array: ByteArray): String? {
        val HEX_CHAR_TABLE = charArrayOf(
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
        )
        val sb = StringBuilder()
        for (b in array) {
            sb.append(HEX_CHAR_TABLE[(b.toInt() and 0xf0) shr 4])
            sb.append(HEX_CHAR_TABLE[b.toInt() and 0x0f])
        }
        var writer: FileWriter? = null
        try {
            // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            writer = FileWriter(
                Environment.getExternalStorageDirectory().toString() + "/codec.txt",
                true
            )
            writer.write(sb.toString())
            writer.write("\n")
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                writer?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return sb.toString()
    }

    fun writeBytes(array: ByteArray?) {
        var writer: FileOutputStream? = null
        try {
            // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            writer = FileOutputStream(
                Environment.getExternalStorageDirectory().toString() + "/codec.h264", true
            )
            writer.write(array)
            writer.write('\n'.toInt())
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                writer?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}