package com.songwj.openvideo.mediacodec.encoder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.songwj.openvideo.mediacodec.muxer.MMuxer
import java.nio.ByteBuffer


/**
 * 音频编码器
 */
// 编码采样率率
private const val DEST_SAMPLE_RATE = 44100
// 编码码率
private const val DEST_BIT_RATE = 96000
// 声道：双声道
private const val CHANNEL_COUNT = 2
//对应录制音频时传入的AudioFormat.ENCODING_PCM_16BIT ，此处应传入16bit
private const val ENCODING_16BIT = 16
class AudioEncoder(muxer: MMuxer): BaseEncoder(muxer) {
//    private val TAG = "AudioEncoder"

    // input frame
    protected var presentationTimeUs = 0L
    // pts
    protected var pts: Long = 0

    init {
        TAG = "AudioEncoder"
        mFramesMaxSize = 0
    }

    override fun createCodec():MediaCodec {
        return MediaCodec.createEncoderByType(encodeType())
    }

    override fun encodeType(): String {
        return MediaFormat.MIMETYPE_AUDIO_AAC
    }

    override fun configEncoder(codec: MediaCodec) {
        val audioFormat = MediaFormat.createAudioFormat(encodeType(), DEST_SAMPLE_RATE, CHANNEL_COUNT)
        // 编码规格，可以看成质量
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        // 码率
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, DEST_BIT_RATE)
        audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 100 * 1024)
//        try {
//            configEncoderWithCQ(codec, audioFormat)
//        } catch (e: Exception) {
//            e.printStackTrace()
//            try {
//                configEncoderWithVBR(codec, audioFormat)
//            } catch (e: Exception) {
//                e.printStackTrace()
//                Log.e(TAG, "配置音频编码器失败")
//            }
//        }
        codec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
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
        muxer.addAudioTrack(mediaFormat)
    }

    override fun frameWaitTimeMs(): Long {
        return 5
    }

    fun dequeueFrame(buffer: ByteArray, buffSize: Int) {
        val frame = Frame()
        frame.setBuffer(buffer)
        //presentationTimeUs = 1000000L * (buffSize / 2) / sampleRate
        //一帧音频帧大小 int size = 采样率 x 位宽 x 采样时间 x 通道数
        // 1s时间戳计算公式  presentationTimeUs = 1000000L * (totalBytes / sampleRate/ audioFormat / channelCount / 8 )
        //totalBytes : 传入编码器的总大小
        //1000 000L : 单位为 微秒，换算后 = 1s,
        //除以8     : pcm原始单位是bit, 1 byte = 8 bit, 1 short = 16 bit, 用 Byte[]、Short[] 承载则需要进行换算
        //presentationTimeUs = 1000000L * (buffSize / 2) / sampleRate
        //一帧音频帧大小 int size = 采样率 x 位宽 x 采样时间 x 通道数
        // 1s时间戳计算公式  presentationTimeUs = 1000000L * (totalBytes / sampleRate/ audioFormat / channelCount / 8 )
        //totalBytes : 传入编码器的总大小
        //1000 000L : 单位为 微秒，换算后 = 1s,
        //除以8     : pcm原始单位是bit, 1 byte = 8 bit, 1 short = 16 bit, 用 Byte[]、Short[] 承载则需要进行换算
        presentationTimeUs += (1.0 * buffSize / (DEST_SAMPLE_RATE * CHANNEL_COUNT * (ENCODING_16BIT / 8)) * 1000000.0).toLong()
        frame.setPresentationTimeUs(presentationTimeUs)
        Log.d(TAG, "pcm一帧时间戳 = " + presentationTimeUs / 1000000.0f)
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
            "AudioEncoder",
            "音频秒数时间戳 = " + bufferInfo.presentationTimeUs / 1000000.0f
        )
        muxer.writeAudioData(byteBuffer, bufferInfo)
    }

    override fun release(muxer: MMuxer) {
        muxer.releaseAudioTrack()
    }
}