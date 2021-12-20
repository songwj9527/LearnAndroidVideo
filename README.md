# LearnAndroidVideo
本人在学习音视频开发的过程中，深刻体会到了由于知识的分散，过渡断层带来的种种困惑和痛苦，以及没有系统直观的代码可以参考，因此产生了此安卓音视频demo。希望通过自己的理解，帮助想入门音视频开发的开发者小伙伴们。


# 视频播放器
  1. MediaCodec解码 + TextureView窗口播放；
  2. 添加MediaCodec解码 + OpenGL渲染播放（FBO灵魂抖动等效果）；
  3. 添加MediaCodec解码 + EGL渲染 + TextureView窗口播放（FBO灵魂抖动等效果）；
  4. Native FFMpeg解码 + TextureView渲染播放；
  5. Native FFMpeg解码 + Native EGL渲染 + TextureView窗口播放；
  6. Native MediaCodec解码 + TextureView窗口播放（未完待续，具体原因是AMediaExtractor_setDataSource无法正常打开视频, 目前未找到解决方法）.


# 相机
  1. camera1预览、拍照和视频录制；
  2. camera1 + 自定义OpenGL 预览、拍照和视频录制；
  3. camera2预览、拍照和视频录制；
  4. camera2 + 自定义OpenGL 预览、拍照和视频录制；
  5. camerax预览、拍照；
  6. GPUImage滤镜 + camera2相机 预览、拍照（视频录制目前没做，懒。。。思路是自定义GPUImage,参考第4点即可实现）；
  7. 推流实现（未完待续）。
