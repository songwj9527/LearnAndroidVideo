package com.songwj.openvideo.mediacodec.encoder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecInfo.CodecCapabilities
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.util.Log
import android.view.Surface
import com.songwj.openvideo.mediacodec.muxer.MMuxer
import java.nio.ByteBuffer


/**
 * 视频编码器
 */

class VideoEncoder(muxer: MMuxer, width: Int, height: Int, isEncodeManually: Boolean) : BaseEncoder(muxer, width, height, isEncodeManually) {
//    private val TAG = "VideoEncoder"

    // 颜色空间 从编码视图的surface窗口获得
    protected var mSurface: Surface? = null
    // pts
    protected var pts: Long = 0

    init {
        TAG = "VideoEncoder"
    }

    // 是否手动编码，false，颜色空间 从编码视图的surface窗口获得；true，颜色空间 从外部获得，需要外部传入数据放到编码队列中去编码
    override fun encodeManually(): Boolean {
        return isEncodeManually
    }

    override fun createCodec(): MediaCodec {
        getSupportTypes()
        return MediaCodec.createEncoderByType(encodeType())
//        return MediaCodec.createByCodecName("OMX.google.h264.encoder")
    }

    private fun getSupportTypes() {
        val allMediaCodecLists = MediaCodecList(-1)
        var avcCodecInfo: MediaCodecInfo? = null
        for (mediaCodecInfo in allMediaCodecLists.codecInfos) {
            if (mediaCodecInfo.isEncoder) {
                val supportTypes = mediaCodecInfo.supportedTypes
                for (supportType in supportTypes) {
                    if (supportType == MediaFormat.MIMETYPE_VIDEO_AVC) {
                        avcCodecInfo = mediaCodecInfo
                        Log.d(TAG, "编码器名称:" + mediaCodecInfo.name + "  " + supportType)
                        val codecCapabilities =
                            avcCodecInfo.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)
                        val colorFormats = codecCapabilities.colorFormats
                        for (colorFormat in colorFormats) {
                            when (colorFormat) {
                                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV411Planar,
                                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV411PackedPlanar,
                                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
                                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar,
                                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
                                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar -> Log.d(
                                    TAG,
                                    "支持的格式::$colorFormat"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun encodeType(): String {
        return MediaFormat.MIMETYPE_VIDEO_AVC
    }

    override fun configEncoder(codec: MediaCodec) {
        Log.e(TAG, "configEncoder: width: $mWidth, height: $mHeight")
        if (mWidth <= 0 || mHeight <= 0) {
            throw IllegalArgumentException("Encode width or height is invalid, width: $mWidth, height: $mHeight")
        }
        val bitrate = 3 * mWidth * mHeight
        val outputFormat = MediaFormat.createVideoFormat(encodeType(), mWidth, mHeight)
        // 码率
        outputFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
        // 帧率
        outputFormat.setInteger(MediaFormat.KEY_FRAME_RATE, DEFAULT_ENCODE_FRAME_RATE)
        // 关键帧间隔-每秒关键帧数
        outputFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2) //1s一个I帧
        if (!encodeManually()) {
            // 颜色空间 从surface当中获得
            outputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
        } else {
            // 颜色空间 从外部获得，需要外部传入数据放到编码队列中去编码
            outputFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
        }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            outputFormat.setInteger(
//                MediaFormat.KEY_PROFILE,
//                MediaCodecInfo.CodecProfileLevel.AVCProfileHigh
//            )
//            outputFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel31)
//        }

//        try {
//            configEncoderWithCQ(codec, outputFormat)
//        } catch (e: Exception) {
//            e.printStackTrace()
//            // 捕获异常，设置为系统默认配置 BITRATE_MODE_VBR
//            try {
//                configEncoderWithVBR(codec, outputFormat)
//            } catch (e: Exception) {
//                e.printStackTrace()
//                Log.e(TAG, "配置视频编码器失败")
//            }
//        }

        //配置编码器
        mCodec.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        if (!encodeManually()) {
            //这个surface显示的内容就是要编码的画面
            mSurface = mCodec.createInputSurface()
        }
    }

    private fun configEncoderWithCQ(codec: MediaCodec, outputFormat: MediaFormat) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // 本部分手机不支持 BITRATE_MODE_CQ 模式，有可能会异常
            outputFormat.setInteger(
                MediaFormat.KEY_BITRATE_MODE,
                MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CQ
            )
        }
        codec.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    }

    private fun configEncoderWithVBR(codec: MediaCodec, outputFormat: MediaFormat) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            outputFormat.setInteger(
                MediaFormat.KEY_BITRATE_MODE,
                MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR
            )
        }
        codec.configure(outputFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    }

    override fun addTrack(muxer: MMuxer, mediaFormat: MediaFormat) {
        muxer.addVideoTrack(mediaFormat)
    }

    fun dequeueFrame(buffer: ByteArray, presentationTimeUs: Long) {
        val frame = Frame()
        frame.setBuffer(buffer)
        frame.setPresentationTimeUs(presentationTimeUs)
        Log.d(TAG, "video一帧时间戳 = " + presentationTimeUs / 1000000.0f)
        dequeueFrame(frame)
    }

    override fun writeData(
        muxer: MMuxer,
        byteBuffer: ByteBuffer,
        bufferInfo: MediaCodec.BufferInfo
    ) {
        byteBuffer.position(bufferInfo.offset)
        byteBuffer.limit(bufferInfo.offset + bufferInfo.size)

        if (pts == 0L) {
            pts = bufferInfo.presentationTimeUs
        }
        bufferInfo.presentationTimeUs = bufferInfo.presentationTimeUs - pts
        Log.d(
            "VideoEncoder",
            "视频秒数时间戳 = " + bufferInfo.presentationTimeUs / 1000000.0f
        )
        muxer.writeVideoData(byteBuffer, bufferInfo)
    }

    override fun release(muxer: MMuxer) {
        muxer.releaseVideoTrack()
    }

    fun getSurface(): Surface? {
        return mSurface
    }

    companion object {
        const val DEFAULT_ENCODE_FRAME_RATE = 30
    }
}