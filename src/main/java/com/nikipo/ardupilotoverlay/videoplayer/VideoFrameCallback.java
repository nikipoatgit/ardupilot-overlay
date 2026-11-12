package com.nikipo.ardupilotoverlay.videoplayer;

@FunctionalInterface
public interface VideoFrameCallback {
    void accept(VideoEngine.VideoFrame frame);
}
