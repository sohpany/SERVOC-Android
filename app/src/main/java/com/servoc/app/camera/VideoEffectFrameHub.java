package com.servoc.app.camera;

/** Process-local bridge from the single JPEG decode stage to the active recorder. */
final class VideoEffectFrameHub {
    interface Consumer { void onDecodedFrame(SharedVideoFrame frame); }
    private static volatile Consumer consumer;
    private VideoEffectFrameHub(){}
    static void set(Consumer c){consumer=c;}
    static Consumer get(){return consumer;}
    static void clear(Consumer c){if(consumer==c)consumer=null;}
}
