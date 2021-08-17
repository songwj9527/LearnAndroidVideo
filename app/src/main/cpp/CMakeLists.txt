# cmake 最低版本
cmake_minimum_required(VERSION 3.4.1)

# 设置生成的so动态库最后输出的路径
# set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

# 支持C++11标准和GNU扩展特性
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

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
        ${CMAKE_SOURCE_DIR}/media/player/default_player/media_player.h
        ${CMAKE_SOURCE_DIR}/media/player/default_player/media_player.cpp
        ${CMAKE_SOURCE_DIR}/media/player/opengl_player/opengl_player.h
        ${CMAKE_SOURCE_DIR}/media/player/opengl_player/opengl_player.cpp
        ${CMAKE_SOURCE_DIR}/media/codec/test_mediacodec.h
        ${CMAKE_SOURCE_DIR}/media/codec/test_mediacodec.cpp

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

        # 合成器
        ${CMAKE_SOURCE_DIR}/media/synthesizer/synthesizer.h
        ${CMAKE_SOURCE_DIR}/media/synthesizer/synthesizer.cpp
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