//
// Created by fgrid on 2/1/21.
//

#ifndef OPENVIDEO_PLAYER_STATE_H
#define OPENVIDEO_PLAYER_STATE_H

enum State {
    /** 初始状态（未进行任何操作）**/
    IDLE,
    /** 准备完成状态（初始化数据提取器、参数、渲染器等）**/
    PREPARED,
    /** 播放中 **/
    RUNNING,
    /** 播放暂停 **/
    PAUSED,
    /** 播放停止 **/
    STOPPED,
    /** 播放完成 **/
    COMPLETED,
    /** 正在快进 **/
    SEEKING,
    /** 播放异常 **/
    ERROR,
};

#endif //OPENVIDEO_PLAYER_STATE_H
