package com.fc.opencv;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.*;

public class RtspTransferToFile {


    /**
     * 按帧录制视频
     *
     * @param inputFile-该地址可以是网络直播/录播地址，也可以是远程/本地文件路径
     * @param outputFile
     *            -该地址只能是文件地址，如果使用该方法推送流媒体服务器会报错，原因是没有设置编码格式
     * @throws FrameGrabber.Exception
     * @throws FrameRecorder.Exception
     * @throws org.bytedeco.javacv.FrameRecorder.Exception
     */
    public static void frameRecord(String inputFile, String outputFile, int audioChannel)
            throws Exception, org.bytedeco.javacv.FrameRecorder.Exception {

        boolean start=true;//该变量建议设置为全局控制变量，用于控制录制结束
        // 获取视频源
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFile);
        grabber.start();
        // 流媒体输出地址，分辨率（长，高），是否录制音频（0:不录制/1:录制）
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, grabber.getImageWidth(),
                grabber.getImageHeight(), audioChannel);

        recorder.setInterleaved(true);
        recorder.setVideoOption("preset","ultrafast");
        recorder.setVideoOption("tune","zerolatency");
        recorder.setVideoBitrate(grabber.getVideoBitrate());
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);//H264
        if(grabber.getAudioBitrate() > 0){
            recorder.setAudioBitrate(grabber.getAudioBitrate());
        }

        recorder.setAudioCodec(grabber.getAudioCodec());
        recorder.setAudioChannels(grabber.getAudioChannels());
        recorder.setSampleRate(grabber.getSampleRate());
        recorder.setFormat("mp4");
        // 开始取视频源
        recordByFrame(grabber, recorder, true);
    }

    /**
     * 保存
     * @param grabber
     * @param recorder
     * @param status
     * @throws Exception
     * @throws org.bytedeco.javacv.FrameRecorder.Exception
     */
    private static void recordByFrame(FFmpegFrameGrabber grabber, FFmpegFrameRecorder recorder, Boolean status)
            throws Exception, org.bytedeco.javacv.FrameRecorder.Exception {
        try {//建议在线程中使用该方法
            recorder.start();
            Frame frame = null;
            while (status&& (frame = grabber.grabFrame()) != null) {
                recorder.record(frame);
            }
            recorder.stop();
            grabber.stop();
        } finally {
            if (grabber != null) {
                grabber.stop();
            }
        }
    }
    //https://blog.csdn.net/u011270282/article/details/50374245
    //
    public static void main(String[] args)
            throws Exception {
        String inputFile = "/Users/fangchi/live/iPc.me-TheAuroraNorthernLights/我只在乎你.mp4";
        //String inputFile = "rtsp://192.168.38.137:8554/ff";
        // Decodes-encodes
        String outputFile = "recorde.mp4";
        frameRecord(inputFile, outputFile,0);
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(outputFile);
        grabber.start();
        System.out.println(grabber.getVideoCodec());
    }

}
