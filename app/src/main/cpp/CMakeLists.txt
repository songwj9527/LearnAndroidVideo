# cmake 最低版本
cmake_minimum_required(VERSION 3.4.1)

# 设置生成的so动态库最后输出的路径
# set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

# 支持C++11标准和GNU扩展特性
# 1) -fPIC
#    fPIC的全称是 Position Independent Code， 用于生成位置无关代码。
#    不加fPIC编译的so必须要在加载到用户程序的地址空间时重定向所有表目，它里面不能引用其它地方的代码，
#    加了fPIC实现真正意义上的多个进程共享so文件。
# 2) 指定CPU架构：-march
#   -march=armv7-a
# 3) 指定CPU类型：-mtune
#   -mtune=cortex-a8
# 4) 设置优化程度：
#   -O0表明不做优化，-O3表示设置为最高优化程度。
# 5）浮点协处理器类型：-mfpu
#   -mfpu=neon // 开启采用NEON优化技术
# 6) 浮点运算处理方式：-mfloat-abi（在指定-mfpu的情况下使用）
#     soft是指所有浮点运算全部在软件层实现，效率当然不高，会存在不必要的浮点到整数、整数到浮点的转换，只适合于早期没有浮点计算单元的ARM处理器；
#     softfp是目前armel的默认设置，它将浮点计算交给FPU处理，但函数参数的传递使用通用的整型寄存器而不是FPU寄存器；
#     hard则使用FPU浮点寄存器将函数参数传递给FPU处理。
#     需要注意的是，在兼容性上，soft与后两者是兼容的，但softfp和hard两种模式不兼容。
#     默认情况下，armel使用softfp，因此将hard模式的armel单独作为一个abi，称之为armhf。而使用hard模式，在每次浮点相关函数调用时，平均能节省20个CPU周期。
#     对ARM这样每个周期都很重要的体系结构来说，这样的提升无疑是巨大的。在完全不改变源码和配置的情况下，在一些应用程序上，使用armhf能得到20%——25%的性能提升。对一些严重依赖于浮点运算的程序，更是可以达到300%的性能提升。
# 7）-ftree-vectorize，选项打开自动向量化，并在使用-O3时自动设置此标志。
# 8）-ffast-math，优化浮点运算，指示编译不必遵循 IEEE 和 ISO 的浮点运算标准，提高浮点运算速度。
# -fPIC -march=armv7-a -mtune=cortex-a8 -O3 -mfloat-abi=softfp -mfpu=neon -ftree-vectorize -ffast-math
#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11 -march=armv7-a -mtune=cortex-a8 -O3 -mfloat-abi=softfp -mfpu=neon -ftree-vectorize -ffast-math")

# 1. 定义so库和头文件所在目录，方面后面使用
set(ffmpeg_lib_dir ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})
set(ffmpeg_head_dir ${CMAKE_SOURCE_DIR}/ffmpeg)

# 2. 添加头文件目录
include_directories(${ffmpeg_head_dir}/include)

# 3. 添加ffmpeg相关的so库
add_library( avutil
        SHARED
        IMPORTED )
set_target_properties( avutil
        PROPERTIES IMPORTED_LOCATION
        ${ffmpeg_lib_dir}/libavutil.so )

add_library(swresample
        SHARED
        IMPORTED )
set_target_properties( swresample
        PROPERTIES IMPORTED_LOCATION
        ${ffmpeg_lib_dir}/libswresample.so )

add_library( avcodec
        SHARED
        IMPORTED )
set_target_properties( avcodec
        PROPERTIES IMPORTED_LOCATION
        ${ffmpeg_lib_dir}/libavcodec.so )

add_library( avfilter
        SHARED
        IMPORTED)
set_target_properties( avfilter
        PROPERTIES IMPORTED_LOCATION
        ${ffmpeg_lib_dir}/libavfilter.so )

add_library( swscale
        SHARED
        IMPORTED)
set_target_properties( swscale
        PROPERTIES IMPORTED_LOCATION
        ${ffmpeg_lib_dir}/libswscale.so )

add_library( avformat
        SHARED
        IMPORTED)
set_target_properties( avformat
        PROPERTIES IMPORTED_LOCATION
        ${ffmpeg_lib_dir}/libavformat.so )

add_library( avdevice
        SHARED
        IMPORTED)
set_target_properties( avdevice
        PROPERTIES IMPORTED_LOCATION
        ${ffmpeg_lib_dir}/libavdevice.so )

# 4. 添加h264相关的so库
add_library( h264
        SHARED
        IMPORTED)
set_target_properties( h264
        PROPERTIES IMPORTED_LOCATION
        ${ffmpeg_lib_dir}/libh264.so )

# 配置目标so库编译信息
add_library(
        # 输出so库的名称
        native-lib

        # 设置生成库的方式，默认为SHARE动态库
        SHARED

        # 列出参与编译的所有源文件
        native-lib.cpp

        # 工具
        ${CMAKE_SOURCE_DIR}/utils/logger.h
        ${CMAKE_SOURCE_DIR}/utils/timer.c
        ${CMAKE_SOURCE_DIR}/media/const.h
        ${CMAKE_SOURCE_DIR}/media/cache_frame.h
        ${CMAKE_SOURCE_DIR}/media/cache_frame.cpp

        # 播放器
        ${CMAKE_SOURCE_DIR}/media/media_codes.h
        ${CMAKE_SOURCE_DIR}/media/player/player_state.h
        ${CMAKE_SOURCE_DIR}/media/player/player.h
        ${CMAKE_SOURCE_DIR}/media/player/player.cpp
        ${CMAKE_SOURCE_DIR}/media/player/ffmpeg_player/ffmpeg_player.h
        ${CMAKE_SOURCE_DIR}/media/player/ffmpeg_player/ffmpeg_player.cpp
        ${CMAKE_SOURCE_DIR}/media/player/ffmpeg_player/default_player/media_player.h
        ${CMAKE_SOURCE_DIR}/media/player/ffmpeg_player/default_player/media_player.cpp
        ${CMAKE_SOURCE_DIR}/media/player/ffmpeg_player/opengl_player/opengl_player.h
        ${CMAKE_SOURCE_DIR}/media/player/ffmpeg_player/opengl_player/opengl_player.cpp

        # 解码器
        ${CMAKE_SOURCE_DIR}/media/decoder/base_decoder.h
        ${CMAKE_SOURCE_DIR}/media/decoder/base_decoder.cpp
        ${CMAKE_SOURCE_DIR}/media/decoder/video/video_decoder.h
        ${CMAKE_SOURCE_DIR}/media/decoder/video/video_decoder.cpp
        ${CMAKE_SOURCE_DIR}/media/decoder/audio/audio_decoder.h
        ${CMAKE_SOURCE_DIR}/media/decoder/audio/audio_decoder.cpp

        # 渲染器
        ${CMAKE_SOURCE_DIR}/media/render/render_frame.h
        ${CMAKE_SOURCE_DIR}/media/render/render_frame.cpp
        ${CMAKE_SOURCE_DIR}/media/render/base_render.h
        ${CMAKE_SOURCE_DIR}/media/render/base_render.cpp
        ${CMAKE_SOURCE_DIR}/media/render/i_render_state_cb.h
        ${CMAKE_SOURCE_DIR}/media/render/video/base_video_render.h
        ${CMAKE_SOURCE_DIR}/media/render/video/base_video_render.cpp
        ${CMAKE_SOURCE_DIR}/media/render/video/default/default_video_render.h
        ${CMAKE_SOURCE_DIR}/media/render/video/default/default_video_render.cpp
        ${CMAKE_SOURCE_DIR}/media/render/video/opengl/opengl_render.h
        ${CMAKE_SOURCE_DIR}/media/render/video/opengl/opengl_render.cpp
        ${CMAKE_SOURCE_DIR}/media/render/video/opengl/opengl_pixel_receiver.h
        ${CMAKE_SOURCE_DIR}/media/render/audio/audio_render.h
        ${CMAKE_SOURCE_DIR}/media/render/audio/audio_render.cpp
        ${CMAKE_SOURCE_DIR}/media/render/audio/opensl_render.h
        ${CMAKE_SOURCE_DIR}/media/render/audio/opensl_render.cpp

        # OpenGL EGL核心
        ${CMAKE_SOURCE_DIR}/media/opengl/egl/egl_core.h
        ${CMAKE_SOURCE_DIR}/media/opengl/egl/egl_core.cpp
        ${CMAKE_SOURCE_DIR}/media/opengl/egl/egl_surface.h
        ${CMAKE_SOURCE_DIR}/media/opengl/egl/egl_surface.cpp

        # OpenGL绘制器
        ${CMAKE_SOURCE_DIR}/media/opengl/drawer/matrix.h
        ${CMAKE_SOURCE_DIR}/media/opengl/drawer/matrix.cpp
        ${CMAKE_SOURCE_DIR}/media/opengl/drawer/drawer.h
        ${CMAKE_SOURCE_DIR}/media/opengl/drawer/drawer.cpp
        ${CMAKE_SOURCE_DIR}/media/opengl/drawer/video_drawer.h
        ${CMAKE_SOURCE_DIR}/media/opengl/drawer/video_drawer.cpp
        ${CMAKE_SOURCE_DIR}/media/opengl/drawer/fbo_soul_video_drawer.h
        ${CMAKE_SOURCE_DIR}/media/opengl/drawer/fbo_soul_video_drawer.cpp

        # OpenGL绘制代理器
        ${CMAKE_SOURCE_DIR}/media/opengl/drawer/proxy/drawer_proxy.h
        ${CMAKE_SOURCE_DIR}/media/opengl/drawer/proxy/drawer_proxy.cpp
        ${CMAKE_SOURCE_DIR}/media/opengl/drawer/proxy/def_drawer_proxy_impl.h
        ${CMAKE_SOURCE_DIR}/media/opengl/drawer/proxy/def_drawer_proxy_impl.cpp

        # MediaCodec音视频分离器
        ${CMAKE_SOURCE_DIR}/media/codec/extractor/base_extractor.h
        ${CMAKE_SOURCE_DIR}/media/codec/extractor/base_extractor.cpp
        ${CMAKE_SOURCE_DIR}/media/codec/extractor/audio_extractor.h
        ${CMAKE_SOURCE_DIR}/media/codec/extractor/audio_extractor.cpp
        ${CMAKE_SOURCE_DIR}/media/codec/extractor/video_extractor.h
        ${CMAKE_SOURCE_DIR}/media/codec/extractor/video_extractor.cpp

        # MediaCodec音视频轨道
        ${CMAKE_SOURCE_DIR}/media/codec/track/i_sync_clock_receiver.h
        ${CMAKE_SOURCE_DIR}/media/codec/track/i_track_callback.h
        ${CMAKE_SOURCE_DIR}/media/codec/track/i_video_track_callback.h
        ${CMAKE_SOURCE_DIR}/media/codec/track/i_track.h
        ${CMAKE_SOURCE_DIR}/media/codec/track/base_track.h
        ${CMAKE_SOURCE_DIR}/media/codec/track/base_track.cpp
        ${CMAKE_SOURCE_DIR}/media/codec/track/audio_track.h
        ${CMAKE_SOURCE_DIR}/media/codec/track/audio_track.cpp
        ${CMAKE_SOURCE_DIR}/media/codec/track/base_video_track.h
        ${CMAKE_SOURCE_DIR}/media/codec/track/base_video_track.cpp
        ${CMAKE_SOURCE_DIR}/media/codec/track/default_video_track.h
        ${CMAKE_SOURCE_DIR}/media/codec/track/default_video_track.cpp

        # MediaCodec音视频播放器
        ${CMAKE_SOURCE_DIR}/media/player/codec_player/codec_player.h
        ${CMAKE_SOURCE_DIR}/media/player/codec_player/codec_player.cpp
        ${CMAKE_SOURCE_DIR}/media/player/codec_player/default_codec_player.h
        ${CMAKE_SOURCE_DIR}/media/player/codec_player/default_codec_player.cpp

        # 重新打包器
        ${CMAKE_SOURCE_DIR}/media/muxer/ff_repack.h
        ${CMAKE_SOURCE_DIR}/media/muxer/ff_repack.cpp

        # mp4打包器
        ${CMAKE_SOURCE_DIR}/media/muxer/i_muxer_cb.h
        ${CMAKE_SOURCE_DIR}/media/muxer/mp4_muxer.h
        ${CMAKE_SOURCE_DIR}/media/muxer/mp4_muxer.cpp

        # 编码器
        ${CMAKE_SOURCE_DIR}/media/encoder/encode_cache_frame.h
        ${CMAKE_SOURCE_DIR}/media/encoder/i_encode_state_cb.h
        ${CMAKE_SOURCE_DIR}/media/encoder/i_encoder.h
        ${CMAKE_SOURCE_DIR}/media/encoder/base_encoder.h
        ${CMAKE_SOURCE_DIR}/media/encoder/base_encoder.cpp
        ${CMAKE_SOURCE_DIR}/media/encoder/video/video_encoder.h
        ${CMAKE_SOURCE_DIR}/media/encoder/video/video_encoder.cpp
        ${CMAKE_SOURCE_DIR}/media/encoder/audio/audio_encoder.h
        ${CMAKE_SOURCE_DIR}/media/encoder/audio/audio_encoder.cpp
)

# 查找代码中使用到的系统log库
find_library(log-lib log)
# 查找代码中使用到的系统media库
find_library(media-ndk mediandk)

# 指定编译目标库时，cmake要链接的库
target_link_libraries(
        # 指定目标库，native-lib 是在上面 add_library 中配置的目标库
        native-lib

        # 系统log库
        ${log-lib}

        # FFmpeg 相关的库
        avutil
        swresample
        avcodec
        avfilter
        swscale
        avformat
        avdevice

        # android系统库
        -landroid

        # android系统OpenSLES库
        OpenSLES

        # android系统GL库
        -lEGL
        -lGLESv2

        # 系统media库
        ${media-ndk}

        # h264 相关的库
        h264 )
